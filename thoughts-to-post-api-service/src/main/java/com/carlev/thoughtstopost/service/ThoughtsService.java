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
    private final ThoughtsKafkaProducer kafkaProducer;
    private final SocialMediaService socialMediaService;

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
        ThoughtsToPost thought = ThoughtsToPost.builder()
                .userId(userId)
                .originalThought(request.getThought())
                .selectedPlatforms(request.getPlatforms())
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
     * Get history for a thought.
     */
    public List<ThoughtsToPostHistory> getThoughtHistory(String thoughtId) {
        return historyRepository.findByThoughtsToPostIdOrderByVersionDesc(thoughtId);
    }

    /**
     * Approve a thought and post to social media.
     */
    @Transactional
    public ThoughtResponse approveAndPost(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() != PostStatus.ENRICHED) {
            throw new RuntimeException("Thought is not ready for approval. Status: " + thought.getStatus());
        }

        // Update status to approved
        thought.setStatus(PostStatus.APPROVED);
        thought.setUpdatedBy(userId);
        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.APPROVE, userId);

        // Post to social media
        try {
            thought.setStatus(PostStatus.POSTING);
            thoughtsRepository.save(thought);

            socialMediaService.postToSelectedPlatforms(thought);

            thought.setStatus(PostStatus.POSTED);
            thought = thoughtsRepository.save(thought);
            createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.POST, userId);

            log.info("Successfully posted thought to social media: {}", id);
        } catch (Exception e) {
            log.error("Failed to post to social media: {}", e.getMessage(), e);
            thought.setStatus(PostStatus.FAILED);
            thought.setErrorMessage("Failed to post: " + e.getMessage());
            thought = thoughtsRepository.save(thought);
        }

        return ThoughtResponse.fromEntity(thought);
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
        ThoughtRequestMessage message = ThoughtRequestMessage.builder()
                .requestId(thought.getId())
                .userId(thought.getUserId())
                .originalThought(thought.getOriginalThought())
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
