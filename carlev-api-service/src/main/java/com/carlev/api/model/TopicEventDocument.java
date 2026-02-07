package com.carlev.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document model combining Event, EnrichedEvent, and Intelligence
 * This is the main document stored in MongoDB
 */
@Document(collection = "topic_events")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicEventDocument {
    @Id
    private String id;
    
    // Base Event Data
    private Event event;
    
    // Enriched Event Data (after AI context enrichment)
    private EnrichedEvent enrichedEvent;
    
    // Generated Intelligence
    private Intelligence intelligence;
    
    // Processing Status
    private ProcessingStatus status;
    private String statusMessage;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
    
    // Expandable metadata for future extensions
    private Map<String, Object> extendedData;
    
    // Version for optimistic locking
    private Long version;

    public enum ProcessingStatus {
        COLLECTED,      // Event collected but not processed
        ENRICHING,      // Currently being enriched with AI
        ENRICHED,       // Enrichment complete
        ANALYZING,      // Generating intelligence
        COMPLETED,      // Full processing complete
        FAILED          // Processing failed
    }

    // Constructors
    public TopicEventDocument() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ProcessingStatus.COLLECTED;
        this.version = 1L;
    }

    public TopicEventDocument(Event event) {
        this();
        this.event = event;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public EnrichedEvent getEnrichedEvent() {
        return enrichedEvent;
    }

    public void setEnrichedEvent(EnrichedEvent enrichedEvent) {
        this.enrichedEvent = enrichedEvent;
    }

    public Intelligence getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(Intelligence intelligence) {
        this.intelligence = intelligence;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Map<String, Object> getExtendedData() {
        return extendedData;
    }

    public void setExtendedData(Map<String, Object> extendedData) {
        this.extendedData = extendedData;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
