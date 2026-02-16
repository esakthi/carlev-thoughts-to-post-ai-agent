package com.carlev.thoughtstopost.dto;

import com.carlev.thoughtstopost.model.PlatformType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new thought post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateThoughtRequest {

    @NotBlank(message = "Thought content is required")
    private String thought;

    private String categoryId;

    @NotEmpty(message = "At least one platform must be selected")
    private List<PlatformType> platforms;

    private String additionalInstructions;
}
