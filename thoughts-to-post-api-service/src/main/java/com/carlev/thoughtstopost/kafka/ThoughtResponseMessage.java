package com.carlev.thoughtstopost.kafka;

import com.carlev.thoughtstopost.model.PlatformType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties (like snake_case fields we don't need)
public class ThoughtResponseMessage {

    @JsonProperty("request_id") // Map from snake_case to camelCase
    private String requestId;
    
    @JsonProperty("user_id") // Map from snake_case to camelCase
    private String userId;
    
    private String status; // "in_progress", "completed", "partially_completed", or "failed"
    
    @JsonProperty("enriched_contents") // Map from snake_case to camelCase
    private List<EnrichedContentMessage> enrichedContents;
    
    @JsonProperty("generated_image") // Map from snake_case to camelCase
    private GeneratedImageMessage generatedImage; // Legacy support

    @JsonProperty("failed_platforms")
    private List<PlatformType> failedPlatforms;
    
    private Integer version;
    
    @JsonProperty("error_message") // Map from snake_case to camelCase
    private String errorMessage;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonProperty("processed_at") // Map from snake_case to camelCase
    private LocalDateTime processedAt;

    /**
     * Enriched content from AI agent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EnrichedContentMessage {
        private PlatformType platform;
        private String title;
        private String body;
        private List<String> hashtags;
        
        @JsonProperty("call_to_action")
        private String callToAction;
        
        @JsonProperty("character_count")
        private Integer characterCount;

        private Double progress;

        @JsonProperty("images")
        private List<GeneratedImageMessage> images;
    }

    /**
     * Generated image from AI agent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeneratedImageMessage {
        @JsonProperty("id")
        private String id;

        @JsonProperty("image_base64")
        private String imageBase64;
        
        @JsonProperty("image_format")
        private String imageFormat;
        
        @JsonProperty("prompt_used")
        private String promptUsed;
        
        private Integer width;
        private Integer height;

        @JsonProperty("tag")
        private String tag;

        @JsonProperty("created_at")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime createdAt;
    }
}
