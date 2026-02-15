package com.carlev.thoughtstopost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for approving a thought with user comments and choices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveThoughtRequest {
    private String textContentComments;
    private String imageContentComments;

    @Builder.Default
    private boolean postText = true;

    @Builder.Default
    private boolean postImage = true;
}
