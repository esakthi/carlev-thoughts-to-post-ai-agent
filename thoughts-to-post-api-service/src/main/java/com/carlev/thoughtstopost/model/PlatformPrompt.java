package com.carlev.thoughtstopost.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "platform_prompts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPrompt {
    @Id
    private String id;
    private PlatformType platform;
    private String promptText;

    @CreatedDate
    private LocalDateTime createdDateTime;

    @LastModifiedDate
    private LocalDateTime updatedDateTime;
}
