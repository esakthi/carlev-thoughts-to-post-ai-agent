"""Main entry point for the Thoughts-to-Post AI Agent."""

import logging
import signal
import sys
from typing import Optional

from .config import settings
from .kafka import KafkaRequestConsumer, KafkaResponseProducer
from .agents import ContentEnrichmentAgent, ImageGenerationAgent, VideoGenerationAgent
from .memory import CheckpointMemory
from .models import (
    ThoughtRequest,
    AgentResponse,
    RequestStatus,
    AgentContext,
    EnrichedContent,
    GeneratedImage,
)
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
    ],
)

logger = logging.getLogger(__name__)


class ThoughtsToPostAgent:
    """Main orchestrator for the AI agent pipeline."""

    def __init__(self):
        """Initialize the agent and its components."""
        self.consumer = KafkaRequestConsumer()
        self.producer = KafkaResponseProducer()
        self.content_agent = ContentEnrichmentAgent()
        self.image_agent = ImageGenerationAgent()
        self.video_agent = VideoGenerationAgent()
        self.memory = CheckpointMemory(persist_dir="./checkpoints")
        self._shutdown_requested = False

    def _setup_signal_handlers(self) -> None:
        """Set up signal handlers for graceful shutdown."""

        def signal_handler(signum, frame):
            logger.info(f"Received signal {signum}, initiating shutdown...")
            self._shutdown_requested = True
            self.consumer.stop()

        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)

    def _sync_history_to_agent(self, context: AgentContext) -> None:
        """Convert stored dict history to LangChain messages and set in agent."""
        logger.info(f"Syncing history TO agent for request: {context.request_id}")
        messages = []
        for msg in context.conversation_history:
            if msg.get("type") == "human":
                messages.append(HumanMessage(content=msg["content"]))
            elif msg.get("type") == "ai":
                messages.append(AIMessage(content=msg["content"]))
            elif msg.get("type") == "system":
                messages.append(SystemMessage(content=msg["content"]))

        logger.debug(f"Converting {len(messages)} historical messages for LangChain")
        self.content_agent.set_history(context.request_id, messages)

    def _sync_history_from_agent(self, context: AgentContext) -> None:
        """Convert agent's LangChain messages back to dicts and store in context."""
        logger.info(f"Syncing history FROM agent for request: {context.request_id}")
        messages = self.content_agent.get_history(context.request_id)
        history_dicts = []
        for msg in messages:
            msg_type = "unknown"
            if isinstance(msg, HumanMessage):
                msg_type = "human"
            elif isinstance(msg, AIMessage):
                msg_type = "ai"
            elif isinstance(msg, SystemMessage):
                msg_type = "system"

            history_dicts.append({
                "type": msg_type,
                "content": msg.content
            })
        context.conversation_history = history_dicts

    def process_request(self, request: ThoughtRequest, headers: Optional[dict] = None) -> None:
        """Process a single thought enrichment request.

        Args:
            request: The thought request to process
            headers: Kafka headers for retry tracking
        """
        logger.info(f"Processing request: {request.request_id} (Headers: {headers})")

        # Create or get existing context
        logger.debug(f"Retrieving context for request {request.request_id}")
        context = self.memory.get_context(request.request_id)
        if not context:
            logger.info(f"No existing context found for {request.request_id}. Creating new context.")
            context = self.memory.create_context(request)
        else:
            logger.info(f"Found existing context for {request.request_id} (Version: {context.current_version})")
            # If refinement, add to context
            if request.additional_instructions and request.version > context.current_version:
                logger.info(f"Refinement detected for request {request.request_id}.")
                self.memory.add_refinement(request.request_id, request.additional_instructions)

        try:
            # Update status to processing
            self.memory.update_context(
                request.request_id, status=RequestStatus.PROCESSING
            )

            # Sync history from persistent context to agent's memory
            self._sync_history_to_agent(context)

            # Case 1: Image Refinement only
            if request.image_refinement_instructions:
                logger.info(f"Processing image refinement for request {request.request_id}")

                platforms_to_process = request.platforms or [p.platform for p in context.enriched_contents]
                if request.target_platform:
                    platforms_to_process = [request.target_platform]

                for platform_type in platforms_to_process:
                    # Find existing enriched content
                    content = next((c for c in context.enriched_contents if c.platform == platform_type), None)
                    if content:
                        logger.info(f"Generating refined image for {platform_type.value}")
                        try:
                            new_image = self.image_agent.generate_for_content(
                                content,
                                request.image_refinement_instructions,
                                request_id=request.request_id,
                            )
                            content.images.append(new_image)

                            # Update local context incrementally
                            self.memory.update_context(
                                request.request_id, enriched_contents=context.enriched_contents
                            )
                        except Exception as e:
                            logger.error(f"Failed to generate refined image for {platform_type}: {e}")

                final_status = RequestStatus.COMPLETED
                self.memory.update_context(request.request_id, status=final_status)

                response = AgentResponse(
                    request_id=request.request_id,
                    user_id=request.user_id,
                    status=final_status,
                    enriched_contents=context.enriched_contents,
                    version=context.current_version,
                )
                self.producer.send(response)
                return

            # Case 2: Full enrichment or Text Refinement (Standard Flow)
            logger.info(f"Enriching content for platforms: {request.platforms}")
            all_enriched_contents = context.enriched_contents or []
            failed_platforms = []

            for platform in request.platforms:
                logger.info(f"-> Processing platform: {platform}")
                try:
                    # Step 1: Enrich Text
                    enriched = self.content_agent.enrich_for_platform(request, platform)

                    # Step 2: Generate unique image for this platform
                    logger.info(f"Generating unique image for platform: {platform}")
                    config = next((c for c in request.platform_configurations if c.platform == platform), None)

                    try:
                        user_img_prompt = config.image_prompt if config else None
                        img_params = config.image_params.model_dump() if config and config.image_params else None

                        initial_image = self.image_agent.generate_for_content(
                            enriched,
                            user_image_prompt=user_img_prompt,
                            image_params=img_params,
                            request_id=request.request_id,
                        )
                        enriched.images = [initial_image]
                    except Exception as e:
                        logger.warning(f"Initial image generation failed for {platform}: {e}")
                        enriched.images = []

                    # Step 3: Video Generation
                    if config and config.video_prompt:
                        logger.info(f"Generating video for platform: {platform}")
                        try:
                            video_result = self.video_agent.generate_for_content(
                                enriched,
                                user_video_prompt=config.video_prompt,
                                video_params=config.video_params.model_dump() if config.video_params else None,
                                request_id=request.request_id
                            )
                            logger.info(f"Video generation simulated: {video_result}")
                            # In future, EnrichedContent will have a videos list
                        except Exception as e:
                            logger.warning(f"Video generation failed for {platform}: {e}")

                    # Update list
                    existing_idx = next((i for i, c in enumerate(all_enriched_contents) if c.platform == platform), -1)
                    if existing_idx >= 0:
                        # Carry over old images if this is a text-only refinement?
                        # Requirements say "regenerate one and keep history".
                        # For text refinement, we probably want new images representing new text.
                        all_enriched_contents[existing_idx] = enriched
                    else:
                        all_enriched_contents.append(enriched)

                    # Update local context incrementally
                    self.memory.update_context(
                        request.request_id, enriched_contents=all_enriched_contents
                    )

                    # Send IN_PROGRESS update
                    progress_response = AgentResponse(
                        request_id=request.request_id,
                        user_id=request.user_id,
                        status=RequestStatus.IN_PROGRESS,
                        enriched_contents=[enriched],
                        version=context.current_version,
                    )
                    self.producer.send(progress_response)
                except Exception as e:
                    logger.error(f"Failed to enrich for {platform}: {e}")
                    failed_platforms.append(platform)

            if not all_enriched_contents and request.platforms:
                msg = f"Failed to enrich content for any of the requested platforms"
                raise Exception(msg)

            # Sync history back
            self._sync_history_from_agent(context)

            # Determine final status
            final_status = RequestStatus.COMPLETED
            if failed_platforms:
                if all_enriched_contents:
                    final_status = RequestStatus.PARTIALLY_COMPLETED
                else:
                    final_status = RequestStatus.FAILED

            self.memory.update_context(request.request_id, status=final_status)

            # Create and send final response
            response = AgentResponse(
                request_id=request.request_id,
                user_id=request.user_id,
                status=final_status,
                enriched_contents=all_enriched_contents,
                failed_platforms=failed_platforms,
                version=context.current_version,
                error_message=f"Failed platforms: {failed_platforms}" if failed_platforms else None
            )

            self.producer.send(response)
            logger.info(f"Successfully processed request: {request.request_id}")

        except Exception as e:
            logger.error(f"Failed to process request {request.request_id}: {e}", exc_info=True)

            # Retry logic
            retry_count = int(headers.get("x-retry-count", 0)) if headers else 0
            if retry_count < 3:
                new_retry_count = retry_count + 1
                retry_topics = ["retry-5s", "retry-30s", "retry-5m"]
                next_topic = f"{settings.kafka_request_topic}-{retry_topics[retry_count]}"

                logger.info(f"Retrying request {request.request_id} (Attempt {new_retry_count}) via topic {next_topic}")

                retry_headers = [
                    ("x-retry-count", str(new_retry_count).encode("utf-8")),
                    ("x-last-error", str(e).encode("utf-8"))
                ]

                self.producer.send(request.model_dump(), topic=next_topic, headers=retry_headers)
                return

            self.memory.update_context(request.request_id, status=RequestStatus.FAILED, error_message=str(e))
            error_response = AgentResponse(
                request_id=request.request_id,
                user_id=request.user_id,
                status=RequestStatus.FAILED,
                error_message=str(e),
                version=context.current_version if context else 1,
            )
            self.producer.send(error_response)

    def start(self) -> None:
        """Start the agent and begin processing messages."""
        logger.info("Starting Thoughts-to-Post AI Agent")
        self._setup_signal_handlers()
        try:
            self.producer.connect()
            self.consumer.start(self.process_request)
        except KeyboardInterrupt:
            logger.info("Keyboard interrupt received")
        finally:
            self.shutdown()

    def shutdown(self) -> None:
        """Gracefully shutdown the agent."""
        logger.info("Shutting down AI Agent...")
        self.consumer.stop()
        self.producer.close()


def main():
    agent = ThoughtsToPostAgent()
    agent.start()


if __name__ == "__main__":
    main()
