"""Kafka producer for sending enrichment responses."""

import json
import logging
from typing import Optional

from kafka import KafkaProducer as KafkaPythonProducer
from kafka.errors import KafkaError

from ..config import settings
from ..models import AgentResponse

logger = logging.getLogger(__name__)


class KafkaResponseProducer:
    """Produces enrichment responses to Kafka."""

    def __init__(
        self,
        bootstrap_servers: Optional[str] = None,
        topic: Optional[str] = None,
    ):
        """Initialize the Kafka producer.

        Args:
            bootstrap_servers: Kafka bootstrap servers (defaults to settings)
            topic: Topic to produce to (defaults to settings)
        """
        self.bootstrap_servers = bootstrap_servers or settings.kafka_bootstrap_servers
        self.topic = topic or settings.kafka_response_topic
        self._producer: Optional[KafkaPythonProducer] = None

    def _create_producer(self) -> KafkaPythonProducer:
        """Create and configure the Kafka producer."""
        return KafkaPythonProducer(
            bootstrap_servers=self.bootstrap_servers.split(","),
            value_serializer=lambda v: json.dumps(v, default=str).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8") if k else None,
            acks="all",
            retries=3,
            max_in_flight_requests_per_connection=1,
        )

    def connect(self) -> None:
        """Connect to Kafka."""
        if not self._producer:
            self._producer = self._create_producer()
            logger.info(f"Kafka producer connected to {self.bootstrap_servers}")

    def send(self, response: AgentResponse) -> None:
        """Send an enrichment response to Kafka.

        Args:
            response: The AgentResponse to send
        """
        if not self._producer:
            self.connect()

        try:
            # Use request_id as the message key for partitioning
            future = self._producer.send(
                self.topic,
                key=response.request_id,
                value=response.model_dump(),
            )

            # Wait for the message to be sent
            record_metadata = future.get(timeout=10)

            logger.info(
                f"Sent response for request {response.request_id} "
                f"to {record_metadata.topic}:{record_metadata.partition} "
                f"at offset {record_metadata.offset}"
            )

        except KafkaError as e:
            logger.error(f"Failed to send response: {e}", exc_info=True)
            raise

    def flush(self) -> None:
        """Flush any pending messages."""
        if self._producer:
            self._producer.flush()

    def close(self) -> None:
        """Close the producer gracefully."""
        if self._producer:
            logger.info("Closing Kafka producer...")
            self._producer.flush()
            self._producer.close()
            self._producer = None
            logger.info("Kafka producer closed.")
