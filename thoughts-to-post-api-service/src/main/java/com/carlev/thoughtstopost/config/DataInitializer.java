package com.carlev.thoughtstopost.config;

import com.carlev.thoughtstopost.model.PlatformPrompt;
import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtCategory;
import com.carlev.thoughtstopost.model.UserAccount;
import com.carlev.thoughtstopost.repository.PlatformPromptRepository;
import com.carlev.thoughtstopost.repository.ThoughtCategoryRepository;
import com.carlev.thoughtstopost.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ThoughtCategoryRepository categoryRepository;
    private final PlatformPromptRepository platformPromptRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedCategories();
        seedPlatformPrompts();
        seedDefaultUser();
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            log.info("Seeding default thought category...");
            ThoughtCategory defaultCategory = ThoughtCategory.builder()
                    .thoughtCategory("Default")
                    .searchDescription("General category for all types of thoughts.")
                    .modelRole("""
                            You are an AI content enrichment specialist. Your task is to transform
                            raw thoughts and ideas into polished, engaging social media content.

                            Guidelines:
                            1. Expand on the core idea while maintaining the original intent
                            2. Add relevant context, examples, or data points when appropriate
                            3. Use clear, accessible language
                            4. Create content that provides value to readers
                            5. Maintain authenticity - the content should feel genuine, not overly promotional

                            Always respond with ONLY the enriched content text, no additional commentary or formatting markers.""")
                    .build();
            categoryRepository.save(defaultCategory);
        }
    }

    private void seedPlatformPrompts() {
        seedPlatformPrompt(PlatformType.LINKEDIN, """
                You are an expert LinkedIn content creator specializing in professional
                thought leadership posts. Your content should be:
                - Professional yet engaging and personable
                - Between 1200-1500 characters (optimal for LinkedIn engagement)
                - Include a compelling hook in the first line
                - Use line breaks for readability
                - End with a thought-provoking question or call-to-action
                - Include 3-5 relevant hashtags

                Write content that establishes authority while being relatable to professionals.""");

        seedPlatformPrompt(PlatformType.FACEBOOK, """
                You are an expert Facebook content creator. Your content should be:
                - Conversational and engaging
                - Between 100-250 words for optimal engagement
                - Include emotional hooks that encourage sharing
                - Use emojis sparingly but effectively
                - End with a question to encourage comments
                - Include 2-3 relevant hashtags

                Write content that feels personal and encourages community interaction.""");

        seedPlatformPrompt(PlatformType.INSTAGRAM, """
                You are an expert Instagram caption writer. Your content should be:
                - Engaging and visually descriptive
                - Start with a hook before the "more" fold (first 125 characters)
                - Between 150-300 words total
                - Use emojis strategically throughout
                - Include a clear call-to-action
                - End with 20-30 relevant hashtags (separated by line breaks)

                Write content that complements visual media and encourages saves and shares.""");
    }

    private void seedPlatformPrompt(PlatformType platform, String promptText) {
        Optional<PlatformPrompt> existing = platformPromptRepository.findByPlatform(platform);
        if (existing.isEmpty()) {
            log.info("Seeding platform prompt for {}...", platform);
            PlatformPrompt prompt = PlatformPrompt.builder()
                    .platform(platform)
                    .promptText(promptText)
                    .build();
            platformPromptRepository.save(prompt);
        }
    }

    private void seedDefaultUser() {
        String email = "sakthi.nem@gmail.com";
        if (userAccountRepository.findById(email).isEmpty()) {
            log.info("Seeding default user {}...", email);
            UserAccount defaultUser = UserAccount.builder()
                    .userId(email)
                    .password(passwordEncoder.encode("Password$123"))
                    .roles(new java.util.HashSet<>(java.util.List.of("ROLE_USER")))
                    .build();
            userAccountRepository.save(defaultUser);
        } else {
            // Update existing default user if password is not set
            userAccountRepository.findById(email).ifPresent(user -> {
                if (user.getPassword() == null) {
                    log.info("Updating default user {} with password...", email);
                    user.setPassword(passwordEncoder.encode("Password$123"));
                    if (user.getRoles() == null || user.getRoles().isEmpty()) {
                        user.setRoles(new java.util.HashSet<>(java.util.List.of("ROLE_USER")));
                    }
                    userAccountRepository.save(user);
                }
            });
        }
    }
}
