package com.carlev.thoughtstopost.scheduler;

import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import com.carlev.thoughtstopost.service.ThoughtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler for retrying failed or pending social media posts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialMediaPostingScheduler {

    private final ThoughtsToPostRepository thoughtsRepository;
    private final ThoughtsService thoughtsService;

    /**
     * Poll for posts that need retry every 10 minutes.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    public void retryPendingPosts() {
        log.info("Starting scheduled task to retry pending social media posts...");

        // Find thoughts that are in APPROVED or FAILED status
        List<ThoughtsToPost> candidateThoughts = thoughtsRepository.findByStatus(PostStatus.APPROVED);
        candidateThoughts.addAll(thoughtsRepository.findByStatus(PostStatus.FAILED));
        candidateThoughts.addAll(thoughtsRepository.findByStatus(PostStatus.POSTING)); // In case some got stuck

        for (ThoughtsToPost thought : candidateThoughts) {
            try {
                processThoughtForRetry(thought);
            } catch (Exception e) {
                log.error("Error processing thought {} for retry: {}", thought.getId(), e.getMessage());
            }
        }

        log.info("Finished scheduled task to retry pending social media posts.");
    }

    private void processThoughtForRetry(ThoughtsToPost thought) {
        boolean needsRetry = false;
        boolean permanentFailure = false;

        for (ThoughtsToPost.EnrichedContent ec : thought.getEnrichedContents()) {
            if (ec.getStatus() != PostStatus.POSTED) {
                if (ec.getRetryCount() != null && ec.getRetryCount() >= 100) {
                    log.warn("Platform {} for thought {} has reached max retry limit (100).", ec.getPlatform(),
                            thought.getId());
                    permanentFailure = true;
                } else {
                    needsRetry = true;
                }
            }
        }

        if (permanentFailure && !needsRetry) {
            // If all failing platforms reached 100 retries, mark as permanent failure?
            // Or just leave it as FAILED. The requirement says "mark the post as failed to
            // post".
            thought.setStatus(PostStatus.FAILED);
            thought.setErrorMessage("Reached maximum retry limit (100) for one or more platforms.");
            thoughtsRepository.save(thought);
            return;
        }

        if (needsRetry) {
            log.info("Thought {} needs retry. Attempting posting...", thought.getId());
            thoughtsService.attemptPosting(thought.getId());
        }
    }
}
