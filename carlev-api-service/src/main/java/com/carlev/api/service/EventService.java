package com.carlev.api.service;

import com.carlev.api.dto.EventRequest;
import com.carlev.api.model.Event;
import com.carlev.api.model.TopicEventDocument;
import com.carlev.api.repository.TopicEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.support.SendResult;

/**
 * Service for processing events: saving to MongoDB and sending to Kafka
 */
@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final TopicEventRepository topicEventRepository;
    private final KafkaProducerService kafkaProducerService;

    public EventService(TopicEventRepository topicEventRepository, 
                       KafkaProducerService kafkaProducerService) {
        this.topicEventRepository = topicEventRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Process a single event: save to MongoDB and send to Kafka
     * @param eventRequest Event request from REST API
     * @return Saved TopicEventDocument
     */
    @Transactional
    public TopicEventDocument processEvent(EventRequest eventRequest) {
        logger.info("Processing event: {}", eventRequest.getTitle());

        // Convert EventRequest to Event model
        Event event = convertToEvent(eventRequest);

        // Create TopicEventDocument
        TopicEventDocument document = new TopicEventDocument(event);
        document.setStatus(TopicEventDocument.ProcessingStatus.COLLECTED);
        
        // Generate ID if not provided
        if (document.getId() == null || document.getId().isEmpty()) {
            document.setId(UUID.randomUUID().toString());
        }
        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(document.getId());
        }

        // Save to MongoDB
        TopicEventDocument savedDocument = saveToDatabase(document);
        logger.info("Event saved to MongoDB with ID: {}", savedDocument.getId());

        // Send to Kafka and wait for completion
        try {
            CompletableFuture<SendResult<String, Object>> kafkaFuture = 
                kafkaProducerService.sendEvent(savedDocument.getId(), savedDocument);
            
            // Wait for Kafka send to complete (with timeout)
            SendResult<String, Object> sendResult = kafkaFuture.get(10, TimeUnit.SECONDS);
            
            logger.info("Event sent successfully to Kafka with ID: {} at offset: {}", 
                savedDocument.getId(), sendResult.getRecordMetadata().offset());
            
            // Update status after successful Kafka send
            savedDocument.setStatus(TopicEventDocument.ProcessingStatus.ENRICHING);
            savedDocument.setUpdatedAt(LocalDateTime.now());
            savedDocument = saveToDatabase(savedDocument);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for Kafka send to complete: {}", savedDocument.getId(), e);
            savedDocument.setStatus(TopicEventDocument.ProcessingStatus.FAILED);
            savedDocument.setStatusMessage("Kafka send timeout: " + e.getMessage());
            savedDocument = saveToDatabase(savedDocument);
        } catch (Exception e) {
            logger.error("Failed to send event to Kafka: {}", savedDocument.getId(), e);
            savedDocument.setStatus(TopicEventDocument.ProcessingStatus.FAILED);
            savedDocument.setStatusMessage("Kafka send failed: " + e.getMessage());
            savedDocument = saveToDatabase(savedDocument);
        }

        return savedDocument;
    }

    /**
     * Process multiple events in batch
     * @param eventRequests List of event requests
     * @return Number of successfully processed events
     */
    @Transactional
    public int processBatchEvents(java.util.List<EventRequest> eventRequests) {
        logger.info("Processing batch of {} events", eventRequests.size());
        int successCount = 0;

        for (EventRequest eventRequest : eventRequests) {
            try {
                processEvent(eventRequest);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to process event: {}", eventRequest.getTitle(), e);
            }
        }

        // Flush Kafka producer after batch processing
        kafkaProducerService.flush();
        logger.info("Batch processing completed: {}/{} events processed successfully", 
            successCount, eventRequests.size());

        return successCount;
    }

    /**
     * Convert EventRequest DTO to Event model
     */
    private Event convertToEvent(EventRequest eventRequest) {
        Event event = new Event();
        event.setId(eventRequest.getId());
        event.setTitle(eventRequest.getTitle());
        event.setDescription(eventRequest.getDescription());
        event.setSource(eventRequest.getSource());
        event.setUrl(eventRequest.getUrl());
        event.setCategory(eventRequest.getCategory());
        event.setPublishedAt(eventRequest.getPublishedAt());
        event.setMetadata(eventRequest.getMetadata());
        event.setRawData(eventRequest.getRawData());
        return event;
    }

    /**
     * Save TopicEventDocument to MongoDB
     */
    private TopicEventDocument saveToDatabase(TopicEventDocument document) {
        return topicEventRepository.save(document);
    }
}
