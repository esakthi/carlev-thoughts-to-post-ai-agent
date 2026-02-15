package com.carlev.thoughtstopost.repository;

import com.carlev.thoughtstopost.model.AppConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppConfigRepository extends MongoRepository<AppConfig, String> {
    Optional<AppConfig> findByKey(String key);
}
