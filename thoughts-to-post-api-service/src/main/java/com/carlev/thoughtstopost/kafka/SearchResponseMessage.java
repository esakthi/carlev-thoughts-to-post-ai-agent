package com.carlev.thoughtstopost.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka message for search responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseMessage {
    private String correlationId;
    private String result;
    private String error;
}
