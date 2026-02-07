package com.carlev.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * DTO for event response from REST API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventResponse {
    private String id;
    private String message;
    private LocalDateTime timestamp;
    private String status;

    // Constructors
    public EventResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public EventResponse(String id, String message, String status) {
        this();
        this.id = id;
        this.message = message;
        this.status = status;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
