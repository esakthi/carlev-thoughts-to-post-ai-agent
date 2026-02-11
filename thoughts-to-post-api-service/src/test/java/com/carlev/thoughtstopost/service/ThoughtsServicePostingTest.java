package com.carlev.thoughtstopost.service;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.model.ThoughtsToPostHistory;
import com.carlev.thoughtstopost.repository.ThoughtsToPostHistoryRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import com.carlev.thoughtstopost.social.SocialMediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ThoughtsServicePostingTest {

    @Mock
    private ThoughtsToPostRepository thoughtsRepository;

    @Mock
    private ThoughtsToPostHistoryRepository historyRepository;

    @Mock
    private SocialMediaService socialMediaService;

    @InjectMocks
    private ThoughtsService thoughtsService;

    private ThoughtsToPost thought;

    @BeforeEach
    void setUp() {
        thought = new ThoughtsToPost();
        thought.setId("test-id");
        thought.setStatus(PostStatus.APPROVED);

        ThoughtsToPost.EnrichedContent content = new ThoughtsToPost.EnrichedContent();
        content.setPlatform(PlatformType.LINKEDIN);
        content.setStatus(PostStatus.PENDING);

        List<ThoughtsToPost.EnrichedContent> contents = new ArrayList<>();
        contents.add(content);
        thought.setEnrichedContents(contents);
    }

    @Test
    void attemptPosting_Success_AllPlatforms() {
        // Arrange
        when(thoughtsRepository.findById("test-id")).thenReturn(Optional.of(thought));
        when(thoughtsRepository.save(any(ThoughtsToPost.class))).thenReturn(thought);

        // Mock that the service updates the content status to POSTED
        doAnswer(invocation -> {
            ThoughtsToPost t = invocation.getArgument(0);
            t.getEnrichedContents().get(0).setStatus(PostStatus.POSTED);
            return null;
        }).when(socialMediaService).postToSelectedPlatforms(any(ThoughtsToPost.class));

        // Act
        thoughtsService.attemptPosting("test-id");

        // Assert
        assertEquals(PostStatus.POSTED, thought.getStatus());
        verify(socialMediaService).postToSelectedPlatforms(any(ThoughtsToPost.class));
        verify(historyRepository).save(any(ThoughtsToPostHistory.class));
    }

    @Test
    void attemptPosting_PartialSuccess() {
        // Arrange
        thought.setSelectedPlatforms(List.of(PlatformType.LINKEDIN, PlatformType.FACEBOOK));

        ThoughtsToPost.EnrichedContent content2 = new ThoughtsToPost.EnrichedContent();
        content2.setPlatform(PlatformType.FACEBOOK);
        content2.setStatus(PostStatus.PENDING);
        thought.getEnrichedContents().add(content2);

        when(thoughtsRepository.findById("test-id")).thenReturn(Optional.of(thought));
        when(thoughtsRepository.save(any(ThoughtsToPost.class))).thenReturn(thought);

        // Mock that only one platform succeeds
        doAnswer(invocation -> {
            ThoughtsToPost t = invocation.getArgument(0);
            t.getEnrichedContents().get(0).setStatus(PostStatus.POSTED);
            t.getEnrichedContents().get(1).setStatus(PostStatus.FAILED);
            return null;
        }).when(socialMediaService).postToSelectedPlatforms(any(ThoughtsToPost.class));

        // Act
        thoughtsService.attemptPosting("test-id");

        // Assert
        assertEquals(PostStatus.FAILED, thought.getStatus()); // Overall failed if any platform failed
        verify(socialMediaService).postToSelectedPlatforms(any(ThoughtsToPost.class));
    }

    @Test
    void attemptPosting_Exception() {
        // Arrange
        when(thoughtsRepository.findById("test-id")).thenReturn(Optional.of(thought));
        when(thoughtsRepository.save(any(ThoughtsToPost.class))).thenReturn(thought);
        doThrow(new RuntimeException("System Error")).when(socialMediaService)
                .postToSelectedPlatforms(any(ThoughtsToPost.class));

        // Act
        thoughtsService.attemptPosting("test-id");

        // Assert
        assertEquals(PostStatus.FAILED, thought.getStatus());
        verify(thoughtsRepository, atLeast(2)).save(thought);
    }
}
