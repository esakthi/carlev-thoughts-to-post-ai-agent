package com.carlev.thoughtstopost.repository;

import com.carlev.thoughtstopost.model.ThoughtsToPostHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ThoughtsToPostHistory documents.
 */
@Repository
public interface ThoughtsToPostHistoryRepository extends MongoRepository<ThoughtsToPostHistory, String> {

    /**
     * Find all history entries for a specific thought document.
     */
    List<ThoughtsToPostHistory> findByThoughtsToPostIdOrderByVersionDesc(String thoughtsToPostId);

    /**
     * Find all history entries for a user.
     */
    List<ThoughtsToPostHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find a specific version of a thought.
     */
    ThoughtsToPostHistory findByThoughtsToPostIdAndVersion(String thoughtsToPostId, Long version);
}
