package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.model.PlatformPrompt;
import com.carlev.thoughtstopost.model.PlatformType;
import com.carlev.thoughtstopost.model.ThoughtCategory;
import com.carlev.thoughtstopost.repository.PlatformPromptRepository;
import com.carlev.thoughtstopost.repository.ThoughtCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {

    private final ThoughtCategoryRepository categoryRepository;
    private final PlatformPromptRepository platformPromptRepository;

    // Thought Categories
    @GetMapping("/categories")
    public List<ThoughtCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping("/categories")
    public ThoughtCategory createCategory(@RequestBody ThoughtCategory category) {
        log.info("Creating new category: {}", category.getThoughtCategory());
        return categoryRepository.save(category);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ThoughtCategory> updateCategory(@PathVariable String id, @RequestBody ThoughtCategory categoryDetails) {
        return categoryRepository.findById(id)
                .map(category -> {
                    category.setThoughtCategory(categoryDetails.getThoughtCategory());
                    category.setSearchDescription(categoryDetails.getSearchDescription());
                    category.setModelRole(categoryDetails.getModelRole());
                    return ResponseEntity.ok(categoryRepository.save(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        return categoryRepository.findById(id)
                .map(category -> {
                    categoryRepository.delete(category);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Platform Prompts
    @GetMapping("/platform-prompts")
    public List<PlatformPrompt> getAllPlatformPrompts() {
        return platformPromptRepository.findAll();
    }

    @PostMapping("/platform-prompts")
    public PlatformPrompt createPlatformPrompt(@RequestBody PlatformPrompt prompt) {
        log.info("Creating platform prompt for: {}", prompt.getPlatform());
        return platformPromptRepository.save(prompt);
    }

    @PutMapping("/platform-prompts/{id}")
    public ResponseEntity<PlatformPrompt> updatePlatformPrompt(@PathVariable String id, @RequestBody PlatformPrompt promptDetails) {
        return platformPromptRepository.findById(id)
                .map(prompt -> {
                    prompt.setPlatform(promptDetails.getPlatform());
                    prompt.setPromptText(promptDetails.getPromptText());
                    return ResponseEntity.ok(platformPromptRepository.save(prompt));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
