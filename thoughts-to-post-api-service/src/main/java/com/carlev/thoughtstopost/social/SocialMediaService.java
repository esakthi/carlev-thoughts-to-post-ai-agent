package com.carlev.thoughtstopost.social;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtsToPost;

import java.util.Map;

/**
 * Interface for social media posting services.
 */
public interface SocialMediaService {

    /**
     * Post content to all selected platforms.
     *
     * @param thought The thought with enriched content to post
     * @return Map of platform to post URL/ID
     */
    Map<PlatformType, String> postToSelectedPlatforms(ThoughtsToPost thought);

    /**
     * Post content to a specific platform.
     *
     * @param thought  The thought with enriched content
     * @param platform The target platform
     * @return Post URL or ID
     */
    String postToPlatform(ThoughtsToPost thought, PlatformType platform);

    /**
     * Check if a platform is configured and ready.
     *
     * @param platform The platform to check
     * @return true if ready to post
     */
    boolean isPlatformConfigured(PlatformType platform);
}
