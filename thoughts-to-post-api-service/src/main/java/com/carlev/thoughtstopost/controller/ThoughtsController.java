package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.dto.ApproveThoughtRequest;
import com.carlev.thoughtstopost.dto.CreateThoughtRequest;
import com.carlev.thoughtstopost.dto.ThoughtResponse;
import com.carlev.thoughtstopost.model.PostStatus;
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
     * Get all thoughts for a user, optionally filtered by status.
     * 
     * @param status    Optional status to filter by
     * @param notStatus Optional status to exclude
     * @param userId    User ID from header
     * @return List of thought responses
     */
    @GetMapping
    public ResponseEntity<List<ThoughtResponse>> getUserThoughts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) PostStatus notStatus,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        log.info("Getting thoughts for user: {} with filter status: {}, notStatus: {}", userId, status, notStatus);
        List<ThoughtResponse> responses;
        if (status != null) {
            responses = thoughtsService.getUserThoughtsByStatus(userId, status);
        } else if (notStatus != null) {
            responses = thoughtsService.getUserThoughtsByStatusNot(userId, notStatus);
        } else {
            responses = thoughtsService.getUserThoughts(userId);
        }
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
     * @param id      The thought ID
     * @param request The approval request with comments and choices
     * @param userId  User ID from header
     * @return Updated thought response
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ThoughtResponse> approveAndPost(
            @PathVariable String id,
            @RequestBody ApproveThoughtRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        log.info("Approving and posting thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.approveAndPost(id, request, userId);
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
     * Update enriched content of a thought.
     *
     * @param id      The thought ID
     * @param request The thought response DTO with updated content
     * @param userId  User ID from header
     * @return Updated thought response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ThoughtResponse> updateThought(
            @PathVariable String id,
            @RequestBody ThoughtResponse request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        log.info("Updating thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.updateEnrichedContent(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resubmit a thought for re-enrichment.
     *
     * @param id      The thought ID
     * @param request Map containing additionalInstructions
     * @param userId  User ID from header
     * @return Updated thought response
     */
    @PostMapping("/{id}/re-enrich")
    public ResponseEntity<ThoughtResponse> reenrichThought(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        String instructions = request.get("additionalInstructions");
        log.info("Re-enriching thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.reenrichThought(id, instructions, userId);
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
