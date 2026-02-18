package com.carlev.thoughtstopost.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity for storing user's social media accounts and tokens.
 */
@Document(collection = "user_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
    @Id
    private String userId; // This is the email address

    private String password;

    @Builder.Default
    private java.util.Set<String> roles = new java.util.HashSet<>(java.util.List.of("ROLE_USER"));

    @Builder.Default
    private Map<PlatformType, SocialToken> tokens = new HashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialToken {
        private String accessToken;
        private String refreshToken;
        private LocalDateTime expiresAt;
        private String scope;
        private String personUrn; // Specific to LinkedIn

        @Builder.Default
        private Map<String, String> additionalData = new HashMap<>();
    }
}
