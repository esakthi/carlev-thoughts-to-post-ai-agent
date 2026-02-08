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

    def _create_consumer(self) -> KafkaPythonConsumer:
        """Create and configure the Kafka consumer."""
        return KafkaPythonConsumer(
            self.topic,
            bootstrap_servers=self.bootstrap_servers.split(","),
            group_id=self.group_id,
            auto_offset_reset="earliest",
            enable_auto_commit=True,
            value_deserializer=lambda m: json.loads(m.decode("utf-8")),
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
                            logger.info(
                                f"Received message: key={record.key}, "
                                f"partition={topic_partition.partition}, "
                                f"offset={record.offset}"
                            )

                            # Parse the message into ThoughtRequest
                            thought_request = ThoughtRequest(**record.value)
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
