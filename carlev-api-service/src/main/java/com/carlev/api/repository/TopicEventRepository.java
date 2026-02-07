package com.carlev.api.repository;

import com.carlev.api.model.TopicCategory;
import com.carlev.api.model.TopicEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB repository for TopicEventDocument
 */
@Repository
public interface TopicEventRepository extends MongoRepository<TopicEventDocument, String> {
    
    // Find by category
    List<TopicEventDocument> findByEventCategory(TopicCategory category);
    
    // Find by processing status
    List<TopicEventDocument> findByStatus(TopicEventDocument.ProcessingStatus status);
    
    // Find by category and status
    List<TopicEventDocument> findByEventCategoryAndStatus(
        TopicCategory category, 
        TopicEventDocument.ProcessingStatus status
    );
    
    // Find events created within a date range
    List<TopicEventDocument> findByCreatedAtBetween(
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    // Find by category within date range
    List<TopicEventDocument> findByEventCategoryAndCreatedAtBetween(
        TopicCategory category,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
}
