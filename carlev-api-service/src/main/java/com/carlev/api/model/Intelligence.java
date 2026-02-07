package com.carlev.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Intelligence model representing generated insights and analysis around a topic
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Intelligence {
    private String intelligenceId;
    private String eventId;
    private String analysisSummary;
    private List<String> implications; // Key implications of the event
    private List<String> relatedTrends; // Related trends or patterns
    private Map<String, Object> predictions; // AI-generated predictions (expandable)
    private Map<String, Object> riskAssessment; // Risk factors and assessment
    private Map<String, Object> opportunities; // Identified opportunities
    private Integer relevanceScore; // 0-100 relevance score
    private Integer impactScore; // 0-100 impact score
    private LocalDateTime generatedAt;
    private String generationModel; // Which AI model generated this intelligence
    private Map<String, Object> intelligenceMetadata; // Additional intelligence data

    // Constructors
    public Intelligence() {
        this.generatedAt = LocalDateTime.now();
    }

    public Intelligence(String eventId) {
        this();
        this.eventId = eventId;
    }

    // Getters and Setters
    public String getIntelligenceId() {
        return intelligenceId;
    }

    public void setIntelligenceId(String intelligenceId) {
        this.intelligenceId = intelligenceId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAnalysisSummary() {
        return analysisSummary;
    }

    public void setAnalysisSummary(String analysisSummary) {
        this.analysisSummary = analysisSummary;
    }

    public List<String> getImplications() {
        return implications;
    }

    public void setImplications(List<String> implications) {
        this.implications = implications;
    }

    public List<String> getRelatedTrends() {
        return relatedTrends;
    }

    public void setRelatedTrends(List<String> relatedTrends) {
        this.relatedTrends = relatedTrends;
    }

    public Map<String, Object> getPredictions() {
        return predictions;
    }

    public void setPredictions(Map<String, Object> predictions) {
        this.predictions = predictions;
    }

    public Map<String, Object> getRiskAssessment() {
        return riskAssessment;
    }

    public void setRiskAssessment(Map<String, Object> riskAssessment) {
        this.riskAssessment = riskAssessment;
    }

    public Map<String, Object> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(Map<String, Object> opportunities) {
        this.opportunities = opportunities;
    }

    public Integer getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Integer relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Integer getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(Integer impactScore) {
        this.impactScore = impactScore;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public void setGenerationModel(String generationModel) {
        this.generationModel = generationModel;
    }

    public Map<String, Object> getIntelligenceMetadata() {
        return intelligenceMetadata;
    }

    public void setIntelligenceMetadata(Map<String, Object> intelligenceMetadata) {
        this.intelligenceMetadata = intelligenceMetadata;
    }
}
