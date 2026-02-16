"""Kafka producer for sending enrichment responses."""

import json
import logging
from datetime import datetime
from typing import Optional

from kafka import KafkaProducer as KafkaPythonProducer
from kafka.errors import KafkaError

from pydantic import BaseModel
from ..config import settings
from ..models import AgentResponse, SearchResponse

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

    def _serialize_value(self, value: dict) -> bytes:
        """Serialize response value to JSON with ISO-8601 date formatting.
        
        Args:
            value: Dictionary to serialize
            
        Returns:
            JSON-encoded bytes with ISO-8601 formatted dates
        """
        def json_serializer(obj):
            """Custom JSON serializer for datetime objects."""
            if isinstance(obj, datetime):
                return obj.isoformat()
            raise TypeError(f"Type {type(obj)} not serializable")
        
        return json.dumps(value, default=json_serializer).encode("utf-8")

    def _create_producer(self) -> KafkaPythonProducer:
        """Create and configure the Kafka producer."""
        return KafkaPythonProducer(
            bootstrap_servers=self.bootstrap_servers.split(","),
            value_serializer=self._serialize_value,
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

    def send(self, response: BaseModel, topic: Optional[str] = None) -> None:
        """Send a response to Kafka.

        Args:
            response: The response model to send
            topic: Optional topic override
        """
        if not self._producer:
            self.connect()

        target_topic = topic or self.topic

        # Determine key
        key = None
        if hasattr(response, "request_id"):
            key = response.request_id
        elif hasattr(response, "correlation_id"):
            key = response.correlation_id

        try:
            future = self._producer.send(
                target_topic,
                key=key,
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
