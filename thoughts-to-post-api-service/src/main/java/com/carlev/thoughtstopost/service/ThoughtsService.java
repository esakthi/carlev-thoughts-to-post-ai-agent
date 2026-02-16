package com.carlev.thoughtstopost.service;

import com.carlev.thoughtstopost.dto.CreateThoughtRequest;
import com.carlev.thoughtstopost.dto.ThoughtResponse;
import com.carlev.thoughtstopost.kafka.ThoughtRequestMessage;
import com.carlev.thoughtstopost.kafka.ThoughtResponseMessage;
import com.carlev.thoughtstopost.kafka.ThoughtsKafkaProducer;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.model.ThoughtsToPostHistory;
import com.carlev.thoughtstopost.repository.ThoughtsToPostHistoryRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import com.carlev.thoughtstopost.social.SocialMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
    private final com.carlev.thoughtstopost.repository.AppConfigRepository configRepository;
    private final com.carlev.thoughtstopost.repository.ThoughtCategoryRepository thoughtCategoryRepository;
    private final ThoughtsKafkaProducer kafkaProducer;
    private final SocialMediaService socialMediaService;

    private static final List<String> DEFAULT_CATEGORIES = List.of("Tech", "Politics", "Social", "Others");

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

        // Create the document
        String category = request.getCategory();
        if (category == null || category.trim().isEmpty()) {
            category = "Others";
        }

        ThoughtsToPost thought = ThoughtsToPost.builder()
                .userId(userId)
                .originalThought(request.getThought())
                .selectedPlatforms(request.getPlatforms())
                .category(category)
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
     * Get a thought by ID.
     */
    public ThoughtResponse getThought(String id) {
        ThoughtsToPost thought = thoughtsRepository.findById(id)
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
     * Get history for a thought.
     */
    public List<ThoughtsToPostHistory> getThoughtHistory(String thoughtId) {
        return historyRepository.findByThoughtsToPostIdOrderByVersionDesc(thoughtId);
    }

    /**
     * Approve a thought and post to social media.
     */
    @Transactional
    public ThoughtResponse approveAndPost(String id, com.carlev.thoughtstopost.dto.ApproveThoughtRequest request, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findById(id)
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
        ThoughtsToPost thought = thoughtsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        thought.setStatus(PostStatus.REJECTED);
        thought.setUpdatedBy(userId);
        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.REJECT, userId);

        log.info("Rejected thought: {}", id);
        return ThoughtResponse.fromEntity(thought);
    }

    /**
     * Delete a thought from thoughts_to_post collection but keep it in history.
     */
    @Transactional
    public void deleteThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        // Create history entry for DELETE action
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.DELETE, userId);

        // Delete from repository
        thoughtsRepository.deleteById(id);
        log.info("Deleted thought: {} by user: {}", id, userId);
    }

    /**
     * Get available categories from config or defaults.
     */
    public List<String> getCategories() {
        List<String> categories = thoughtCategoryRepository.findAll().stream()
                .map(com.carlev.thoughtstopost.model.ThoughtCategory::getCategory)
                .collect(Collectors.toList());

        if (categories.isEmpty()) {
            return configRepository.findByKey("categories")
                    .map(com.carlev.thoughtstopost.model.AppConfig::getValue)
                    .orElse(DEFAULT_CATEGORIES);
        }
        return categories;
    }

    /**
     * Update enriched content of a thought manually.
     */
    @Transactional
    public ThoughtResponse updateEnrichedContent(String id, ThoughtResponse request, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findById(id)
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
        ThoughtsToPost thought = thoughtsRepository.findById(id)
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
     * Handle AI agent response from Kafka.
     */
    @Transactional
    public void handleAgentResponse(ThoughtResponseMessage message) {
        log.info("Handling AI agent response for request: {}", message.getRequestId());

        ThoughtsToPost thought = thoughtsRepository.findById(message.getRequestId())
                .orElseThrow(() -> new RuntimeException("Thought not found: " + message.getRequestId()));

        if ("completed".equalsIgnoreCase(message.getStatus())) {
            // Update with enriched content
            if (message.getEnrichedContents() != null) {
                List<ThoughtsToPost.EnrichedContent> enrichedContents = message.getEnrichedContents().stream()
                        .map(ec -> ThoughtsToPost.EnrichedContent.builder()
                                .platform(ec.getPlatform())
                                .title(ec.getTitle())
                                .body(ec.getBody())
                                .hashtags(ec.getHashtags())
                                .callToAction(ec.getCallToAction())
                                .characterCount(ec.getCharacterCount())
                                .build())
                        .collect(Collectors.toList());
                thought.setEnrichedContents(enrichedContents);
            }

            // Update with generated image
            if (message.getGeneratedImage() != null) {
                thought.setGeneratedImageBase64(message.getGeneratedImage().getImageBase64());
                // TODO: Upload to cloud storage and set URL
                thought.setGeneratedImageUrl("data:image/" + message.getGeneratedImage().getImageFormat()
                        + ";base64," + message.getGeneratedImage().getImageBase64());
            }

            thought.setStatus(PostStatus.ENRICHED);
            log.info("Thought enriched successfully: {}", thought.getId());
        } else {
            thought.setStatus(PostStatus.FAILED);
            thought.setErrorMessage(message.getErrorMessage());
            log.error("AI agent processing failed: {}", message.getErrorMessage());
        }

        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.STATUS_CHANGE, "system");
    }

    /**
     * Send thought to AI agent via Kafka.
     */
    private void sendToAiAgent(ThoughtsToPost thought, String additionalInstructions) {
        String modelRole = thoughtCategoryRepository.findByCategory(thought.getCategory())
                .map(com.carlev.thoughtstopost.model.ThoughtCategory::getModelRole)
                .orElse(null);

        ThoughtRequestMessage message = ThoughtRequestMessage.builder()
                .requestId(thought.getId())
                .userId(thought.getUserId())
                .originalThought(thought.getOriginalThought())
                .modelRole(modelRole)
                .platforms(thought.getSelectedPlatforms())
                .additionalInstructions(additionalInstructions)
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
