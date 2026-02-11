package com.carlev.thoughtstopost.social;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SocialMediaServiceImplTest {

    @Mock
    private LinkedInService linkedInService;

    @Mock
    private ThoughtsToPostRepository thoughtsToPostRepository;

    @InjectMocks
    private SocialMediaServiceImpl socialMediaService;

    private ThoughtsToPost thought;
    private ThoughtsToPost.EnrichedContent linkedInContent;

    @BeforeEach
    void setUp() {
        thought = new ThoughtsToPost();
        thought.setId("test-id");
        thought.setSelectedPlatforms(List.of(PlatformType.LINKEDIN));

        linkedInContent = new ThoughtsToPost.EnrichedContent();
        linkedInContent.setPlatform(PlatformType.LINKEDIN);
        linkedInContent.setStatus(PostStatus.PENDING);

        List<ThoughtsToPost.EnrichedContent> contents = new ArrayList<>();
        contents.add(linkedInContent);
        thought.setEnrichedContents(contents);
    }

    @Test
    void postToSelectedPlatforms_Success() {
        // Arrange
        when(linkedInService.post(thought)).thenReturn("urn:li:share:12345");
        when(thoughtsToPostRepository.save(any(ThoughtsToPost.class))).thenReturn(thought);

        // Act
        Map<PlatformType, String> results = socialMediaService.postToSelectedPlatforms(thought);

        // Assert
        assertEquals(1, results.size());
        assertEquals("urn:li:share:12345", results.get(PlatformType.LINKEDIN));
        assertEquals(PostStatus.POSTED, linkedInContent.getStatus());
        assertEquals("urn:li:share:12345", linkedInContent.getPostId());
        assertNull(linkedInContent.getErrorMessage());

        // Verify multiple saves (one before posting, one after)
        verify(thoughtsToPostRepository, atLeast(2)).save(thought);
        verify(linkedInService).post(thought);
    }

    @Test
    void postToSelectedPlatforms_Failure() {
        // Arrange
        when(linkedInService.post(thought)).thenThrow(new RuntimeException("API Error"));
        when(thoughtsToPostRepository.save(any(ThoughtsToPost.class))).thenReturn(thought);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            socialMediaService.postToSelectedPlatforms(thought);
        });

        assertTrue(exception.getMessage().contains("API Error"));
        assertEquals(PostStatus.FAILED, linkedInContent.getStatus());
        assertEquals("API Error", linkedInContent.getErrorMessage());
        assertEquals(1, linkedInContent.getRetryCount());

        verify(thoughtsToPostRepository, atLeast(2)).save(thought);
    }

    @Test
    void postToSelectedPlatforms_AlreadyPosted() {
        // Arrange
        linkedInContent.setStatus(PostStatus.POSTED);
        linkedInContent.setPostId("existing-id");

        // Act
        Map<PlatformType, String> results = socialMediaService.postToSelectedPlatforms(thought);

        // Assert
        assertEquals(1, results.size());
        assertEquals("existing-id", results.get(PlatformType.LINKEDIN));

        // Should not call LinkedInService or save repository since already posted
        verify(linkedInService, never()).post(any());
        verify(thoughtsToPostRepository, never()).save(any());
    }
}
