package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.model.ThoughtCategory;
import com.carlev.thoughtstopost.repository.ThoughtCategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ThoughtCategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ThoughtCategoryRepository categoryRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ThoughtCategory techCategory;

    @BeforeEach
    void setUp() {
        CategoryController categoryController = new CategoryController(categoryRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        techCategory = ThoughtCategory.builder()
                .id("cat1")
                .category("Tech")
                .searchDescription("Latest tech trends")
                .modelRole("Expert in tech content")
                .build();
    }

    @Test
    void getAllCategories_ShouldReturnList() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of(techCategory));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Tech"));
    }

    @Test
    void createCategory_ShouldReturnCreated() throws Exception {
        when(categoryRepository.save(any(ThoughtCategory.class))).thenReturn(techCategory);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(techCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Tech"));
    }

    @Test
    void updateCategory_ShouldReturnUpdated() throws Exception {
        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(techCategory));
        when(categoryRepository.save(any(ThoughtCategory.class))).thenReturn(techCategory);

        mockMvc.perform(put("/api/categories/cat1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(techCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Tech"));
    }

    @Test
    void deleteCategory_ShouldReturnNoContent() throws Exception {
        when(categoryRepository.findById("cat1")).thenReturn(Optional.of(techCategory));

        mockMvc.perform(delete("/api/categories/cat1"))
                .andExpect(status().isNoContent());
    }
}
