package com.carlev.thoughtstopost.kafka;

import com.carlev.thoughtstopost.service.ThoughtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
     */
    @KafkaListener(topics = "${app.kafka.response-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleResponse(@Payload ThoughtResponseMessage message) {
        log.info("Received AI agent response: requestId={}, status={}",
                message.getRequestId(), message.getStatus());

        try {
            thoughtsService.handleAgentResponse(message);
            log.info("Successfully processed AI agent response: requestId={}",
                    message.getRequestId());
        } catch (Exception e) {
            log.error("Error processing AI agent response: requestId={}, error={}",
                    message.getRequestId(), e.getMessage(), e);
        }
    }
}
