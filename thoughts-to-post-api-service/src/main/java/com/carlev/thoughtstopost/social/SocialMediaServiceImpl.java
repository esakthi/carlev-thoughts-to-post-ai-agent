package com.carlev.thoughtstopost.social;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of social media posting service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialMediaServiceImpl implements SocialMediaService {

    private final LinkedInService linkedInService;
    private final ThoughtsToPostRepository thoughtsRepository;
    // Future: FacebookService, InstagramService

    @Override
    public Map<PlatformType, String> postToSelectedPlatforms(ThoughtsToPost thought) {
        Map<PlatformType, String> results = new HashMap<>();

        for (PlatformType platform : thought.getSelectedPlatforms()) {
            ThoughtsToPost.EnrichedContent content = thought.getEnrichedContents().stream()
                    .filter(c -> c.getPlatform() == platform)
                    .findFirst()
                    .orElse(null);

            if (content == null) {
                log.warn("No enriched content found for platform: {} in thought: {}", platform, thought.getId());
                continue;
            }

            if (content.getStatus() == PostStatus.POSTED) {
                log.info("Already posted to platform: {} for thought: {}", platform, thought.getId());
                results.put(platform, content.getPostId());
                continue;
            }

            try {
                // Update status to POSTING
                content.setStatus(PostStatus.POSTING);
                content.setLastRetryAt(LocalDateTime.now());
                thoughtsRepository.save(thought);

                String postId = postToPlatform(thought, platform);

                // Update status to POSTED
                content.setStatus(PostStatus.POSTED);
                content.setPostId(postId);
                content.setErrorMessage(null);
                results.put(platform, postId);

                log.info("Successfully posted to {}: {}", platform, postId);
            } catch (Exception e) {
                log.error("Failed to post to {}: {}", platform, e.getMessage());

                // Update status to FAILED
                content.setStatus(PostStatus.FAILED);
                content.setRetryCount((content.getRetryCount() != null ? content.getRetryCount() : 0) + 1);
                content.setErrorMessage(e.getMessage());

                // If it reached a very high retry count, we might want to fail the whole thing,
                // but the scheduler will handle the 100 limit.

                throw new RuntimeException("Failed to post to " + platform + ": " + e.getMessage(), e);
            } finally {
                thoughtsRepository.save(thought);
            }
        }

        return results;
    }

    @Override
    public String postToPlatform(ThoughtsToPost thought, PlatformType platform) {
        return switch (platform) {
            case LINKEDIN -> linkedInService.post(thought);
            case FACEBOOK -> throw new UnsupportedOperationException("Facebook posting not yet implemented");
            case INSTAGRAM -> throw new UnsupportedOperationException("Instagram posting not yet implemented");
        };
    }

    @Override
    public boolean isPlatformConfigured(PlatformType platform) {
        return switch (platform) {
            case LINKEDIN -> linkedInService.isConfigured();
            case FACEBOOK, INSTAGRAM -> false; // Not yet implemented
        };
    }
}
