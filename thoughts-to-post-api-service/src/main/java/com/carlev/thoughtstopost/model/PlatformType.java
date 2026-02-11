package com.carlev.thoughtstopost.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported social media platforms.
 */
public enum PlatformType {
    LINKEDIN,
    FACEBOOK,
    INSTAGRAM;

    /**
     * Case-insensitive deserialization from JSON.
     * Python sends "linkedin" (lowercase), Java expects "LINKEDIN" (uppercase).
     */
    @JsonCreator
    public static PlatformType fromString(String value) {
        if (value == null) {
            return null;
        }
        // Case-insensitive matching: convert to uppercase and match enum name
        try {
            return PlatformType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown platform type: " + value + ". Supported values: LINKEDIN, FACEBOOK, INSTAGRAM");
        }
    }

    /**
     * Serialize to JSON using the enum name (uppercase).
     */
    @JsonValue
    public String toValue() {
        return this.name();
    }
}
