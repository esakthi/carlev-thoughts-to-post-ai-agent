package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.dto.SearchCriteriaRequest;
import com.carlev.thoughtstopost.dto.SearchExecuteRequest;
import com.carlev.thoughtstopost.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Controller for internet search operations.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/generate-criteria")
    public CompletableFuture<ResponseEntity<String>> generateCriteria(@RequestBody SearchCriteriaRequest request) {
        log.info("Generating search criteria for category: {}", request.getCategory());
        return searchService.generateSearchString(request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<String>> executeSearch(@RequestBody SearchExecuteRequest request) {
        log.info("Executing internet search for: {}", request.getSearchString());
        return searchService.executeSearch(request.getSearchString())
                .thenApply(ResponseEntity::ok);
    }
}
