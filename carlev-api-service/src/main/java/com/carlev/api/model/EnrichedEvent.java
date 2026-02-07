package com.carlev.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Enriched event model with AI LLM context enrichment
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichedEvent {
    private String eventId;
    private String enrichedDescription;
    private String contextSummary;
    private List<String> keyEntities; // Extracted entities (people, companies, locations, etc.)
    private List<String> keyTopics; // Extracted key topics
    private Map<String, Object> aiInsights; // AI-generated insights (expandable)
    private String sentiment; // POSITIVE, NEGATIVE, NEUTRAL
    private Double sentimentScore; // -1.0 to 1.0
    private LocalDateTime enrichedAt;
    private String llmModel; // Which LLM model was used
    private Map<String, Object> enrichmentMetadata; // Additional enrichment data

    // Constructors
    public EnrichedEvent() {
        this.enrichedAt = LocalDateTime.now();
    }

    public EnrichedEvent(String eventId) {
        this();
        this.eventId = eventId;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEnrichedDescription() {
        return enrichedDescription;
    }

    public void setEnrichedDescription(String enrichedDescription) {
        this.enrichedDescription = enrichedDescription;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(String contextSummary) {
        this.contextSummary = contextSummary;
    }

    public List<String> getKeyEntities() {
        return keyEntities;
    }

    public void setKeyEntities(List<String> keyEntities) {
        this.keyEntities = keyEntities;
    }

    public List<String> getKeyTopics() {
        return keyTopics;
    }

    public void setKeyTopics(List<String> keyTopics) {
        this.keyTopics = keyTopics;
    }

    public Map<String, Object> getAiInsights() {
        return aiInsights;
    }

    public void setAiInsights(Map<String, Object> aiInsights) {
        this.aiInsights = aiInsights;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public Double getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public LocalDateTime getEnrichedAt() {
        return enrichedAt;
    }

    public void setEnrichedAt(LocalDateTime enrichedAt) {
        this.enrichedAt = enrichedAt;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public Map<String, Object> getEnrichmentMetadata() {
        return enrichmentMetadata;
    }

    public void setEnrichmentMetadata(Map<String, Object> enrichmentMetadata) {
        this.enrichmentMetadata = enrichmentMetadata;
    }
}
