package com.carlev.thoughtstopost.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PromptType {
    TEXT,
    IMAGE,
    VIDEO,
    OTHERS;

    @JsonCreator
    public static PromptType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PromptType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHERS;
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
