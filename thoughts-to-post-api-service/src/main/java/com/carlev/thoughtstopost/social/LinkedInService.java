package com.carlev.thoughtstopost.social;

import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import com.carlev.thoughtstopost.model.UserAccount;
import com.carlev.thoughtstopost.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LinkedIn API integration service.
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
    private final UserAccountRepository userAccountRepository;

    // LinkedIn API base URL
    private static final String LINKEDIN_API_URL = "https://api.linkedin.com/v2";

    /**
     * Check if LinkedIn is configured.
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty() &&
                clientSecret != null && !clientSecret.isEmpty();
    }

    /**
     * Check if a specific user has authorized LinkedIn.
     */
    public boolean isUserAuthorized(String userId) {
        return userAccountRepository.findById(userId)
                .map(account -> account.getTokens().containsKey(PlatformType.LINKEDIN))
                .orElse(false) || (configuredAccessToken != null && !configuredAccessToken.isEmpty());
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
                    "LinkedIn is not configured. Set LINKEDIN_CLIENT_ID and LINKEDIN_CLIENT_SECRET.");
        }

        // Get LinkedIn-specific content
        ThoughtsToPost.EnrichedContent linkedInContent = thought.getEnrichedContents().stream()
                .filter(c -> c.getPlatform() == PlatformType.LINKEDIN)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No LinkedIn content found"));

        String accessToken = getUserAccessToken(thought.getUserId());
        String personUrn = getPersonUrn(thought.getUserId());

        // Create the post using LinkedIn's Share API
        return createShare(accessToken, personUrn, linkedInContent, thought.getGeneratedImageBase64());
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
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> exchangeCodeForToken(String authorizationCode) {
        WebClient client = webClientBuilder.baseUrl("https://www.linkedin.com").build();

        log.info("Exchanging authorization code for LinkedIn access token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", authorizationCode);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);

        return client.post()
                .uri("/oauth/v2/accessToken")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * Get member profile information.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMemberInfo(String accessToken) {
        WebClient client = webClientBuilder.baseUrl(LINKEDIN_API_URL).build();

        return client.get()
                .uri("/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * Save or update user's LinkedIn token.
     */
    public void saveUserAccount(String userId, Map<String, Object> tokenResponse, Map<String, Object> memberInfo) {
        String accessToken = (String) tokenResponse.get("access_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        String scope = (String) tokenResponse.get("scope");

        // OpenID Connect 'sub' is used as person ID
        String sub = (String) memberInfo.get("sub");
        String personUrn = "urn:li:person:" + sub;

        UserAccount account = userAccountRepository.findById(userId)
                .orElse(UserAccount.builder().userId(userId).build());

        UserAccount.SocialToken token = UserAccount.SocialToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 5184000))
                .scope(scope)
                .personUrn(personUrn)
                .additionalData(new HashMap<>())
                .build();

        if (memberInfo.containsKey("name")) {
            token.getAdditionalData().put("name", (String) memberInfo.get("name"));
        }
        if (memberInfo.containsKey("email")) {
            token.getAdditionalData().put("email", (String) memberInfo.get("email"));
        }

        account.getTokens().put(PlatformType.LINKEDIN, token);
        userAccountRepository.save(account);

        log.info("Saved LinkedIn token for user: {} with personUrn: {}", userId, personUrn);
    }

    /**
     * Get user's stored access token.
     */
    private String getUserAccessToken(String userId) {
        Optional<UserAccount> account = userAccountRepository.findById(userId);
        if (account.isPresent() && account.get().getTokens().containsKey(PlatformType.LINKEDIN)) {
            return account.get().getTokens().get(PlatformType.LINKEDIN).getAccessToken();
        }

        if (configuredAccessToken != null && !configuredAccessToken.isEmpty()) {
            return configuredAccessToken;
        }

        throw new IllegalStateException(
                "No LinkedIn access token found for user: " + userId + ". Please authorize LinkedIn.");
    }

    /**
     * Get user's LinkedIn person URN.
     */
    private String getPersonUrn(String userId) {
        Optional<UserAccount> account = userAccountRepository.findById(userId);
        if (account.isPresent() && account.get().getTokens().containsKey(PlatformType.LINKEDIN)) {
            return account.get().getTokens().get(PlatformType.LINKEDIN).getPersonUrn();
        }

        if (configuredUserUrn != null && !configuredUserUrn.isEmpty()) {
            return configuredUserUrn;
        }

        throw new IllegalStateException(
                "No LinkedIn User URN found for user: " + userId + ". Please authorize LinkedIn.");
    }

    /**
     * Create a share (post) on LinkedIn.
     */
    private String createShare(String accessToken, String personUrn, ThoughtsToPost.EnrichedContent content, String imageBase64) {
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
