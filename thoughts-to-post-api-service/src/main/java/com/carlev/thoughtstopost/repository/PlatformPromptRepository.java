package com.carlev.thoughtstopost.repository;

import com.carlev.thoughtstopost.model.PlatformPrompt;
import com.carlev.thoughtstopost.model.PlatformType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformPromptRepository extends MongoRepository<PlatformPrompt, String> {
    Optional<PlatformPrompt> findByPlatform(PlatformType platform);
}
