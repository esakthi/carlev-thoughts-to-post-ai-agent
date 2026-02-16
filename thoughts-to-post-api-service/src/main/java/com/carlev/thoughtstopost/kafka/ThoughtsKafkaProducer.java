package com.carlev.thoughtstopost.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for sending thought enrichment requests to the AI agent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ThoughtsKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.request-topic}")
    private String requestTopic;

    /**
     * Send a thought request to the AI agent.
     *
     * @param message The request message to send
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, Object>> sendRequest(
            ThoughtRequestMessage message) {
        log.info("Sending thought request to Kafka: requestId={}", message.getRequestId());

        return kafkaTemplate.send(requestTopic, message.getRequestId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message: {}", ex.getMessage(), ex);
                    } else {
                        log.info("Message sent successfully: topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
