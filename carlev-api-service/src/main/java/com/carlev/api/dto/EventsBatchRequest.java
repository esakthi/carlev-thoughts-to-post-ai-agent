package com.carlev.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO for batch event requests (matching events.json structure)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventsBatchRequest {
    private List<EventRequest> events;
    private BatchMetadata metadata;

    // Constructors
    public EventsBatchRequest() {
    }

    // Getters and Setters
    public List<EventRequest> getEvents() {
        return events;
    }

    public void setEvents(List<EventRequest> events) {
        this.events = events;
    }

    public BatchMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BatchMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Inner class for batch metadata
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BatchMetadata {
        private String collectionDate;
        private String source;
        private Integer totalEvents;
        private String version;

        // Getters and Setters
        public String getCollectionDate() {
            return collectionDate;
        }

        public void setCollectionDate(String collectionDate) {
            this.collectionDate = collectionDate;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Integer getTotalEvents() {
            return totalEvents;
        }

        public void setTotalEvents(Integer totalEvents) {
            this.totalEvents = totalEvents;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
