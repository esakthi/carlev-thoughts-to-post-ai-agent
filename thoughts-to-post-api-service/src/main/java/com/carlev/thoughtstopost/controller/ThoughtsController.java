package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.dto.CreateThoughtRequest;
import com.carlev.thoughtstopost.dto.ThoughtResponse;
import com.carlev.thoughtstopost.model.ThoughtsToPostHistory;
import com.carlev.thoughtstopost.service.ThoughtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for thought post operations.
 */
@RestController
@RequestMapping("/api/thoughts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // TODO: Configure proper CORS for production
public class ThoughtsController {

    private final ThoughtsService thoughtsService;

    /**
     * Create a new thought post.
     * 
     * @param request The create request with thought and platforms
     * @param userId  User ID from header (TODO: Get from JWT/session)
     * @return Created thought response
     */
    @PostMapping
    public ResponseEntity<ThoughtResponse> createThought(
            @Valid @RequestBody CreateThoughtRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        log.info("Creating thought for user: {}", userId);
        ThoughtResponse response = thoughtsService.createThought(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a thought by ID.
     * 
     * @param id The thought ID
     * @return Thought response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ThoughtResponse> getThought(@PathVariable String id) {
        log.info("Getting thought: {}", id);
        ThoughtResponse response = thoughtsService.getThought(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all thoughts for a user.
     * 
     * @param userId User ID from header
     * @return List of thought responses
     */
    @GetMapping
    public ResponseEntity<List<ThoughtResponse>> getUserThoughts(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        log.info("Getting thoughts for user: {}", userId);
        List<ThoughtResponse> responses = thoughtsService.getUserThoughts(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get history for a thought.
     * 
     * @param id The thought ID
     * @return List of history entries
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ThoughtsToPostHistory>> getThoughtHistory(@PathVariable String id) {
        log.info("Getting history for thought: {}", id);
        List<ThoughtsToPostHistory> history = thoughtsService.getThoughtHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Approve a thought and post to social media.
     * 
     * @param id     The thought ID
     * @param userId User ID from header
     * @return Updated thought response
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ThoughtResponse> approveAndPost(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        log.info("Approving and posting thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.approveAndPost(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a thought.
     * 
     * @param id     The thought ID
     * @param userId User ID from header
     * @return Updated thought response
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ThoughtResponse> rejectThought(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        log.info("Rejecting thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.rejectThought(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
