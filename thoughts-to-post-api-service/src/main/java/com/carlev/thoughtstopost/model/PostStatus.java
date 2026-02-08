package com.carlev.thoughtstopost.model;

/**
 * Status of a thought post throughout its lifecycle.
 */
public enum PostStatus {
    PENDING, // Initial state, awaiting processing
    PROCESSING, // Being processed by AI agent
    ENRICHED, // Content enriched, awaiting approval
    APPROVED, // User approved, ready to post
    POSTING, // Currently being posted to social media
    POSTED, // Successfully posted
    FAILED, // Processing or posting failed
    REJECTED // User rejected the enriched content
}
