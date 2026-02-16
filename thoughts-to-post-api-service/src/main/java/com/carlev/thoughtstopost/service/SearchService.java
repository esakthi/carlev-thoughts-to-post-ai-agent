package com.carlev.thoughtstopost.service;

import com.carlev.thoughtstopost.dto.SearchCriteriaRequest;
import com.carlev.thoughtstopost.kafka.SearchRequestMessage;
import com.carlev.thoughtstopost.kafka.SearchResponseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling internet searches via Kafka and AI Agent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.search-request-topic}")
    private String searchRequestTopic;

    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Generate a search string based on category and description.
     */
    public CompletableFuture<String> generateSearchString(SearchCriteriaRequest request) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        SearchRequestMessage message = SearchRequestMessage.builder()
                .correlationId(correlationId)
                .type("GENERATE_CRITERIA")
                .category(request.getCategory())
                .description(request.getDescription())
                .build();

        log.info("Sending GENERATE_CRITERIA request to Kafka: correlationId={}", correlationId);
        kafkaTemplate.send(searchRequestTopic, correlationId, message);

        // Add timeout
        future.orTimeout(30, TimeUnit.SECONDS).whenComplete((res, ex) -> {
            pendingRequests.remove(correlationId);
            if (ex != null) {
                log.error("Search criteria generation timed out or failed for correlationId={}", correlationId);
            }
        });

        return future;
    }

    /**
     * Execute an internet search with the given search string.
     */
    public CompletableFuture<String> executeSearch(String searchString) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        SearchRequestMessage message = SearchRequestMessage.builder()
                .correlationId(correlationId)
                .type("EXECUTE_SEARCH")
                .searchString(searchString)
                .build();

        log.info("Sending EXECUTE_SEARCH request to Kafka: correlationId={}", correlationId);
        kafkaTemplate.send(searchRequestTopic, correlationId, message);

        // Add timeout
        future.orTimeout(60, TimeUnit.SECONDS).whenComplete((res, ex) -> {
            pendingRequests.remove(correlationId);
            if (ex != null) {
                log.error("Internet search timed out or failed for correlationId={}", correlationId);
            }
        });

        return future;
    }

    /**
     * Listen for search responses from Kafka.
     */
    @KafkaListener(topics = "${app.kafka.search-response-topic}")
    public void handleSearchResponse(String messageJson) {
        try {
            SearchResponseMessage response = objectMapper.readValue(messageJson, SearchResponseMessage.class);
            log.info("Received search response for correlationId={}", response.getCorrelationId());
            CompletableFuture<String> future = pendingRequests.remove(response.getCorrelationId());
            if (future != null) {
                if (response.getError() != null) {
                    future.completeExceptionally(new RuntimeException(response.getError()));
                } else {
                    future.complete(response.getResult());
                }
            } else {
                log.warn("Received search response for unknown or timed-out correlationId: {}", response.getCorrelationId());
            }
        } catch (Exception e) {
            log.error("Error deserializing search response: {}", e.getMessage());
        }
    }
}
