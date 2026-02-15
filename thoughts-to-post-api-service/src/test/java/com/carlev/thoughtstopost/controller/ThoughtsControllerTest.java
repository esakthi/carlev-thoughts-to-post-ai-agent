package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.service.ThoughtsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ThoughtsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ThoughtsService thoughtsService;

    @BeforeEach
    void setUp() {
        ThoughtsController thoughtsController = new ThoughtsController(thoughtsService);
        mockMvc = MockMvcBuilders.standaloneSetup(thoughtsController).build();
    }

    @Test
    void deleteThought_shouldReturnNoContent() throws Exception {
        String id = "123";
        String userId = "user-1";

        mockMvc.perform(delete("/api/thoughts/" + id)
                .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(thoughtsService).deleteThought(id, userId);
    }

    @Test
    void getCategories_shouldReturnList() throws Exception {
        List<String> categories = List.of("Tech", "Politics");
        when(thoughtsService.getCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/thoughts/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Tech"))
                .andExpect(jsonPath("$[1]").value("Politics"));
    }
}
