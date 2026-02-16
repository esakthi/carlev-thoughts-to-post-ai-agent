package com.carlev.thoughtstopost.kafka;

import com.carlev.thoughtstopost.model.PlatformType;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String modelRole;
    private String searchDescription;
    private java.util.Map<PlatformType, String> platformPrompts;
    private Integer version;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;
}
