package com.carlev.thoughtstopost.social;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    // Future: FacebookService, InstagramService

    @Override
    public Map<PlatformType, String> postToSelectedPlatforms(ThoughtsToPost thought) {
        Map<PlatformType, String> results = new HashMap<>();

        for (PlatformType platform : thought.getSelectedPlatforms()) {
            try {
                String postId = postToPlatform(thought, platform);
                results.put(platform, postId);
                log.info("Posted to {}: {}", platform, postId);
            } catch (Exception e) {
                log.error("Failed to post to {}: {}", platform, e.getMessage());
                throw new RuntimeException("Failed to post to " + platform + ": " + e.getMessage(), e);
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
