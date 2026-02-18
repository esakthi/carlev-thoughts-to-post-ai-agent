package com.carlev.thoughtstopost.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * History document for auditing changes to ThoughtsToPost.
 * Each modification creates a new history entry.
 */
@Document(collection = "thoughts_to_post_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtsToPostHistory {

    @Id
    private String id;

    /**
     * Reference to the original ThoughtsToPost document ID.
     */
    private String thoughtsToPostId;

    /**
     * Version number at the time of this snapshot.
     */
    private Long version;

    /**
     * Type of action that created this history entry.
     */
    private ActionType actionType;

    /**
     * User who performed the action.
     */
    private String performedBy;

    /**
     * Timestamp when this history entry was created.
     */
    @CreatedDate
    private LocalDateTime createdAt;

    // Snapshot of the document state at this version
    private String userId;
    private String originalThought;
    private String additionalInstructions;
    private List<ThoughtsToPost.PlatformSelection> platformSelections;
    private List<ThoughtsToPost.EnrichedContent> enrichedContents;
    private String generatedImageUrl;
    private List<PlatformType> selectedPlatforms;
    private PostStatus status;
    private String errorMessage;

    /**
     * Types of actions that create history entries.
     */
    public enum ActionType {
        CREATE,
        UPDATE,
        STATUS_CHANGE,
        APPROVE,
        REJECT,
        POST,
        DELETE
    }

    /**
     * Create a history entry from a ThoughtsToPost document.
     */
    public static ThoughtsToPostHistory fromThoughtsToPost(
            ThoughtsToPost thought,
            ActionType actionType,
            String performedBy) {
        return ThoughtsToPostHistory.builder()
                .thoughtsToPostId(thought.getId())
                .version(thought.getVersion())
                .actionType(actionType)
                .performedBy(performedBy)
                .userId(thought.getUserId())
                .originalThought(thought.getOriginalThought())
                .additionalInstructions(thought.getAdditionalInstructions())
                .platformSelections(thought.getPlatformSelections())
                .enrichedContents(thought.getEnrichedContents())
                .generatedImageUrl(thought.getGeneratedImageUrl())
                .selectedPlatforms(thought.getSelectedPlatforms())
                .status(thought.getStatus())
                .errorMessage(thought.getErrorMessage())
                .build();
    }
}
