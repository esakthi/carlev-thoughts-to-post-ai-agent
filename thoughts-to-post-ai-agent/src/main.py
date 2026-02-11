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
)

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

    def process_request(self, request: ThoughtRequest) -> None:
        """Process a single thought enrichment request.

        Args:
            request: The thought request to process
        """
        logger.info(f"Processing request: {request.request_id}")

        # Create or get existing context
        context = self.memory.get_context(request.request_id)
        if not context:
            context = self.memory.create_context(request)

        try:
            # Update status to processing
            self.memory.update_context(
                request.request_id, status=RequestStatus.PROCESSING
            )

            # Step 1: Enrich content for all platforms
            logger.info(f"Enriching content for platforms: {request.platforms}")
            enriched_contents = self.content_agent.enrich_all_platforms(request)

            if not enriched_contents and request.platforms:
                msg = f"Failed to enrich content for any of the requested platforms: {request.platforms}"
                logger.error(msg)
                raise Exception(msg)

            # Update context with enriched contents
            self.memory.update_context(
                request.request_id, enriched_contents=enriched_contents
            )

            # Step 2: Generate image based on the first platform's content
            generated_image = None
            if enriched_contents:
                logger.info("Generating image for content...")
                try:
                    generated_image = self.image_agent.generate_for_content(
                        enriched_contents[0]
                    )
                    self.memory.update_context(
                        request.request_id, generated_image=generated_image
                    )
                except Exception as e:
                    logger.warning(f"Image generation failed, continuing without image: {e}")

            # Mark as completed
            self.memory.update_context(
                request.request_id, status=RequestStatus.COMPLETED
            )

            # Create and send response
            response = AgentResponse(
                request_id=request.request_id,
                user_id=request.user_id,
                status=RequestStatus.COMPLETED,
                enriched_contents=enriched_contents,
                generated_image=generated_image,
                version=context.current_version,
            )

            self.producer.send(response)
            logger.info(f"Successfully processed request: {request.request_id}")

        except Exception as e:
            logger.error(f"Failed to process request {request.request_id}: {e}", exc_info=True)

            # Update context with error
            self.memory.update_context(
                request.request_id,
                status=RequestStatus.FAILED,
                error_message=str(e),
            )

            # Send error response
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
        logger.info("=" * 60)
        logger.info("Starting Thoughts-to-Post AI Agent")
        logger.info(f"Kafka Bootstrap Servers: {settings.kafka_bootstrap_servers}")
        logger.info(f"Request Topic: {settings.kafka_request_topic}")
        logger.info(f"Response Topic: {settings.kafka_response_topic}")
        logger.info(f"Ollama Model: {settings.ollama_model}")
        logger.info(f"Image Generator: {settings.image_generator_type}")
        logger.info("=" * 60)

        self._setup_signal_handlers()

        try:
            # Connect the producer
            self.producer.connect()

            # Start consuming messages (blocking)
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
        logger.info("AI Agent shutdown complete")


def main():
    """Main entry point."""
    agent = ThoughtsToPostAgent()
    agent.start()


if __name__ == "__main__":
    main()
