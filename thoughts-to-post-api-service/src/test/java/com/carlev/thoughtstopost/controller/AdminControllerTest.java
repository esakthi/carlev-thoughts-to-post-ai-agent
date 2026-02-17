package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.model.PlatformPrompt;
import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtCategory;
import com.carlev.thoughtstopost.repository.PlatformPromptRepository;
import com.carlev.thoughtstopost.repository.ThoughtCategoryRepository;
import com.carlev.thoughtstopost.repository.UserAccountRepository;
import com.carlev.thoughtstopost.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ThoughtCategoryRepository categoryRepository;

    @MockBean
    private PlatformPromptRepository platformPromptRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void getAllCategories_ShouldReturnList() throws Exception {
        ThoughtCategory cat = ThoughtCategory.builder().id("1").thoughtCategory("Tech").build();
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(cat));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].thoughtCategory").value("Tech"));
    }

    @Test
    @WithMockUser
    void createCategory_ShouldReturnCreated() throws Exception {
        ThoughtCategory cat = ThoughtCategory.builder().thoughtCategory("Tech").build();
        when(categoryRepository.save(any())).thenReturn(cat);

        mockMvc.perform(post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thoughtCategory").value("Tech"));
    }

    @Test
    @WithMockUser
    void getAllPlatformPrompts_ShouldReturnList() throws Exception {
        PlatformPrompt prompt = PlatformPrompt.builder().id("1").platform(PlatformType.LINKEDIN).promptText("test").build();
        when(platformPromptRepository.findAll()).thenReturn(Arrays.asList(prompt));

        mockMvc.perform(get("/api/admin/platform-prompts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].platform").value("LINKEDIN"));
    }
}
