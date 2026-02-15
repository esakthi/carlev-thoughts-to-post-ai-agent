package com.carlev.thoughtstopost.social;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkedInServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private LinkedInService linkedInService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkedInService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(linkedInService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(linkedInService, "configuredUserUrn", "urn:li:person:test-user");
        ReflectionTestUtils.setField(linkedInService, "configuredAccessToken", "test-access-token");

        // Mock repository
        lenient().when(userAccountRepository.findById(anyString())).thenReturn(java.util.Optional.empty());

        // Mock WebClient.Builder
        lenient().when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.build()).thenReturn(webClient);

        // Mock WebClient post/put chain
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(webClient.put()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPostTextOnly() {
        ThoughtsToPost.EnrichedContent content = ThoughtsToPost.EnrichedContent.builder()
                .platform(PlatformType.LINKEDIN)
                .body("Hello LinkedIn! This is a text-only post.")
                .hashtags(List.of("test", "java"))
                .build();

        ThoughtsToPost thought = ThoughtsToPost.builder()
                .userId("user-1")
                .enrichedContents(List.of(content))
                .build();

        // Mock the response
        Mono<Map> responseMono = Mono.just(Map.of("id", "urn:li:share:placeholder"));
        lenient().when(responseSpec.bodyToMono(Map.class)).thenReturn(responseMono);

        String result = linkedInService.post(thought);

        assertNotNull(result);
        assertEquals("urn:li:share:placeholder", result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPostWithImage() throws IOException {
        // Load dummy image from test resources
        byte[] imageBytes;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sample_post_text.JPG")) {
            assertNotNull(is, "Sample image 'sample_post_text.JPG' not found in test resources!");
            imageBytes = is.readAllBytes();
        }
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        ThoughtsToPost.EnrichedContent content = ThoughtsToPost.EnrichedContent.builder()
                .platform(PlatformType.LINKEDIN)
                .body("Check out this amazing image!")
                .hashtags(List.of("image", "ai"))
                .build();

        ThoughtsToPost thought = ThoughtsToPost.builder()
                .userId("user-1")
                .enrichedContents(List.of(content))
                .generatedImageBase64(base64Image)
                .build();

        // Mock registerUpload response
        Mono<Map> registerMono = Mono.just(Map.of(
                "value", Map.of(
                        "uploadMechanism", Map.of(
                                "com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest",
                                Map.of("uploadUrl", "http://upload.url")),
                        "asset", "urn:li:digitalmediaAsset:12345")));

        // Mock final post response
        Mono<Map> postMono = Mono.just(Map.of("id", "urn:li:share:placeholder"));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(registerMono, postMono);

        String result = linkedInService.post(thought);

        assertNotNull(result);
        assertEquals("urn:li:share:placeholder", result);

        verify(webClientBuilder, atLeastOnce()).build();
    }
}
