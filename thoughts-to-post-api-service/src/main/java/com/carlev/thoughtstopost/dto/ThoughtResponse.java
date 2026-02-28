package com.carlev.thoughtstopost.dto;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for thought post data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtResponse {

    private String id;
    private String userId;
    private String categoryId;
    private String originalThought;
    private String additionalInstructions;
    private List<PlatformSelectionDto> platformSelections;
    private List<EnrichedContentDto> enrichedContents;
    private String generatedImageUrl; // Legacy
    private List<PlatformType> selectedPlatforms;
    private PostStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String errorMessage;
    private String textContentComments;
    private String imageContentComments;
    private boolean postText;
    private boolean postImage;

    /**
     * DTO for platform selection.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformSelectionDto {
        private PlatformType platform;
        private String presetId;
        private String additionalContext;
    }

    /**
     * DTO for enriched content.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrichedContentDto {
        private PlatformType platform;
        private String title;
        private String body;
        private List<String> hashtags;
        private String callToAction;
        private Integer characterCount;
        private List<GeneratedImageDto> images;
        private PostStatus status;
        private String errorMessage;
    }

    /**
     * DTO for generated image.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedImageDto {
        private String id;
        private String url;
        private String prompt;
        private String format;
        private Integer width;
        private Integer height;
        private boolean selected;
        private String tag;
        private LocalDateTime createdAt;
    }

    /**
     * Convert from entity to DTO.
     */
    public static ThoughtResponse fromEntity(ThoughtsToPost entity) {
        List<PlatformSelectionDto> selectionDtos = entity.getPlatformSelections() != null
                ? entity.getPlatformSelections().stream()
                        .map(ps -> PlatformSelectionDto.builder()
                                .platform(ps.getPlatform())
                                .presetId(ps.getPresetId())
                                .additionalContext(ps.getAdditionalContext())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        List<EnrichedContentDto> enrichedDtos = entity.getEnrichedContents() != null
                ? entity.getEnrichedContents().stream()
                        .map(ec -> EnrichedContentDto.builder()
                                .platform(ec.getPlatform())
                                .title(ec.getTitle())
                                .body(ec.getBody())
                                .hashtags(ec.getHashtags())
                                .callToAction(ec.getCallToAction())
                                .characterCount(ec.getCharacterCount())
                                .status(ec.getStatus())
                                .errorMessage(ec.getErrorMessage())
                                .images(ec.getImages() != null ? ec.getImages().stream()
                                        .map(img -> GeneratedImageDto.builder()
                                                .id(img.getId())
                                                .url(img.getUrl())
                                                .prompt(img.getPrompt())
                                                .format(img.getFormat())
                                                .width(img.getWidth())
                                                .height(img.getHeight())
                                                .selected(img.isSelected())
                                                .tag(img.getTag())
                                                .createdAt(img.getCreatedAt())
                                                .build())
                                        .collect(Collectors.toList()) : List.of())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        return ThoughtResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .categoryId(entity.getCategoryId())
                .originalThought(entity.getOriginalThought())
                .additionalInstructions(entity.getAdditionalInstructions())
                .platformSelections(selectionDtos)
                .enrichedContents(enrichedDtos)
                .generatedImageUrl(entity.getGeneratedImageUrl())
                .selectedPlatforms(entity.getSelectedPlatforms())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .errorMessage(entity.getErrorMessage())
                .textContentComments(entity.getTextContentComments())
                .imageContentComments(entity.getImageContentComments())
                .postText(entity.isPostText())
                .postImage(entity.isPostImage())
                .build();
    }
}
