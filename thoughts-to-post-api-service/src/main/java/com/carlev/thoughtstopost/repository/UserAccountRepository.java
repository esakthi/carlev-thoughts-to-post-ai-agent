package com.carlev.thoughtstopost.repository;

import com.carlev.thoughtstopost.model.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for UserAccount documents.
 */
@Repository
public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
}
