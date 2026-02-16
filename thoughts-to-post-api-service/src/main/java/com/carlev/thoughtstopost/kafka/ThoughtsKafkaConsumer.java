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
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThoughtsKafkaConsumer {

    private final ThoughtsService thoughtsService;
    private final ObjectMapper objectMapper;

    /**
     * Handle incoming response messages from the AI agent.
     *
     * @param messageJson The response message from AI agent as JSON string
     * @param key The Kafka message key
     */
    @KafkaListener(topics = "${app.kafka.response-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleResponse(
            @Payload(required = false) String messageJson,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        
        // Handle null messages
        if (messageJson == null) {
            log.warn("Received null message from Kafka (key: {})", key);
            return;
        }

        ThoughtResponseMessage message = null;
        try {
            message = objectMapper.readValue(messageJson, ThoughtResponseMessage.class);
            log.info("Received AI agent response: requestId={}, status={}",
                    message.getRequestId(), message.getStatus());

            thoughtsService.handleAgentResponse(message);
            log.info("Successfully processed AI agent response: requestId={}",
                    message.getRequestId());
        } catch (Exception e) {
            log.error("Error processing AI agent response: requestId={}, error={}",
                    message != null && message.getRequestId() != null ? message.getRequestId() : "unknown",
                    e.getMessage(), e);
            // Re-throw to let the error handler manage retries
            // throw e; // Commenting out to avoid infinite loop if the message is truly bad
        }
    }
}
