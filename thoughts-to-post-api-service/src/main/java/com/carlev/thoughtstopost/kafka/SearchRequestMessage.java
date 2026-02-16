package com.carlev.thoughtstopost.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka message for search requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestMessage {
    private String correlationId;
    private String type; // GENERATE_CRITERIA or EXECUTE_SEARCH
    private String category;
    private String description;
    private String searchString;
}
