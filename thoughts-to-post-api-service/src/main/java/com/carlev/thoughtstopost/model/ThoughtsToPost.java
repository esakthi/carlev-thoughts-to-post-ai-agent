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

    private String originalThought;

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

        // Posting status tracking
        @Builder.Default
        private PostStatus status = PostStatus.PENDING;
        private String postId;
        @Builder.Default
        private Integer retryCount = 0;
        private LocalDateTime lastRetryAt;
        private String errorMessage;
    }
}
