package com.carlev.thoughtstopost.kafka;

import com.carlev.thoughtstopost.service.ThoughtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for receiving AI agent processing results.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ThoughtsKafkaConsumer {

    private final ThoughtsService thoughtsService;

    /**
     * Handle incoming response messages from the AI agent.
     *
     * @param message The response message from AI agent
     * @param key The Kafka message key
     */
    @KafkaListener(topics = "${app.kafka.response-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleResponse(
            @Payload(required = false) ThoughtResponseMessage message,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        
        // Handle null messages (can occur if deserialization fails)
        if (message == null) {
            log.warn("Received null message from Kafka (key: {}). This may indicate a deserialization error. " +
                    "The error handler will manage retries and skip this message if it cannot be processed.", key);
            // Throw exception to let error handler manage retries
            throw new IllegalArgumentException("Cannot process null message. Deserialization may have failed.");
        }

        log.info("Received AI agent response: requestId={}, status={}",
                message.getRequestId(), message.getStatus());

        try {
            thoughtsService.handleAgentResponse(message);
            log.info("Successfully processed AI agent response: requestId={}",
                    message.getRequestId());
        } catch (Exception e) {
            log.error("Error processing AI agent response: requestId={}, error={}",
                    message.getRequestId() != null ? message.getRequestId() : "unknown", 
                    e.getMessage(), e);
            // Re-throw to let the error handler manage retries
            throw e;
        }
    }
}
