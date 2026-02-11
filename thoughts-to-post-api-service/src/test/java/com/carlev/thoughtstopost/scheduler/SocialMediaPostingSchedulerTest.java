package com.carlev.thoughtstopost.scheduler;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import com.carlev.thoughtstopost.service.ThoughtsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SocialMediaPostingSchedulerTest {

    @Mock
    private ThoughtsToPostRepository thoughtsRepository;

    @Mock
    private ThoughtsService thoughtsService;

    @InjectMocks
    private SocialMediaPostingScheduler scheduler;

    private ThoughtsToPost thought;
    private ThoughtsToPost.EnrichedContent content;

    @BeforeEach
    void setUp() {
        thought = new ThoughtsToPost();
        thought.setId("test-id");
        thought.setStatus(PostStatus.FAILED);

        content = new ThoughtsToPost.EnrichedContent();
        content.setPlatform(PlatformType.LINKEDIN);
        content.setStatus(PostStatus.FAILED);
        content.setRetryCount(1);

        List<ThoughtsToPost.EnrichedContent> contents = new ArrayList<>();
        contents.add(content);
        thought.setEnrichedContents(contents);
    }

    @Test
    void retryPendingPosts_CallsAttemptPosting() {
        // Arrange
        when(thoughtsRepository.findByStatus(PostStatus.APPROVED)).thenReturn(new ArrayList<>());
        when(thoughtsRepository.findByStatus(PostStatus.FAILED))
                .thenReturn(new ArrayList<>(Collections.singletonList(thought)));
        when(thoughtsRepository.findByStatus(PostStatus.POSTING)).thenReturn(new ArrayList<>());

        // Act
        scheduler.retryPendingPosts();

        // Assert
        verify(thoughtsService).attemptPosting("test-id");
    }

    @Test
    void retryPendingPosts_RespectsRetryLimit() {
        // Arrange
        content.setRetryCount(100);
        when(thoughtsRepository.findByStatus(PostStatus.APPROVED)).thenReturn(new ArrayList<>());
        when(thoughtsRepository.findByStatus(PostStatus.FAILED))
                .thenReturn(new ArrayList<>(Collections.singletonList(thought)));
        when(thoughtsRepository.findByStatus(PostStatus.POSTING)).thenReturn(new ArrayList<>());

        // Act
        scheduler.retryPendingPosts();

        // Assert
        verify(thoughtsService, never()).attemptPosting(any());
        verify(thoughtsRepository).save(thought);
        assertEquals("Reached maximum retry limit (100) for one or more platforms.", thought.getErrorMessage());
    }

    @Test
    void retryPendingPosts_MixedStatuses() {
        // Arrange
        ThoughtsToPost approvedThought = new ThoughtsToPost();
        approvedThought.setId("approved-id");
        approvedThought.setStatus(PostStatus.APPROVED);
        approvedThought.setEnrichedContents(Collections.singletonList(content));

        when(thoughtsRepository.findByStatus(PostStatus.APPROVED))
                .thenReturn(new ArrayList<>(Collections.singletonList(approvedThought)));
        when(thoughtsRepository.findByStatus(PostStatus.FAILED))
                .thenReturn(new ArrayList<>(Collections.singletonList(thought)));
        when(thoughtsRepository.findByStatus(PostStatus.POSTING)).thenReturn(new ArrayList<>());

        // Act
        scheduler.retryPendingPosts();

        // Assert
        verify(thoughtsService).attemptPosting("approved-id");
        verify(thoughtsService).attemptPosting("test-id");
    }
}
