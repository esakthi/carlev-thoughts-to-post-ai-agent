package com.carlev.thoughtstopost.social;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * LinkedIn API v2 integration service.
 * 
 * TODO: Implement OAuth 2.0 flow for user authentication
 * TODO: Store and refresh access tokens per user
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInService {

    @Value("${app.linkedin.client-id}")
    private String clientId;

    @Value("${app.linkedin.client-secret}")
    private String clientSecret;

    @Value("${app.linkedin.redirect-uri}")
    private String redirectUri;

    @Value("${app.linkedin.access-token}")
    private String configuredAccessToken;

    @Value("${app.linkedin.user-urn}")
    private String configuredUserUrn;

    private final WebClient.Builder webClientBuilder;

    // LinkedIn API base URL
    private static final String LINKEDIN_API_URL = "https://api.linkedin.com/v2";

    /**
     * Check if LinkedIn is configured.
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty() &&
                clientSecret != null && !clientSecret.isEmpty() &&
                configuredAccessToken != null && !configuredAccessToken.isEmpty() &&
                configuredUserUrn != null && !configuredUserUrn.isEmpty();
    }

    /**
     * Post content to LinkedIn.
     * 
     * @param thought The thought with enriched content
     * @return The post URN/ID
     */
    public String post(ThoughtsToPost thought) {
        if (!isConfigured()) {
            throw new RuntimeException(
                    "LinkedIn is not configured. Set LINKEDIN_CLIENT_ID, LINKEDIN_CLIENT_SECRET, LINKEDIN_ACCESS_TOKEN, and LINKEDIN_USER_URN.");
        }

        // Get LinkedIn-specific content
        ThoughtsToPost.EnrichedContent linkedInContent = thought.getEnrichedContents().stream()
                .filter(c -> c.getPlatform() == PlatformType.LINKEDIN)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No LinkedIn content found"));

        // TODO: Get user's LinkedIn access token (requires OAuth implementation)
        String accessToken = getUserAccessToken(thought.getUserId());

        // Create the post using LinkedIn's Share API
        return createShare(accessToken, linkedInContent, thought.getGeneratedImageBase64());
    }

    /**
     * Get OAuth authorization URL for user to authorize the app.
     */
    public String getAuthorizationUrl(String state) {
        return "https://www.linkedin.com/oauth/v2/authorization" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&state=" + state +
                "&scope=openid%20profile%20email%20w_member_social";
    }

    /**
     * Exchange authorization code for access token.
     * 
     * TODO: Implement actual OAuth flow
     */
    public Map<String, String> exchangeCodeForToken(String authorizationCode) {
        WebClient client = webClientBuilder.baseUrl("https://www.linkedin.com").build();

        // This is a placeholder - actual implementation would make the OAuth call
        log.info("Exchanging authorization code for access token");

        // TODO: Make actual POST request to /oauth/v2/accessToken
        Map<String, String> result = new HashMap<>();
        result.put("access_token", "placeholder_token");
        result.put("expires_in", "5184000");

        return result;
    }

    /**
     * Get user's stored access token.
     * 
     * TODO: Retrieve from database/secure storage
     */
    private String getUserAccessToken(String userId) {
        if (configuredAccessToken != null && !configuredAccessToken.isEmpty()) {
            return configuredAccessToken;
        }
        // TODO: Retrieve from database per user (requires OAuth implementation)
        throw new IllegalStateException(
                "No LinkedIn access token found. Please set LINKEDIN_ACCESS_TOKEN environment variable.");
    }

    /**
     * Create a share (post) on LinkedIn.
     */
    private String createShare(String accessToken, ThoughtsToPost.EnrichedContent content, String imageBase64) {
        String personUrn = configuredUserUrn != null && !configuredUserUrn.isEmpty() ? configuredUserUrn : null;
        if (personUrn == null) {
            throw new IllegalStateException(
                    "No LinkedIn User URN found. Please set LINKEDIN_USER_URN environment variable (e.g., urn:li:person:XXXXX).");
        }

        String assetUrn = null;

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                log.info("Image provided, starting upload process...");
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

                // 1. Register Upload
                Map<String, String> uploadInfo = registerUpload(accessToken, personUrn);
                String uploadUrl = uploadInfo.get("uploadUrl");
                assetUrn = uploadInfo.get("asset");

                // 2. Upload Image
                uploadImage(uploadUrl, imageBytes, accessToken);
                log.info("Image uploaded successfully: {}", assetUrn);

            } catch (Exception e) {
                log.error("Failed to upload image, falling back to text-only post: {}", e.getMessage());
                // Fallback to text-only if image upload fails
            }
        }

        // Build the post content
        String postText = content.getBody();
        if (content.getHashtags() != null && !content.getHashtags().isEmpty()) {
            postText += "\n\n" + content.getHashtags().stream()
                    .map(h -> "#" + h)
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
        }

        Map<String, Object> shareRequest = buildShareRequest(postText, assetUrn, personUrn);

        log.info("Creating LinkedIn share for content: {}...", postText.substring(0, Math.min(50, postText.length())));

        WebClient client = webClientBuilder
                .baseUrl(LINKEDIN_API_URL)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();

        return client.post()
                .uri("/ugcPosts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(shareRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("id"))
                .block();
    }

    /**
     * Register an image upload with LinkedIn.
     */
    private Map<String, String> registerUpload(String accessToken, String personUrn) {
        WebClient client = webClientBuilder
                .baseUrl(LINKEDIN_API_URL)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();

        Map<String, Object> registerRequest = new HashMap<>();

        Map<String, Object> registerUploadRequest = new HashMap<>();
        registerUploadRequest.put("recipes",
                java.util.Collections.singletonList("urn:li:digitalmediaRecipe:feedshare-image"));
        registerUploadRequest.put("owner", personUrn);

        Map<String, Object> serviceRelationships = new HashMap<>();
        serviceRelationships.put("relationshipType", "OWNER");
        serviceRelationships.put("identifier", "urn:li:userGeneratedContent");
        registerUploadRequest.put("serviceRelationships", java.util.Collections.singletonList(serviceRelationships));

        registerRequest.put("registerUploadRequest", registerUploadRequest);

        // Make actual POST request to /assets?action=registerUpload
        Map response = client.post()
                .uri("/assets?action=registerUpload")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Failed to register upload: response is null");
        }

        // Parse response to get uploadUrl and asset
        Map<String, Object> value = (Map<String, Object>) response.get("value");
        String asset = (String) value.get("asset");
        Map<String, Object> uploadMechanism = (Map<String, Object>) ((Map<String, Object>) value.get("uploadMechanism"))
                .get(
                        "com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest");
        String uploadUrl = (String) uploadMechanism.get("uploadUrl");

        Map<String, String> result = new HashMap<>();
        result.put("uploadUrl", uploadUrl);
        result.put("asset", asset);
        return result;
    }

    /**
     * Upload the image binary data.
     */
    private void uploadImage(String uploadUrl, byte[] imageBytes, String accessToken) {
        // Create a new client for the upload URL (which might be different domain)
        WebClient client = webClientBuilder.build();

        client.put()
                .uri(uploadUrl)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.IMAGE_PNG) // Or detect type
                .bodyValue(imageBytes)
                .retrieve()
                .toBodilessEntity()
                .block();

        log.info("Image uploaded successfully to {}", uploadUrl);
    }

    /**
     * Build the share request body for LinkedIn API.
     */
    private Map<String, Object> buildShareRequest(String postText, String assetUrn, String personUrn) {
        // LinkedIn UGC Post format
        Map<String, Object> request = new HashMap<>();

        request.put("author", personUrn);
        request.put("lifecycleState", "PUBLISHED");

        Map<String, Object> specificContent = new HashMap<>();
        Map<String, Object> shareContent = new HashMap<>();
        Map<String, Object> commentary = new HashMap<>();
        commentary.put("text", postText);
        shareContent.put("shareCommentary", commentary);

        if (assetUrn != null) {
            shareContent.put("shareMediaCategory", "IMAGE");
            Map<String, Object> media = new HashMap<>();
            media.put("status", "READY");
            media.put("description", new HashMap<String, String>() {
                {
                    put("text", "AI Generated Image");
                }
            });
            media.put("media", assetUrn);
            media.put("title", new HashMap<String, String>() {
                {
                    put("text", "Thought Visualization");
                }
            });
            shareContent.put("media", java.util.Collections.singletonList(media));
        } else {
            shareContent.put("shareMediaCategory", "NONE");
        }

        specificContent.put("com.linkedin.ugc.ShareContent", shareContent);
        request.put("specificContent", specificContent);

        Map<String, Object> visibility = new HashMap<>();
        visibility.put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");
        request.put("visibility", visibility);

        return request;
    }
}
