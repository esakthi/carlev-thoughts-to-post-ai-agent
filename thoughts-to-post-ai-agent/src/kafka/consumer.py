"""Kafka consumer for receiving thought enrichment requests."""

import json
import logging
from typing import Callable, Optional

from kafka import KafkaConsumer as KafkaPythonConsumer
from kafka.errors import KafkaError

from ..config import settings
from ..models import ThoughtRequest

logger = logging.getLogger(__name__)


class KafkaRequestConsumer:
    """Consumes thought enrichment requests from Kafka."""

    def __init__(
        self,
        bootstrap_servers: Optional[str] = None,
        topic: Optional[str] = None,
        group_id: Optional[str] = None,
    ):
        """Initialize the Kafka consumer.

        Args:
            bootstrap_servers: Kafka bootstrap servers (defaults to settings)
            topic: Topic to consume from (defaults to settings)
            group_id: Consumer group ID (defaults to settings)
        """
        self.bootstrap_servers = bootstrap_servers or settings.kafka_bootstrap_servers
        self.topic = topic or settings.kafka_request_topic
        self.group_id = group_id or settings.kafka_consumer_group
        self._consumer: Optional[KafkaPythonConsumer] = None
        self._running = False

    def _deserialize_value(self, message: bytes) -> Optional[dict]:
        """Safely deserialize a Kafka message value.
        
        Args:
            message: Raw message bytes
            
        Returns:
            Deserialized JSON dict, or None if message is empty/invalid
        """
        if not message:
            return None
        
        try:
            decoded = message.decode("utf-8")
            if not decoded.strip():
                return None
            return json.loads(decoded)
        except (json.JSONDecodeError, UnicodeDecodeError) as e:
            logger.warning(f"Failed to deserialize message: {e}")
            return None

    def _create_consumer(self) -> KafkaPythonConsumer:
        """Create and configure the Kafka consumer."""
        return KafkaPythonConsumer(
            self.topic,
            bootstrap_servers=self.bootstrap_servers.split(","),
            group_id=self.group_id,
            auto_offset_reset="earliest",
            enable_auto_commit=True,
            value_deserializer=self._deserialize_value,
            key_deserializer=lambda k: k.decode("utf-8") if k else None,
            max_poll_interval_ms=300000,  # 5 minutes
            session_timeout_ms=30000,
        )

    def start(self, message_handler: Callable[[ThoughtRequest], None]) -> None:
        """Start consuming messages and process them with the handler.

        Args:
            message_handler: Callback function to process each ThoughtRequest
        """
        self._consumer = self._create_consumer()
        self._running = True

        logger.info(
            f"Starting Kafka consumer on topic '{self.topic}' "
            f"with group '{self.group_id}'"
        )

        try:
            while self._running:
                # Poll for messages with a timeout
                messages = self._consumer.poll(timeout_ms=1000)

                for topic_partition, records in messages.items():
                    for record in records:
                        try:
                            # Log comprehensive message details
                            logger.info("=" * 80)
                            logger.info("KAFKA MESSAGE RECEIVED")
                            logger.info("=" * 80)
                            logger.info(f"Topic: {topic_partition.topic}")
                            logger.info(f"Partition: {topic_partition.partition}")
                            logger.info(f"Offset: {record.offset}")
                            logger.info(f"Timestamp: {record.timestamp}")
                            logger.info(f"Timestamp Type: {record.timestamp_type}")
                            logger.info(f"Key: {record.key}")
                            
                            # Log headers if present
                            if record.headers:
                                logger.info("Headers:")
                                for header_key, header_value in record.headers:
                                    header_key_str = header_key.decode("utf-8") if isinstance(header_key, bytes) else header_key
                                    header_value_str = header_value.decode("utf-8") if isinstance(header_value, bytes) else header_value
                                    logger.info(f"  {header_key_str}: {header_value_str}")
                            else:
                                logger.info("Headers: None")
                            
                            # Log message value
                            logger.info("Message Value:")
                            if record.value is None:
                                logger.warning("  Value is None (empty or invalid message)")
                            else:
                                try:
                                    # Pretty print JSON if it's a dict
                                    if isinstance(record.value, dict):
                                        value_json = json.dumps(record.value, indent=2, ensure_ascii=False)
                                        logger.info(f"  {value_json}")
                                    else:
                                        logger.info(f"  {record.value}")
                                except Exception as e:
                                    logger.warning(f"  Could not format value: {e}")
                                    logger.info(f"  Raw value: {record.value}")
                            
                            logger.info("=" * 80)

                            # Skip empty or invalid messages
                            if record.value is None:
                                logger.warning(
                                    f"Skipping empty/invalid message at offset {record.offset}"
                                )
                                continue

                            # Parse the message into ThoughtRequest
                            logger.info(f"Before parsing ThoughtRequest: Record value: {record.value}")
                            thought_request = ThoughtRequest(**record.value)
                            logger.info(f"Successfully parsed ThoughtRequest: request_id={thought_request.request_id}")
                            message_handler(thought_request)

                        except Exception as e:
                            logger.error(
                                f"Error processing message at offset {record.offset}: {e}",
                                exc_info=True,
                            )

        except KafkaError as e:
            logger.error(f"Kafka error: {e}", exc_info=True)
            raise
        finally:
            self.stop()

    def stop(self) -> None:
        """Stop the consumer gracefully."""
        self._running = False
        if self._consumer:
            logger.info("Closing Kafka consumer...")
            self._consumer.close()
            self._consumer = None
            logger.info("Kafka consumer closed.")
