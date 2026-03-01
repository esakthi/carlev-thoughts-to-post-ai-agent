package com.carlev.thoughtstopost.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main document entity for storing thought posts.
 */
@Document(collection = "thoughts_to_post")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtsToPost {

    @Id
    private String id;

    private String userId;

    private String categoryId;

    private String originalThought;

    private String additionalInstructions;

    @Builder.Default
    private List<PlatformSelection> platformSelections = new ArrayList<>();

    @Builder.Default
    private List<EnrichedContent> enrichedContents = new ArrayList<>();

    private String generatedImageBase64;

    private String generatedImageUrl;

    @Builder.Default
    private List<PlatformType> selectedPlatforms = new ArrayList<>();

    @Builder.Default
    private PostStatus status = PostStatus.PENDING;

    @Version
    private Long version;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    private String errorMessage;

    private String textContentComments;

    private String imageContentComments;

    @Builder.Default
    private boolean postText = true;

    @Builder.Default
    private boolean postImage = true;

    /**
     * Selection details for each platform.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformSelection {
        private PlatformType platform;
        private String presetId;
        private String imagePresetId;
        private String videoPresetId;
        private String additionalContext;
        private com.carlev.thoughtstopost.kafka.GenerationParameters imageParams;
        private com.carlev.thoughtstopost.kafka.GenerationParameters videoParams;
    }

    /**
     * Nested class for platform-specific enriched content.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrichedContent {
        private PlatformType platform;
        private String title;
        private String body;
        @Builder.Default
        private List<String> hashtags = new ArrayList<>();
        private String callToAction;
        private Integer characterCount;

        // Platform-specific images
        @Builder.Default
        private List<GeneratedImage> images = new ArrayList<>();

        // Posting status tracking
        @Builder.Default
        private PostStatus status = PostStatus.PENDING;
        private String postId;
        @Builder.Default
        private Integer retryCount = 0;
        private LocalDateTime lastRetryAt;
        private String errorMessage;
        @Builder.Default
        private Double progress = 0.0;
    }

    /**
     * Represents a generated image.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedImage {
        private String id;
        private String base64Data;
        private String url; // Data URI or cloud URL
        private String prompt;
        private String format;
        private Integer width;
        private Integer height;
        @Builder.Default
        private boolean selected = false;
        private String tag; // User-defined tag/position (e.g., "pre-explanation")
        private LocalDateTime createdAt;
    }
}
