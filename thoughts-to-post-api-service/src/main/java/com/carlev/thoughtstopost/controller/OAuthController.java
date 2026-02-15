package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.social.LinkedInService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Controller for OAuth flows with social media platforms.
 */
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OAuthController {

    private final LinkedInService linkedInService;

    /**
     * Get LinkedIn authorization status for a user.
     */
    @GetMapping("/linkedin/status")
    public ResponseEntity<Map<String, Boolean>> getLinkedInStatus(@RequestHeader("X-User-Id") String userId) {
        boolean authorized = linkedInService.isUserAuthorized(userId);
        return ResponseEntity.ok(Map.of("authorized", authorized));
    }

    /**
     * Initiate LinkedIn OAuth flow.
     * Returns the authorization URL for the user to visit.
     */
    @GetMapping("/linkedin/authorize")
    public ResponseEntity<Map<String, String>> initiateLinkedInAuth(@RequestHeader("X-User-Id") String userId) {
        // Use userId as state to identify the user on callback
        // In production, this should be a secure hash or session-linked value
        String state = userId;
        String authUrl = linkedInService.getAuthorizationUrl(state);

        log.info("Initiating LinkedIn OAuth flow for user: {}", userId);

        return ResponseEntity.ok(Map.of(
                "authorizationUrl", authUrl,
                "state", state));
    }

    /**
     * LinkedIn OAuth callback handler.
     * Receives the authorization code and exchanges it for an access token.
     */
    @GetMapping("/linkedin/callback")
    public void handleLinkedInCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {
        log.info("Received LinkedIn OAuth callback for user (state): {}", state);
        String userId = state;

        try {
            // 1. Exchange code for token
            Map<String, Object> tokenResponse = linkedInService.exchangeCodeForToken(code);
            String accessToken = (String) tokenResponse.get("access_token");

            // 2. Get member info to get the LinkedIn ID/URN
            Map<String, Object> memberInfo = linkedInService.getMemberInfo(accessToken);

            // 3. Save the token and account info
            linkedInService.saveUserAccount(userId, tokenResponse, memberInfo);

            log.info("Successfully completed LinkedIn OAuth flow for user: {}", userId);

            // 4. Redirect back to frontend dashboard
            response.sendRedirect("http://localhost:4200/?auth=linkedin_success");
        } catch (Exception e) {
            log.error("Failed to process LinkedIn OAuth callback: {}", e.getMessage());
            response.sendRedirect("http://localhost:4200/?auth=linkedin_error&message=" + e.getMessage());
        }
    }
}
