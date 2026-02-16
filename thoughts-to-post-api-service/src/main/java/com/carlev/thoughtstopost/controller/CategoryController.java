package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.model.ThoughtCategory;
import com.carlev.thoughtstopost.repository.ThoughtCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing thought categories.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CategoryController {

    private final ThoughtCategoryRepository categoryRepository;

    @GetMapping
    public List<ThoughtCategory> getAllCategories() {
        log.info("Getting all thought categories");
        return categoryRepository.findAll();
    }

    @PostMapping
    public ThoughtCategory createCategory(@RequestBody ThoughtCategory category) {
        log.info("Creating new thought category: {}", category.getCategory());
        return categoryRepository.save(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThoughtCategory> updateCategory(@PathVariable String id, @RequestBody ThoughtCategory categoryDetails) {
        log.info("Updating thought category: {}", id);
        return categoryRepository.findById(id)
                .map(category -> {
                    category.setCategory(categoryDetails.getCategory());
                    category.setSearchDescription(categoryDetails.getSearchDescription());
                    category.setModelRole(categoryDetails.getModelRole());
                    return ResponseEntity.ok(categoryRepository.save(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        log.info("Deleting thought category: {}", id);
        return categoryRepository.findById(id)
                .map(category -> {
                    categoryRepository.delete(category);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
