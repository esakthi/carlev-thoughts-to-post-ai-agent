package com.carlev.thoughtstopost.service;

import com.carlev.thoughtstopost.dto.CreateThoughtRequest;
import com.carlev.thoughtstopost.dto.ThoughtResponse;
import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.model.ThoughtsToPostHistory;
import com.carlev.thoughtstopost.repository.AppConfigRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostHistoryRepository;
import com.carlev.thoughtstopost.repository.ThoughtsToPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ThoughtsServiceTest {

    @Mock
    private ThoughtsToPostRepository thoughtsRepository;

    @Mock
    private ThoughtsToPostHistoryRepository historyRepository;

    @Mock
    private AppConfigRepository configRepository;

    @Mock
    private com.carlev.thoughtstopost.kafka.ThoughtsKafkaProducer kafkaProducer;

    @Mock
    private com.carlev.thoughtstopost.social.SocialMediaService socialMediaService;

    private ThoughtsService thoughtsService;

    @BeforeEach
    void setUp() {
        thoughtsService = new ThoughtsService(thoughtsRepository, historyRepository, configRepository, kafkaProducer, socialMediaService);
    }

    @Test
    void createThought_shouldSetCategory() {
        CreateThoughtRequest request = CreateThoughtRequest.builder()
                .thought("Test thought")
                .platforms(List.of(com.carlev.thoughtstopost.model.PlatformType.LINKEDIN))
                .category("Tech")
                .build();

        ThoughtsToPost savedThought = ThoughtsToPost.builder()
                .id("123")
                .category("Tech")
                .originalThought("Test thought")
                .status(PostStatus.PENDING)
                .build();

        when(thoughtsRepository.save(any(ThoughtsToPost.class))).thenReturn(savedThought);

        ThoughtResponse response = thoughtsService.createThought(request, "user-1");

        assertThat(response.getCategory()).isEqualTo("Tech");
        verify(thoughtsRepository, atLeastOnce()).save(argThat(t -> "Tech".equals(t.getCategory())));
    }

    @Test
    void createThought_shouldDefaultCategoryToOthers() {
        CreateThoughtRequest request = CreateThoughtRequest.builder()
                .thought("Test thought")
                .platforms(List.of(com.carlev.thoughtstopost.model.PlatformType.LINKEDIN))
                .build();

        ThoughtsToPost savedThought = ThoughtsToPost.builder()
                .id("123")
                .category("Others")
                .originalThought("Test thought")
                .status(PostStatus.PENDING)
                .build();

        when(thoughtsRepository.save(any(ThoughtsToPost.class))).thenReturn(savedThought);

        ThoughtResponse response = thoughtsService.createThought(request, "user-1");

        assertThat(response.getCategory()).isEqualTo("Others");
        verify(thoughtsRepository, atLeastOnce()).save(argThat(t -> "Others".equals(t.getCategory())));
    }

    @Test
    void deleteThought_shouldCreateHistoryEntryAndDelete() {
        String id = "123";
        String userId = "user-1";
        ThoughtsToPost thought = ThoughtsToPost.builder()
                .id(id)
                .category("Tech")
                .originalThought("To be deleted")
                .build();

        when(thoughtsRepository.findById(id)).thenReturn(Optional.of(thought));

        thoughtsService.deleteThought(id, userId);

        verify(historyRepository).save(argThat(h ->
            h.getThoughtsToPostId().equals(id) &&
            h.getActionType() == ThoughtsToPostHistory.ActionType.DELETE &&
            h.getPerformedBy().equals(userId)
        ));
        verify(thoughtsRepository).deleteById(id);
    }
}
