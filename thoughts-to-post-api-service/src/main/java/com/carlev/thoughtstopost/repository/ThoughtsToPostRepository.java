package com.carlev.thoughtstopost.repository;

import com.carlev.thoughtstopost.model.PostStatus;
import com.carlev.thoughtstopost.model.ThoughtsToPost;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ThoughtsToPost documents.
 */
@Repository
public interface ThoughtsToPostRepository extends MongoRepository<ThoughtsToPost, String> {

    /**
     * Find all thoughts by user ID.
     */
    List<ThoughtsToPost> findByUserId(String userId);

    /**
     * Find all thoughts by user ID and status.
     */
    List<ThoughtsToPost> findByUserIdAndStatus(String userId, PostStatus status);

    /**
     * Find all thoughts by user ID and status not equal to.
     */
    List<ThoughtsToPost> findByUserIdAndStatusNot(String userId, PostStatus status);

    /**
     * Find all thoughts by status.
     */
    List<ThoughtsToPost> findByStatus(PostStatus status);

    /**
     * Find all pending thoughts ordered by creation date.
     */
    List<ThoughtsToPost> findByStatusOrderByCreatedAtAsc(PostStatus status);

    /**
     * Find all thoughts by user ID and platform.
     */
    List<ThoughtsToPost> findByUserIdAndSelectedPlatformsContains(String userId, com.carlev.thoughtstopost.model.PlatformType platform);
}
