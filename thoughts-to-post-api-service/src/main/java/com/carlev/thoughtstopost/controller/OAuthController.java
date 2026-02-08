package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.social.LinkedInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

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
     * Initiate LinkedIn OAuth flow.
     * Returns the authorization URL for the user to visit.
     */
    @GetMapping("/linkedin/authorize")
    public ResponseEntity<Map<String, String>> initiateLinkedInAuth() {
        String state = UUID.randomUUID().toString();
        String authUrl = linkedInService.getAuthorizationUrl(state);

        log.info("Initiating LinkedIn OAuth flow with state: {}", state);

        return ResponseEntity.ok(Map.of(
                "authorizationUrl", authUrl,
                "state", state));
    }

    /**
     * LinkedIn OAuth callback handler.
     * Receives the authorization code and exchanges it for an access token.
     */
    @GetMapping("/linkedin/callback")
    public ResponseEntity<Map<String, Object>> handleLinkedInCallback(
            @RequestParam String code,
            @RequestParam String state) {
        log.info("Received LinkedIn OAuth callback with state: {}", state);

        try {
            Map<String, String> tokenResponse = linkedInService.exchangeCodeForToken(code);

            // TODO: Store the access token securely associated with the user
            log.info("Successfully obtained LinkedIn access token");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "LinkedIn authorization successful",
                    "expiresIn", tokenResponse.getOrDefault("expires_in", "unknown")));
        } catch (Exception e) {
            log.error("Failed to exchange LinkedIn auth code: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}
