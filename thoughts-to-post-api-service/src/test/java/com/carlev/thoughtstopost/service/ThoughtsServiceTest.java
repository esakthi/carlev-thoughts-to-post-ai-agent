package com.carlev.thoughtstopost.service;

import com.carlev.thoughtstopost.dto.CreateThoughtRequest;
import com.carlev.thoughtstopost.dto.ThoughtResponse;
import com.carlev.thoughtstopost.kafka.ThoughtRequestMessage;
import com.carlev.thoughtstopost.kafka.ThoughtResponseMessage;
import com.carlev.thoughtstopost.kafka.ThoughtsKafkaProducer;
import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtCategory;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.repository.PlatformPromptRepository;
import com.carlev.thoughtstopost.repository.ThoughtCategoryRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostHistoryRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import com.carlev.thoughtstopost.social.SocialMediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ThoughtsServiceTest {

    @Mock
    private ThoughtsToPostRepository thoughtsRepository;
    @Mock
    private ThoughtsToPostHistoryRepository historyRepository;
    @Mock
    private ThoughtsKafkaProducer kafkaProducer;
    @Mock
    private SocialMediaService socialMediaService;
    @Mock
    private ThoughtCategoryRepository categoryRepository;
    @Mock
    private PlatformPromptRepository platformPromptRepository;

    @InjectMocks
    private ThoughtsService thoughtsService;

    private ThoughtCategory techCategory;

    @BeforeEach
    void setUp() {
        techCategory = ThoughtCategory.builder()
                .id("cat-1")
                .thoughtCategory("Technology")
                .searchDescription("Tech search")
                .modelRole("Tech role")
                .build();
    }

    @Test
    void createThought_WithCategory_ShouldSendEnrichmentRequest() {
        // Arrange
        CreateThoughtRequest request = new CreateThoughtRequest();
        request.setThought("AI is cool");
        request.setCategoryId("cat-1");
        request.setPlatforms(List.of(PlatformType.LINKEDIN));

        ThoughtsToPost savedThought = ThoughtsToPost.builder()
                .id("thought-1")
                .userId("user-1")
                .categoryId("cat-1")
                .originalThought("AI is cool")
                .selectedPlatforms(List.of(PlatformType.LINKEDIN))
                .status(PostStatus.PENDING)
                .build();

        when(thoughtsRepository.save(any())).thenReturn(savedThought);
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(techCategory));

        // Act
        ThoughtResponse response = thoughtsService.createThought(request, "user-1");

        // Assert
        assertNotNull(response);
        assertEquals("thought-1", response.getId());

        ArgumentCaptor<ThoughtRequestMessage> msgCaptor = ArgumentCaptor.forClass(ThoughtRequestMessage.class);
        verify(kafkaProducer).sendRequest(msgCaptor.capture());

        ThoughtRequestMessage sentMsg = msgCaptor.getValue();
        assertEquals("Tech role", sentMsg.getModelRole());
        assertEquals("Tech search", sentMsg.getSearchDescription());
        assertEquals("AI is cool", sentMsg.getOriginalThought());
    }

    @Test
    void handleAgentResponse_ShouldUpdateImageWithDataUri() {
        // Arrange
        ThoughtsToPost thought = ThoughtsToPost.builder()
                .id("thought-1")
                .status(PostStatus.PROCESSING)
                .build();

        when(thoughtsRepository.findById("thought-1")).thenReturn(Optional.of(thought));
        when(thoughtsRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ThoughtResponseMessage message = new ThoughtResponseMessage();
        message.setRequestId("thought-1");
        message.setStatus("completed");

        ThoughtResponseMessage.GeneratedImageMessage image = new ThoughtResponseMessage.GeneratedImageMessage();
        image.setImageBase64("abc");
        image.setImageFormat("png");
        message.setGeneratedImage(image);

        // Act
        thoughtsService.handleAgentResponse(message);

        // Assert
        assertEquals(PostStatus.ENRICHED, thought.getStatus());
        assertEquals("abc", thought.getGeneratedImageBase64());
        assertEquals("data:image/png;base64,abc", thought.getGeneratedImageUrl());
    }
}
