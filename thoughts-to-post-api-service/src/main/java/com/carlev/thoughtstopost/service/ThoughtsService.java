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
                        .imagePresetId(config.getImagePresetId())
                        .videoPresetId(config.getVideoPresetId())
                        .additionalContext(config.getAdditionalContext())
                        .imageParams(config.getImageParams())
                        .videoParams(config.getVideoParams())
                        .build());
                selectedPlatforms.add(config.getPlatform());
            }
        } else if (request.getPlatforms() != null) {
            for (PlatformType platform : request.getPlatforms()) {
                selections.add(ThoughtsToPost.PlatformSelection.builder()
                        .platform(platform)
                        .build());
                selectedPlatforms.add(platform);
            }
        }

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

        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.CREATE, userId);

        sendToAiAgent(thought, request.getAdditionalInstructions(), null, null);

        log.info("Created thought with ID: {}", thought.getId());
        return ThoughtResponse.fromEntity(thought);
    }

    public ThoughtResponse getThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));
        return ThoughtResponse.fromEntity(thought);
    }

    public List<ThoughtResponse> getUserThoughts(String userId) {
        return thoughtsRepository.findByUserId(userId).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ThoughtResponse> getUserThoughtsByPlatform(String userId, PlatformType platform) {
        return thoughtsRepository.findByUserIdAndSelectedPlatformsContains(userId, platform).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ThoughtResponse> getUserThoughtsByStatus(String userId, PostStatus status) {
        return thoughtsRepository.findByUserIdAndStatus(userId, status).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ThoughtResponse> getUserThoughtsByStatusNot(String userId, PostStatus status) {
        return thoughtsRepository.findByUserIdAndStatusNot(userId, status).stream()
                .map(ThoughtResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ThoughtsToPostHistory> getThoughtHistory(String thoughtId, String userId) {
        thoughtsRepository.findByIdAndUserId(thoughtId, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + thoughtId));
        return historyRepository.findByThoughtsToPostIdOrderByVersionDesc(thoughtId);
    }

    @Transactional
    public ThoughtResponse approveAndPost(String id, com.carlev.thoughtstopost.dto.ApproveThoughtRequest request, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() != PostStatus.ENRICHED && thought.getStatus() != PostStatus.FAILED && thought.getStatus() != PostStatus.PARTIALLY_COMPLETED) {
            throw new RuntimeException("Thought is not ready for approval. Status: " + thought.getStatus());
        }

        thought.setStatus(PostStatus.APPROVED);
        thought.setUpdatedBy(userId);
        thought.setTextContentComments(request.getTextContentComments());
        thought.setImageContentComments(request.getImageContentComments());
        thought.setPostText(request.isPostText());
        thought.setPostImage(request.isPostImage());

        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.APPROVE, userId);

        attemptPosting(id);

        return ThoughtResponse.fromEntity(thoughtsRepository.findById(id).orElse(thought));
    }

    public void attemptPosting(String id) {
        ThoughtsToPost thought = thoughtsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() != PostStatus.APPROVED && thought.getStatus() != PostStatus.POSTING
                && thought.getStatus() != PostStatus.FAILED) {
            log.warn("Thought {} is not in a postable state: {}", id, thought.getStatus());
            return;
        }

        try {
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

            thought = thoughtsRepository.findById(id).orElse(thought);
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
        } catch (Exception e) {
            log.error("Failed post attempt for thought {}: {}", id, e.getMessage());
            thought.setStatus(PostStatus.FAILED);
            thought.setErrorMessage("Post attempt failed: " + e.getMessage());
            thoughtsRepository.save(thought);
        }
    }

    @Transactional
    public ThoughtResponse rejectThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        thought.setStatus(PostStatus.REJECTED);
        thought.setUpdatedBy(userId);
        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.REJECT, userId);
        return ThoughtResponse.fromEntity(thought);
    }

    @Transactional
    public ThoughtResponse updateEnrichedContent(String id, ThoughtResponse request, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() == PostStatus.POSTED) {
            throw new RuntimeException("Cannot edit content after it has been posted.");
        }

        if (request.getEnrichedContents() != null) {
            List<ThoughtsToPost.EnrichedContent> updatedContents = request.getEnrichedContents().stream()
                    .map(dto -> {
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

                        // Update selected status of images
                        if (dto.getImages() != null) {
                            for (ThoughtResponse.GeneratedImageDto imgDto : dto.getImages()) {
                                existing.getImages().stream()
                                        .filter(img -> img.getId().equals(imgDto.getId()))
                                        .findFirst()
                                        .ifPresent(img -> {
                                            img.setSelected(imgDto.isSelected());
                                            img.setTag(imgDto.getTag());
                                        });
                            }
                        }

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

        sendToAiAgent(savedThought, additionalInstructions, null, null);

        return ThoughtResponse.fromEntity(thoughtsRepository.findById(id).orElse(savedThought));
    }

    @Transactional
    public ThoughtResponse refineImage(String id, String refinementInstructions, PlatformType platform, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        if (thought.getStatus() == PostStatus.POSTED) {
            throw new RuntimeException("Cannot refine image after it has been posted.");
        }

        thought.setUpdatedBy(userId);
        ThoughtsToPost savedThought = thoughtsRepository.save(thought);
        createHistoryEntry(savedThought, ThoughtsToPostHistory.ActionType.UPDATE, userId);

        sendToAiAgent(savedThought, null, refinementInstructions, platform);

        return ThoughtResponse.fromEntity(thoughtsRepository.findById(id).orElse(savedThought));
    }

    @Transactional
    public void deleteThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.DELETE, userId);
        thoughtsRepository.delete(thought);
    }

    @Transactional
    public ThoughtResponse repostThought(String id, String userId) {
        ThoughtsToPost thought = thoughtsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Thought not found: " + id));

        thought.setStatus(PostStatus.PENDING);
        thought.setEnrichedContents(new ArrayList<>());
        thought.setGeneratedImageUrl(null);
        thought.setGeneratedImageBase64(null);
        thought.setErrorMessage(null);
        thought.setUpdatedBy(userId);

        ThoughtsToPost savedThought = thoughtsRepository.save(thought);
        createHistoryEntry(savedThought, ThoughtsToPostHistory.ActionType.UPDATE, userId);

        sendToAiAgent(savedThought, "Reposting this thought.", null, null);

        return ThoughtResponse.fromEntity(thoughtsRepository.findById(id).orElse(savedThought));
    }

    @Transactional
    public void handleAgentResponse(ThoughtResponseMessage message) {
        log.info("Handling AI agent response for request: {} with status: {}", message.getRequestId(), message.getStatus());

        ThoughtsToPost thought = thoughtsRepository.findById(message.getRequestId())
                .orElseThrow(() -> new RuntimeException("Thought not found: " + message.getRequestId()));

        if (message.getEnrichedContents() != null) {
            for (ThoughtResponseMessage.EnrichedContentMessage ecMsg : message.getEnrichedContents()) {
                ThoughtsToPost.EnrichedContent content = thought.getEnrichedContents().stream()
                        .filter(existing -> existing.getPlatform() == ecMsg.getPlatform())
                        .findFirst()
                        .orElse(null);

                if (content == null) {
                    content = ThoughtsToPost.EnrichedContent.builder()
                            .platform(ecMsg.getPlatform())
                            .status(PostStatus.PENDING)
                            .images(new ArrayList<>())
                            .build();
                    thought.getEnrichedContents().add(content);
                }

                if (ecMsg.getBody() != null) {
                    content.setTitle(ecMsg.getTitle());
                    content.setBody(ecMsg.getBody());
                    content.setHashtags(ecMsg.getHashtags());
                    content.setCallToAction(ecMsg.getCallToAction());
                    content.setCharacterCount(ecMsg.getCharacterCount());
                    if (ecMsg.getProgress() != null) {
                        content.setProgress(ecMsg.getProgress());
                    }
                }

                if (ecMsg.getImages() != null) {
                    for (ThoughtResponseMessage.GeneratedImageMessage imgMsg : ecMsg.getImages()) {
                        boolean exists = content.getImages().stream().anyMatch(i -> i.getId().equals(imgMsg.getId()));
                        if (!exists) {
                            String dataUri = "data:image/" + imgMsg.getImageFormat() + ";base64," + imgMsg.getImageBase64();
                            content.getImages().add(ThoughtsToPost.GeneratedImage.builder()
                                    .id(imgMsg.getId())
                                    .base64Data(imgMsg.getImageBase64())
                                    .url(dataUri)
                                    .prompt(imgMsg.getPromptUsed())
                                    .format(imgMsg.getImageFormat())
                                    .width(imgMsg.getWidth())
                                    .height(imgMsg.getHeight())
                                    .tag(imgMsg.getTag())
                                    .createdAt(imgMsg.getCreatedAt() != null ? imgMsg.getCreatedAt() : LocalDateTime.now())
                                    .selected(content.getImages().isEmpty()) // Select first by default
                                    .build());
                        }
                    }
                }
            }
        }

        String incomingStatus = message.getStatus().toLowerCase();
        if ("completed".equals(incomingStatus)) {
            thought.setStatus(PostStatus.ENRICHED);
        } else if ("in_progress".equals(incomingStatus)) {
            thought.setStatus(PostStatus.PROCESSING);
        } else if ("partially_completed".equals(incomingStatus)) {
            thought.setStatus(PostStatus.PARTIALLY_COMPLETED);
            thought.setErrorMessage(message.getErrorMessage());
        } else {
            thought.setStatus(PostStatus.FAILED);
            thought.setErrorMessage(message.getErrorMessage());
        }

        thought = thoughtsRepository.save(thought);
        createHistoryEntry(thought, ThoughtsToPostHistory.ActionType.STATUS_CHANGE, "system");
    }

    private void sendToAiAgent(ThoughtsToPost thought, String additionalInstructions, String imageRefinementInstructions, PlatformType targetPlatform) {
        String categoryId = thought.getCategoryId();
        ThoughtCategory category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }
        if (category == null) {
            category = categoryRepository.findByThoughtCategory("Default").orElse(null);
        }

        List<ThoughtRequestMessage.PlatformConfiguration> configurations = new ArrayList<>();
        Map<PlatformType, String> legacyPlatformPrompts = new HashMap<>();

        for (ThoughtsToPost.PlatformSelection selection : thought.getPlatformSelections()) {
            if (targetPlatform != null && selection.getPlatform() != targetPlatform) {
                continue;
            }

            String promptText = null;
            if (selection.getPresetId() != null) {
                promptText = platformPromptRepository.findById(selection.getPresetId())
                        .map(PlatformPrompt::getPromptText)
                        .orElse(null);
            }

            if (promptText == null) {
                promptText = platformPromptRepository.findAllByPlatform(selection.getPlatform()).stream()
                        .filter(p -> p.getType() == com.carlev.thoughtstopost.model.PromptType.TEXT || p.getType() == null)
                        .findFirst()
                        .map(PlatformPrompt::getPromptText)
                        .orElse("");
            }

            String imagePrompt = null;
            if (selection.getImagePresetId() != null) {
                imagePrompt = platformPromptRepository.findById(selection.getImagePresetId())
                        .map(PlatformPrompt::getPromptText)
                        .orElse(null);
            }

            String videoPrompt = null;
            if (selection.getVideoPresetId() != null) {
                videoPrompt = platformPromptRepository.findById(selection.getVideoPresetId())
                        .map(PlatformPrompt::getPromptText)
                        .orElse(null);
            }

            configurations.add(ThoughtRequestMessage.PlatformConfiguration.builder()
                    .platform(selection.getPlatform())
                    .prompt(promptText)
                    .imagePrompt(imagePrompt)
                    .videoPrompt(videoPrompt)
                    .additionalContext(selection.getAdditionalContext())
                    .imageParams(selection.getImageParams())
                    .videoParams(selection.getVideoParams())
                    .build());

            legacyPlatformPrompts.put(selection.getPlatform(), promptText);
        }

        ThoughtRequestMessage message = ThoughtRequestMessage.builder()
                .requestId(thought.getId())
                .userId(thought.getUserId())
                .originalThought(thought.getOriginalThought())
                .platforms(configurations.stream().map(ThoughtRequestMessage.PlatformConfiguration::getPlatform).collect(Collectors.toList()))
                .additionalInstructions(additionalInstructions)
                .imageRefinementInstructions(imageRefinementInstructions)
                .targetPlatform(targetPlatform)
                .modelRole(category != null ? category.getModelRole() : null)
                .searchDescription(category != null ? category.getSearchDescription() : null)
                .platformPrompts(legacyPlatformPrompts)
                .platformConfigurations(configurations)
                .version(thought.getVersion() != null ? thought.getVersion().intValue() : 1)
                .createdAt(LocalDateTime.now())
                .build();

        kafkaProducer.sendRequest(message);
        thought.setStatus(PostStatus.PROCESSING);
        thoughtsRepository.save(thought);
    }

    private void createHistoryEntry(ThoughtsToPost thought, ThoughtsToPostHistory.ActionType actionType, String performedBy) {
        ThoughtsToPostHistory history = ThoughtsToPostHistory.fromThoughtsToPost(thought, actionType, performedBy);
        historyRepository.save(history);
    }
}
