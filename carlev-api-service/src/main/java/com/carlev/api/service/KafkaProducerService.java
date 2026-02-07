package com.carlev.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer Service for sending events to Kafka topics
 */
@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.events}")
    private String eventsTopic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send event to Kafka topic
     * @param key Message key (typically event ID)
     * @param event Event object to send
     * @return CompletableFuture with send result
     */
    public CompletableFuture<SendResult<String, Object>> sendEvent(String key, Object event) {
        logger.info("Sending event to Kafka topic: {} with key: {}", eventsTopic, key);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(eventsTopic, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Event sent successfully to topic: {} with offset: {}", 
                    eventsTopic, result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send event to topic: {}", eventsTopic, ex);
            }
        });
        
        return future;
    }

    /**
     * Flush all pending messages in the producer
     */
    public void flush() {
        logger.info("Flushing Kafka producer...");
        kafkaTemplate.flush();
        logger.info("Kafka producer flushed successfully");
    }

    /**
     * Flush and close the producer (called during application shutdown)
     */
    public void flushAndClose() {
        logger.info("Flushing and closing Kafka producer...");
        try {
            kafkaTemplate.flush();
            // Note: KafkaTemplate manages producer lifecycle, so we don't explicitly close
            // Spring will handle cleanup during application shutdown
            logger.info("Kafka producer flushed successfully");
        } catch (Exception e) {
            logger.error("Error during Kafka producer flush and close", e);
        }
    }
}
