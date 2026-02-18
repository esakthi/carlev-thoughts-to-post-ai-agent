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
import org.springframework.security.core.Authentication;
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
     * @param request        The create request with thought and platforms
     * @param authentication Authentication object
     * @return Created thought response
     */
    @PostMapping
    public ResponseEntity<ThoughtResponse> createThought(
            @Valid @RequestBody CreateThoughtRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Creating thought for user: {}", userId);
        ThoughtResponse response = thoughtsService.createThought(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a thought by ID.
     * 
     * @param id             The thought ID
     * @param authentication Authentication object
     * @return Thought response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ThoughtResponse> getThought(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Getting thought: {} for user: {}", id, userId);
        ThoughtResponse response = thoughtsService.getThought(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all thoughts for a user, optionally filtered by status or platform.
     * 
     * @param status         Optional status to filter by
     * @param notStatus      Optional status to exclude
     * @param platform       Optional platform to filter by
     * @param authentication Authentication object
     * @return List of thought responses
     */
    @GetMapping
    public ResponseEntity<List<ThoughtResponse>> getUserThoughts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) PostStatus notStatus,
            @RequestParam(required = false) com.carlev.thoughtstopost.model.PlatformType platform,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Getting thoughts for user: {} with filter status: {}, notStatus: {}, platform: {}",
                userId, status, notStatus, platform);
        List<ThoughtResponse> responses;
        if (status != null) {
            responses = thoughtsService.getUserThoughtsByStatus(userId, status);
        } else if (notStatus != null) {
            responses = thoughtsService.getUserThoughtsByStatusNot(userId, notStatus);
        } else if (platform != null) {
            responses = thoughtsService.getUserThoughtsByPlatform(userId, platform);
        } else {
            responses = thoughtsService.getUserThoughts(userId);
        }
        return ResponseEntity.ok(responses);
    }

    /**
     * Get history for a thought.
     * 
     * @param id             The thought ID
     * @param authentication Authentication object
     * @return List of history entries
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ThoughtsToPostHistory>> getThoughtHistory(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Getting history for thought: {} for user: {}", id, userId);
        List<ThoughtsToPostHistory> history = thoughtsService.getThoughtHistory(id, userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Approve a thought and post to social media.
     * 
     * @param id             The thought ID
     * @param request        The approval request with comments and choices
     * @param authentication Authentication object
     * @return Updated thought response
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ThoughtResponse> approveAndPost(
            @PathVariable String id,
            @RequestBody ApproveThoughtRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Approving and posting thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.approveAndPost(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a thought.
     * 
     * @param id             The thought ID
     * @param authentication Authentication object
     * @return Updated thought response
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ThoughtResponse> rejectThought(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Rejecting thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.rejectThought(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update enriched content of a thought.
     *
     * @param id             The thought ID
     * @param request        The thought response DTO with updated content
     * @param authentication Authentication object
     * @return Updated thought response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ThoughtResponse> updateThought(
            @PathVariable String id,
            @RequestBody ThoughtResponse request,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Updating thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.updateEnrichedContent(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resubmit a thought for re-enrichment.
     *
     * @param id             The thought ID
     * @param request        Map containing additionalInstructions
     * @param authentication Authentication object
     * @return Updated thought response
     */
    @PostMapping("/{id}/re-enrich")
    public ResponseEntity<ThoughtResponse> reenrichThought(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> request,
            Authentication authentication) {
        String userId = authentication.getName();
        String instructions = request.get("additionalInstructions");
        log.info("Re-enriching thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.reenrichThought(id, instructions, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a thought.
     *
     * @param id             The thought ID
     * @param authentication Authentication object
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThought(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Deleting thought: {} by user: {}", id, userId);
        thoughtsService.deleteThought(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Repost an already posted thought.
     *
     * @param id             The thought ID
     * @param authentication Authentication object
     * @return Reset thought response
     */
    @PostMapping("/{id}/repost")
    public ResponseEntity<ThoughtResponse> repostThought(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("Reposting thought: {} by user: {}", id, userId);
        ThoughtResponse response = thoughtsService.repostThought(id, userId);
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
