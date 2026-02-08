package com.carlev.thoughtstopost.kafka;

import com.carlev.thoughtstopost.model.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message sent to Kafka for the AI agent to process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtRequestMessage {

    private String requestId;
    private String userId;
    private String originalThought;
    private List<PlatformType> platforms;
    private String additionalInstructions;
    private Integer version;
    private LocalDateTime createdAt;
}
