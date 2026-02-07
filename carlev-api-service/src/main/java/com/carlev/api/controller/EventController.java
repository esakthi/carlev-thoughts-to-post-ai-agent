package com.carlev.api.controller;

import com.carlev.api.dto.EventRequest;
import com.carlev.api.dto.EventResponse;
import com.carlev.api.dto.EventsBatchRequest;
import com.carlev.api.model.TopicEventDocument;
import com.carlev.api.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for event management
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * POST /api/v1/events
     * Create a single event
     */
    @PostMapping(path = "/new/event", consumes = "application/json", produces = "application/json")
    public ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest eventRequest) {
        logger.info("Received event creation request: {}", eventRequest.getTitle());
        
        try {
            TopicEventDocument document = eventService.processEvent(eventRequest);
            
            EventResponse response = new EventResponse(
                document.getId(),
                "Event processed successfully and sent to Kafka",
                document.getStatus().toString()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error processing event", e);
            EventResponse response = new EventResponse(
                null,
                "Failed to process event: " + e.getMessage(),
                "FAILED"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * POST /api/v1/events/batch
     * Create multiple events from batch request (matching events.json structure)
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> createBatchEvents(@RequestBody EventsBatchRequest batchRequest) {
        logger.info("Received batch event creation request with {} events", 
            batchRequest.getEvents() != null ? batchRequest.getEvents().size() : 0);
        
        try {
            int successCount = eventService.processBatchEvents(batchRequest.getEvents());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Batch processing completed");
            response.put("totalEvents", batchRequest.getEvents().size());
            response.put("successCount", successCount);
            response.put("failedCount", batchRequest.getEvents().size() - successCount);
            response.put("status", "SUCCESS");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error processing batch events", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to process batch events: " + e.getMessage());
            response.put("status", "FAILED");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET /api/v1/events/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Event Service");
        return ResponseEntity.ok(response);
    }
}
