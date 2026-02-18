package com.carlev.thoughtstopost.service;

import com.carlev.thoughtstopost.dto.CreateThoughtRequest;
import com.carlev.thoughtstopost.dto.ThoughtResponse;
import com.carlev.thoughtstopost.kafka.ThoughtRequestMessage;
import com.carlev.thoughtstopost.kafka.ThoughtResponseMessage;
import com.carlev.thoughtstopost.kafka.ThoughtsKafkaProducer;
import com.carlev.thoughtstopost.model.PlatformPrompt;
import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtCategory;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.model.ThoughtsToPostHistory;
import com.carlev.thoughtstopost.repository.PlatformPromptRepository;
import com.carlev.thoughtstopost.repository.ThoughtCategoryRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostHistoryRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import com.carlev.thoughtstopost.social.SocialMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing thought posts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThoughtsService {

    private final ThoughtsToPostRepository thoughtsRepository;
    private final ThoughtsToPostHistoryRepository historyRepository;
    private final ThoughtsKafkaProducer kafkaProducer;
    private final SocialMediaService socialMediaService;
    private final ThoughtCategoryRepository categoryRepository;
    private final PlatformPromptRepository platformPromptRepository;

    /**
     * Create a new thought post and send it to the AI agent for enrichment.
     *
     * @param request The create request
     * @param userId  The user ID
     * @return The created thought response
     */
    @Transactional
    public ThoughtResponse createThought(CreateThoughtRequest request, String userId) {
        log.info("Creating new thought for user: {}", userId);

        List<ThoughtsToPost.PlatformSelection> selections = new ArrayList<>();
        List<PlatformType> selectedPlatforms = new ArrayList<>();

        if (request.getPlatformConfigs() != null && !request.getPlatformConfigs().isEmpty()) {
            for (CreateThoughtRequest.PlatformConfig config : request.getPlatformConfigs()) {
                selections.add(ThoughtsToPost.PlatformSelection.builder()
                        .platform(config.getPlatform())
                        .presetId(config.getPresetId())
                        .additionalContext(config.getAdditionalContext())
                        .build());
                selectedPlatforms.add(config.getPlatform());
            }
        } else if (request.getPlatforms() != null) {
            // Fallback for backward compatibility if only platforms list is provided
            for (PlatformType platform : request.getPlatforms()) {
                selections.add(ThoughtsToPost.PlatformSelection.builder()
                        .platform(platform)
                        .build());
                selectedPlatforms.add(platform);
            }
        }

        // Create the document
        ThoughtsToPost thought = ThoughtsToPost.builder()
                .userId(userId)
                .categoryId(request.getCategoryId())
                .originalThought(request.getThought())
                .additionalInstructions(request.getAdditionalInstructions())
                .platformSelections(selections)
                .selectedPlatforms(selectedPlatforms)
                .status(PostStatus.PENDING)
                .createdBy(userId)
                .build();

        // Save to MongoDB
        thought = thoughtsRepository.save(thought);

        // Create history entry
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.CREATE, userId);

        // Send to Kafka for AI processing
        sendToAiAgent(thought, request.getAdditionalInstructions());

        log.info("Created thought with ID: {}", thought.getId());
        return ThoughtResponse.fromEntity(thought);
    }

    /**
     * Get a thought by ID for a specific user.
     */
    public ThoughtResponse getThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));
        // print this whole object as json in the logs
        log.info("Thought shared by User to AI for enriching has been Retrieved from MongoDB, thought Details: {}",
                thought);
        return ThoughtResponse.fromEntity(thought);
    }

    /**
     * Get all thoughts for a user.
     */
    public List<ThoughtResponse> getUserThoughts(String userId) {
        return thoughtsRepository.findByUserId(userId).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get thoughts for a user filtered by platform.
     */
    public List<ThoughtResponse> getUserThoughtsByPlatform(String userId, PlatformType platform) {
        return thoughtsRepository.findByUserIdAndSelectedPlatformsContains(userId, platform).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get thoughts for a user filtered by status.
     */
    public List<ThoughtResponse> getUserThoughtsByStatus(String userId, PostStatus status) {
        return thoughtsRepository.findByUserIdAndStatus(userId, status).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get thoughts for a user where status is not equal to provided status.
     */
    public List<ThoughtResponse> getUserThoughtsByStatusNot(String userId, PostStatus status) {
        return thoughtsRepository.findByUserIdAndStatusNot(userId, status).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get history for a thought for a specific user.
     */
    public List<ThoughtsToPostHistory> getThoughtHistory(String thoughtId, String userId) {
        // Verify thought exists and belongs to user first
        thoughtsRepository.findByIdAndUserId(thoughtId, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + thoughtId));

        return historyRepository.findByThoughtsToPostIdOrderByVersionDesc(thoughtId);
    }

    /**
     * Approve a thought and post to social media.
     */
    @Transactional
    public ThoughtResponse approveAndPost(String id, com.carlev.thoughtstopost.dto.ApproveThoughtRequest request, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() != PostStatus.ENRICHED && thought.getStatus() != PostStatus.FAILED) {
            throw new RuntimeException("Thought is not ready for approval. Status: " + thought.getStatus());
        }

        // Update status to approved and store user choices/comments
        thought.setStatus(PostStatus.APPROVED);
        thought.setUpdatedBy(userId);
        thought.setTextContentComments(request.getTextContentComments());
        thought.setImageContentComments(request.getImageContentComments());
        thought.setPostText(request.isPostText());
        thought.setPostImage(request.isPostImage());

        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.APPROVE, userId);

        // Initial attempt to post
        attemptPosting(id);

        return ThoughtResponse.fromEntity(thoughtsRepository.findById(id).orElse(thought));
    }

    /**
     * Attempt to post a thought to all selected platforms.
     * Can be called from the controller or the scheduler.
     */
    public void attemptPosting(String id) {
        ThoughtsToPost thought = thoughtsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() != PostStatus.APPROVED && thought.getStatus() != PostStatus.POSTING
                && thought.getStatus() != PostStatus.FAILED) {
            log.warn("Thought {} is not in a postable state: {}", id, thought.getStatus());
            return;
        }

        try {
            // Check if all platforms already posted
            boolean allPosted = thought.getEnrichedContents().stream()
                    .allMatch(c -> c.getStatus() == PostStatus.POSTED);

            if (allPosted) {
                thought.setStatus(PostStatus.POSTED);
                thoughtsRepository.save(thought);
                return;
            }

            thought.setStatus(PostStatus.POSTING);
            thoughtsRepository.save(thought);

            socialMediaService.postToSelectedPlatforms(thought);

            // Re-fetch or use updated object from service (which saves it)
            thought = thoughtsRepository.findById(id).orElse(thought);

            // Check again if all posted after service call
            boolean fullyPosted = thought.getEnrichedContents().stream()
                    .allMatch(c -> c.getStatus() == PostStatus.POSTED);

            if (fullyPosted) {
                thought.setStatus(PostStatus.POSTED);
                thought = thoughtsRepository.save(thought);
                createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.POST, "system");
            } else {
                thought.setStatus(PostStatus.FAILED);
                thoughtsRepository.save(thought);
            }

            log.info("Finished attempt to post thought: {}", id);
        } catch (Exception e) {
            log.error("Failed post attempt for thought {}: {}", id, e.getMessage());
            thought.setStatus(PostStatus.FAILED);
            thought.setErrorMessage("Post attempt failed: " + e.getMessage());
            thoughtsRepository.save(thought);
        }
    }

    /**
     * Reject a thought.
     */
    @Transactional
    public ThoughtResponse rejectThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        thought.setStatus(PostStatus.REJECTED);
        thought.setUpdatedBy(userId);
        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.REJECT, userId);

        log.info("Rejected thought: {}", id);
        return ThoughtResponse.fromEntity(thought);
    }

    /**
     * Update enriched content of a thought manually.
     */
    @Transactional
    public ThoughtResponse updateEnrichedContent(String id, ThoughtResponse request, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() == PostStatus.POSTED) {
            throw new RuntimeException("Cannot edit content after it has been posted.");
        }

        // Update enriched contents
        if (request.getEnrichedContents() != null) {
            List<ThoughtsToPost.EnrichedContent> updatedContents = request.getEnrichedContents().stream()
                    .map(dto -> {
                        // Find existing content to preserve status, postId etc.
                        ThoughtsToPost.EnrichedContent existing = thought.getEnrichedContents().stream()
                                .filter(ec -> ec.getPlatform() == dto.getPlatform())
                                .findFirst()
                                .orElse(new ThoughtsToPost.EnrichedContent());

                        existing.setPlatform(dto.getPlatform());
                        existing.setTitle(dto.getTitle());
                        existing.setBody(dto.getBody());
                        existing.setHashtags(dto.getHashtags());
                        existing.setCallToAction(dto.getCallToAction());
                        existing.setCharacterCount(dto.getCharacterCount());
                        return existing;
                    })
                    .collect(Collectors.toList());
            thought.setEnrichedContents(updatedContents);
        }

        thought.setUpdatedBy(userId);
        ThoughtsToPost savedThought = thoughtsRepository.save(thought);
        createHistoryEntry(savedThought, ThoughtsToPostHistory.ActionType.UPDATE, userId);

        return ThoughtResponse.fromEntity(savedThought);
    }

    /**
     * Resubmit a thought for re-enrichment by the AI agent.
     */
    @Transactional
    public ThoughtResponse reenrichThought(String id, String additionalInstructions, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() == PostStatus.POSTED) {
            throw new RuntimeException("Cannot re-enrich content after it has been posted.");
        }

        thought.setUpdatedBy(userId);

        ThoughtsToPost savedThought = thoughtsRepository.save(thought);
        createHistoryEntry(savedThought, ThoughtsToPostHistory.ActionType.UPDATE, userId);

        // Send to Kafka for AI processing
        sendToAiAgent(savedThought, additionalInstructions);

        return ThoughtResponse.fromEntity(thoughtsRepository.findById(id).orElse(savedThought));
    }

    /**
     * Delete a thought.
     */
    @Transactional
    public void deleteThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        // Save history before deleting
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.DELETE, userId);

        thoughtsRepository.delete(thought);
        log.info("Deleted thought: {}", id);
    }

    /**
     * Repost an existing thought by resetting it to PENDING.
     */
    @Transactional
    public ThoughtResponse repostThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        // Reset status and enrichment data
        thought.setStatus(PostStatus.PENDING);
        thought.setEnrichedContents(new java.util.ArrayList<>());
        thought.setGeneratedImageUrl(null);
        thought.setGeneratedImageBase64(null);
        thought.setErrorMessage(null);
        thought.setUpdatedBy(userId);

        ThoughtsToPost savedThought = thoughtsRepository.save(thought);
        createHistoryEntry(savedThought, ThoughtsToPostHistory.ActionType.UPDATE, userId);

        // Send to Kafka for AI processing
        sendToAiAgent(savedThought, "Reposting this thought.");

        log.info("Reposted thought: {}", id);
        return ThoughtResponse.fromEntity(savedThought);
    }

    /**
     * Handle AI agent response from Kafka.
     */
    @Transactional
    public void handleAgentResponse(ThoughtResponseMessage message) {
        log.info("Handling AI agent response for request: {} with status: {}", message.getRequestId(), message.getStatus());

        ThoughtsToPost thought = thoughtsRepository.findById(message.getRequestId())
                .orElseThrow(() -> new RuntimeException("Thought not found: " + message.getRequestId()));

        // Update enriched contents incrementally
        if (message.getEnrichedContents() != null) {
            for (ThoughtResponseMessage.EnrichedContentMessage ec : message.getEnrichedContents()) {
                // Remove existing content for this platform if any
                thought.getEnrichedContents().removeIf(existing -> existing.getPlatform() == ec.getPlatform());

                // Add new content
                thought.getEnrichedContents().add(ThoughtsToPost.EnrichedContent.builder()
                        .platform(ec.getPlatform())
                        .title(ec.getTitle())
                        .body(ec.getBody())
                        .hashtags(ec.getHashtags())
                        .callToAction(ec.getCallToAction())
                        .characterCount(ec.getCharacterCount())
                        .status(PostStatus.PENDING) // Ready for approval
                        .build());
            }
        }

        // Update with generated image if provided
        if (message.getGeneratedImage() != null) {
            thought.setGeneratedImageBase64(message.getGeneratedImage().getImageBase64());
            thought.setGeneratedImageUrl("data:image/" + message.getGeneratedImage().getImageFormat()
                    + ";base64," + message.getGeneratedImage().getImageBase64());
        }

        // Update overall status
        String incomingStatus = message.getStatus().toLowerCase();
        if ("completed".equals(incomingStatus)) {
            thought.setStatus(PostStatus.ENRICHED);
            log.info("Thought fully enriched: {}", thought.getId());
        } else if ("in_progress".equals(incomingStatus)) {
            thought.setStatus(PostStatus.PROCESSING);
            log.info("Thought enrichment in progress: {}", thought.getId());
        } else if ("partially_completed".equals(incomingStatus)) {
            thought.setStatus(PostStatus.PARTIALLY_COMPLETED);
            thought.setErrorMessage(message.getErrorMessage());
            log.info("Thought enrichment partially completed: {}", thought.getId());
        } else {
            thought.setStatus(PostStatus.FAILED);
            thought.setErrorMessage(message.getErrorMessage());
            log.error("AI agent processing failed for {}: {}", thought.getId(), message.getErrorMessage());
        }

        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.STATUS_CHANGE, "system");
    }

    /**
     * Send thought to AI agent via Kafka.
     */
    private void sendToAiAgent(ThoughtsToPost thought, String additionalInstructions) {
        // Fetch category details
        String categoryId = thought.getCategoryId();
        ThoughtCategory category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }

        // If no category or category not found, use Default
        if (category == null) {
            category = categoryRepository.findByThoughtCategory("Default").orElse(null);
        }

        // Fetch platform configurations
        List<ThoughtRequestMessage.PlatformConfiguration> configurations = new ArrayList<>();
        Map<PlatformType, String> legacyPlatformPrompts = new HashMap<>();

        for (ThoughtsToPost.PlatformSelection selection : thought.getPlatformSelections()) {
            // Check if this platform is already completed (for retries)
            boolean alreadyCompleted = thought.getEnrichedContents().stream()
                    .anyMatch(ec -> ec.getPlatform() == selection.getPlatform());

            if (alreadyCompleted && thought.getStatus() == PostStatus.PARTIALLY_COMPLETED) {
                continue; // Skip already completed platforms during retry
            }

            String promptText = null;
            if (selection.getPresetId() != null) {
                promptText = platformPromptRepository.findById(selection.getPresetId())
                        .map(PlatformPrompt::getPromptText)
                        .orElse(null);
            }

            if (promptText == null) {
                // Try to find the first one for this platform
                promptText = platformPromptRepository.findAllByPlatform(selection.getPlatform()).stream()
                        .findFirst()
                        .map(PlatformPrompt::getPromptText)
                        .orElse("");
            }

            configurations.add(ThoughtRequestMessage.PlatformConfiguration.builder()
                    .platform(selection.getPlatform())
                    .prompt(promptText)
                    .additionalContext(selection.getAdditionalContext())
                    .build());

            legacyPlatformPrompts.put(selection.getPlatform(), promptText);
        }

        if (configurations.isEmpty() && thought.getStatus() == PostStatus.PARTIALLY_COMPLETED) {
            log.info("All platforms already enriched for thought {}", thought.getId());
            return;
        }

        ThoughtRequestMessage message = ThoughtRequestMessage.builder()
                .requestId(thought.getId())
                .userId(thought.getUserId())
                .originalThought(thought.getOriginalThought())
                .platforms(configurations.stream().map(ThoughtRequestMessage.PlatformConfiguration::getPlatform).collect(Collectors.toList()))
                .additionalInstructions(additionalInstructions)
                .modelRole(category != null ? category.getModelRole() : null)
                .searchDescription(category != null ? category.getSearchDescription() : null)
                .platformPrompts(legacyPlatformPrompts)
                .platformConfigurations(configurations)
                .version(thought.getVersion() != null ? thought.getVersion().intValue() : 1)
                .createdAt(LocalDateTime.now())
                .build();

        kafkaProducer.sendRequest(message);

        // Update status to processing
        thought.setStatus(PostStatus.PROCESSING);
        thoughtsRepository.save(thought);
    }

    /**
     * Create a history entry for audit tracking.
     */
    private void createHistoryEntry(
            ThoughtsToPost thought,
            ThoughtsToPostHistory.ActionType actionType,
            String performedBy) {
        ThoughtsToPostHistory history = ThoughtsToPostHistory.fromThoughtsToPost(
                thought, actionType, performedBy);
        historyRepository.save(history);
        log.debug("Created history entry: action={}, thoughtId={}", actionType, thought.getId());
    }
}
