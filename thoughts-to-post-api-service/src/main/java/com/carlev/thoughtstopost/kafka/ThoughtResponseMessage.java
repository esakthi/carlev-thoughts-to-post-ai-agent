package com.carlev.thoughtstopost.kafka;

import com.carlev.thoughtstopost.model.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message received from Kafka with AI agent processing results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtResponseMessage {

    private String requestId;
    private String userId;
    private String status; // "completed" or "failed"
    private List<EnrichedContentMessage> enrichedContents;
    private GeneratedImageMessage generatedImage;
    private Integer version;
    private String errorMessage;
    private LocalDateTime processedAt;

    /**
     * Enriched content from AI agent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrichedContentMessage {
        private PlatformType platform;
        private String title;
        private String body;
        private List<String> hashtags;
        private String callToAction;
        private Integer characterCount;
    }

    /**
     * Generated image from AI agent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedImageMessage {
        private String imageBase64;
        private String imageFormat;
        private String promptUsed;
        private Integer width;
        private Integer height;
    }
}
