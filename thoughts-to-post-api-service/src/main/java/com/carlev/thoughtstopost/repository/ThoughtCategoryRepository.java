package com.carlev.thoughtstopost.repository;

import com.carlev.thoughtstopost.model.ThoughtCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for thought categories.
 */
@Repository
public interface ThoughtCategoryRepository extends MongoRepository<ThoughtCategory, String> {
    Optional<ThoughtCategory> findByCategory(String category);
}
