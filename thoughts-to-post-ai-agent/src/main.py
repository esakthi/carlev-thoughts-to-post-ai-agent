"""Main entry point for the Thoughts-to-Post AI Agent."""

import logging
import signal
import sys
from typing import Optional

from .config import settings
from .kafka import KafkaRequestConsumer, KafkaResponseProducer
from .agents import ContentEnrichmentAgent, ImageGenerationAgent
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
        logger.info("Initializing ThoughtsToPostAgent orchestrator")
        self.consumer = KafkaRequestConsumer()
        self.producer = KafkaResponseProducer()
        self.content_agent = ContentEnrichmentAgent()
        self.image_agent = ImageGenerationAgent()
        self.memory = CheckpointMemory(persist_dir="./checkpoints")
        self._shutdown_requested = False

    def _setup_signal_handlers(self) -> None:
        """Set up signal handlers for graceful shutdown."""

        def signal_handler(signum, frame):
            logger.info(f"Signal handler triggered for signal {signum}")
            logger.info(f"Initiating graceful shutdown sequence...")
            self._shutdown_requested = True
            self.consumer.stop()

        logger.info("Setting up OS signal handlers (SIGINT, SIGTERM)")
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)

    def _sync_history_to_agent(self, context: AgentContext) -> None:
        """Convert stored dict history to LangChain messages and set in agent."""
        logger.info(f"Syncing history TO agent for request: {context.request_id}")
        messages = []
        if not context.conversation_history:
            logger.info(f"No conversation history found for request {context.request_id}. Starting fresh.")
        else:
            logger.info(f"Found {len(context.conversation_history)} historical messages. Converting to LangChain objects.")
            for i, msg in enumerate(context.conversation_history):
                msg_type = msg.get("type")
                msg_content = msg["content"]
                logger.debug(f"Processing history message {i+1}: type='{msg_type}', content_preview='{msg_content[:50]}...'")

                if msg_type == "human":
                    messages.append(HumanMessage(content=msg_content))
                elif msg_type == "ai":
                    messages.append(AIMessage(content=msg_content))
                elif msg_type == "system":
                    messages.append(SystemMessage(content=msg_content))
                else:
                    logger.warning(f"Unknown message type '{msg_type}' in history. Skipping.")

        logger.info(f"Total messages successfully synced TO agent: {len(messages)}")
        self.content_agent.set_history(context.request_id, messages)

    def _sync_history_from_agent(self, context: AgentContext) -> None:
        """Convert agent's LangChain messages back to dicts and store in context."""
        logger.info(f"Syncing history FROM agent for request: {context.request_id}")
        messages = self.content_agent.get_history(context.request_id)

        if not messages:
            logger.info(f"Agent reported NO conversation history for request {context.request_id}")
            context.conversation_history = []
            return

        logger.info(f"Agent returned {len(messages)} messages. Converting back to serializable dicts.")
        history_dicts = []
        for i, msg in enumerate(messages):
            msg_type = "unknown"
            if isinstance(msg, HumanMessage):
                msg_type = "human"
            elif isinstance(msg, AIMessage):
                msg_type = "ai"
            elif isinstance(msg, SystemMessage):
                msg_type = "system"

            logger.debug(f"Converted LangChain message {i+1}: type='{msg_type}', content_length={len(msg.content)}")
            history_dicts.append({
                "type": msg_type,
                "content": msg.content
            })

        context.conversation_history = history_dicts
        logger.info(f"Syncing history FROM agent completed. Total dicts stored: {len(history_dicts)}")

    def process_request(self, request: ThoughtRequest) -> None:
        """Process a single thought enrichment request."""
        logger.info("-" * 40)
        logger.info(f"STARTING PROCESS_REQUEST: {request.request_id}")
        logger.info("-" * 40)
        logger.info(f"Request parameters: user_id='{request.user_id}', platforms={request.platforms}, version={request.version}")

        # Create or get existing context
        logger.debug(f"Looking up existing memory context for request {request.request_id}")
        context = self.memory.get_context(request.request_id)
        if not context:
            logger.info(f"NEW REQUEST FLOW: No existing context found for {request.request_id}. Initializing new context.")
            context = self.memory.create_context(request)
        else:
            logger.info(f"EXISTING REQUEST FLOW: Found context for {request.request_id} (Current version in memory: {context.current_version})")

            # Control Logic: Check if this is a refinement (new version)
            if request.additional_instructions and request.version > context.current_version:
                logger.info(f"VERSION INCREMENT DETECTED: Incoming version {request.version} > memory version {context.current_version}")
                logger.info(f"Adding refinement instruction: '{request.additional_instructions}'")
                self.memory.add_refinement(request.request_id, request.additional_instructions)
            else:
                logger.info(f"NO VERSION INCREMENT: version={request.version}, memory={context.current_version}. Treating as retry or redundant update.")

        try:
            # Update status to processing
            logger.info(f"Updating request status to '{RequestStatus.PROCESSING}'")
            self.memory.update_context(
                request.request_id, status=RequestStatus.PROCESSING
            )

            # Sync history from persistent context to agent's memory
            self._sync_history_to_agent(context)

            # Control Logic Case 1: Image Refinement only
            if request.image_refinement_instructions:
                logger.info("IMAGE REFINEMENT BRANCH: DETECTED image refinement instructions")
                logger.info(f"Instructions: '{request.image_refinement_instructions}'")

                # Decision for target platforms
                if request.target_platform:
                    logger.info(f"Decision: Target platform specified as '{request.target_platform.value}'")
                    platforms_to_process = [request.target_platform]
                else:
                    logger.info("Decision: No specific target platform. Processing all platforms in existing context.")
                    platforms_to_process = [p.platform for p in context.enriched_contents]

                logger.info(f"Platforms queued for image refinement: {[p.value for p in platforms_to_process]}")

                for platform_type in platforms_to_process:
                    logger.info(f"Iterating over platform for refinement: {platform_type.value}")
                    # Find existing enriched content
                    content = next((c for c in context.enriched_contents if c.platform == platform_type), None)
                    if content:
                        logger.info(f"Matching content found for {platform_type.value}. Triggering generation agent.")
                        try:
                            new_image = self.image_agent.generate_for_content(content, request.image_refinement_instructions)
                            logger.info(f"Refined image generated successfully (ID: {new_image.id})")
                            content.images.append(new_image)

                            logger.debug("Persisting updated image list to memory context")
                            self.memory.update_context(
                                request.request_id, enriched_contents=context.enriched_contents
                            )
                        except Exception as e:
                            logger.error(f"FAILURE in image refinement loop for {platform_type}: {e}")
                    else:
                        logger.warning(f"SKIP refinement: No enriched content found for {platform_type.value}")

                logger.info("Image refinement branch completed. Setting status to COMPLETED.")
                final_status = RequestStatus.COMPLETED
                self.memory.update_context(request.request_id, status=final_status)

                logger.info("Dispatching response message to Kafka producer")
                response = AgentResponse(
                    request_id=request.request_id,
                    user_id=request.user_id,
                    status=final_status,
                    enriched_contents=context.enriched_contents,
                    version=context.current_version,
                )
                self.producer.send(response)
                logger.info(f"FINISH process_request (Image Refinement) for {request.request_id}")
                return
            else:
                logger.info("IMAGE REFINEMENT BRANCH: NOT DETECTED. Proceeding with standard text enrichment flow.")

            # Case 2: Full enrichment or Text Refinement (Standard Flow)
            logger.info(f"STANDARD FLOW: Processing text enrichment for {len(request.platforms)} platforms")
            all_enriched_contents = context.enriched_contents or []
            failed_platforms = []

            for platform in request.platforms:
                logger.info(f"Looping over platform for enrichment: {platform.value}")
                try:
                    # Step 1: Enrich Text
                    logger.info(f"[{platform.value}] Step 1: Enriching Text via content agent...")
                    enriched = self.content_agent.enrich_for_platform(request, platform)
                    logger.info(f"[{platform.value}] Text enrichment success. Length: {len(enriched.body)} characters")

                    # Step 2: Generate unique image for this platform
                    logger.info(f"[{platform.value}] Step 2: Generating initial image representative of enriched text...")
                    try:
                        initial_image = self.image_agent.generate_for_content(enriched)
                        logger.info(f"[{platform.value}] Initial image success (ID: {initial_image.id})")
                        enriched.images = [initial_image]
                    except Exception as e:
                        logger.warning(f"[{platform.value}] Image generation FAIL: {e}. Post will have no initial image.")
                        enriched.images = []

                    # Control Logic: Update or Append
                    existing_idx = next((i for i, c in enumerate(all_enriched_contents) if c.platform == platform), -1)
                    if existing_idx >= 0:
                        logger.info(f"[{platform.value}] Decision: Replacing existing enriched content at index {existing_idx} (UPDATE flow)")
                        all_enriched_contents[existing_idx] = enriched
                    else:
                        logger.info(f"[{platform.value}] Decision: Appending new enriched content (INSERT flow)")
                        all_enriched_contents.append(enriched)

                    logger.debug(f"[{platform.value}] Incrementally updating memory context with enriched content")
                    self.memory.update_context(
                        request.request_id, enriched_contents=all_enriched_contents
                    )

                    # Send IN_PROGRESS update
                    logger.info(f"[{platform.value}] Sending intermediate IN_PROGRESS response to Kafka")
                    progress_response = AgentResponse(
                        request_id=request.request_id,
                        user_id=request.user_id,
                        status=RequestStatus.IN_PROGRESS,
                        enriched_contents=[enriched],
                        version=context.current_version,
                    )
                    self.producer.send(progress_response)
                except Exception as e:
                    logger.error(f"[{platform.value}] CRITICAL FAILURE in platform processing loop: {e}", exc_info=True)
                    failed_platforms.append(platform)

            # Final validation check
            if not all_enriched_contents and request.platforms:
                logger.error("Decision: Process FAILED. Zero platforms successfully enriched out of requested.")
                msg = f"Failed to enrich content for any of the requested platforms"
                raise Exception(msg)
            else:
                logger.info(f"Enrichment loop summary: {len(all_enriched_contents)} success, {len(failed_platforms)} failed")

            # Sync history back
            self._sync_history_from_agent(context)

            # Control Logic: Determine final status
            if failed_platforms:
                if all_enriched_contents:
                    logger.info("Final Status Decision: PARTIALLY_COMPLETED (Some platforms failed, some succeeded)")
                    final_status = RequestStatus.PARTIALLY_COMPLETED
                else:
                    logger.error("Final Status Decision: FAILED (All platform loops failed)")
                    final_status = RequestStatus.FAILED
            else:
                logger.info("Final Status Decision: COMPLETED (All platform loops succeeded)")
                final_status = RequestStatus.COMPLETED

            logger.info(f"Updating context status to '{final_status.value}'")
            self.memory.update_context(request.request_id, status=final_status)

            # Create and send final response
            logger.info("Constructing final AgentResponse message")
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
            logger.info("-" * 40)
            logger.info(f"FINISHED PROCESS_REQUEST: {request.request_id}")
            logger.info("-" * 40)

        except Exception as e:
            logger.error(f"TOP-LEVEL EXCEPTION for request {request.request_id}: {e}", exc_info=True)
            self.memory.update_context(request.request_id, status=RequestStatus.FAILED, error_message=str(e))

            error_response = AgentResponse(
                request_id=request.request_id,
                user_id=request.user_id,
                status=RequestStatus.FAILED,
                error_message=str(e),
                version=context.current_version if context else 1,
            )
            logger.info("Sending emergency ERROR response to Kafka")
            self.producer.send(error_response)

    def start(self) -> None:
        """Start the agent and begin processing messages."""
        logger.info("=" * 60)
        logger.info("Starting Thoughts-to-Post AI Agent Orchestrator")
        logger.info(f"Ollama URL: {settings.ollama_base_url}")
        logger.info(f"Ollama Model: {settings.ollama_model}")
        logger.info(f"Image Gen Type: {settings.image_generator_type}")
        logger.info(f"Kafka Servers: {settings.kafka_bootstrap_servers}")
        logger.info("=" * 60)

        self._setup_signal_handlers()
        try:
            logger.info("Connecting Kafka producer...")
            self.producer.connect()
            logger.info("Kafka producer connected. Starting consumer loop.")
            self.consumer.start(self.process_request)
        except KeyboardInterrupt:
            logger.info("Keyboard interrupt (Ctrl+C) received")
        except Exception as e:
            logger.critical(f"UNEXPECTED AGENT CRASH: {e}", exc_info=True)
        finally:
            self.shutdown()

    def shutdown(self) -> None:
        """Gracefully shutdown the agent."""
        logger.info("SHUTDOWN: Initiating graceful exit...")
        self.consumer.stop()
        self.producer.close()
        logger.info("SHUTDOWN: Agent components stopped. Goodbye!")


def main():
    agent = ThoughtsToPostAgent()
    agent.start()


if __name__ == "__main__":
    main()
