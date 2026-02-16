package com.carlev.thoughtstopost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for executing an internet search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchExecuteRequest {
    private String searchString;
}
