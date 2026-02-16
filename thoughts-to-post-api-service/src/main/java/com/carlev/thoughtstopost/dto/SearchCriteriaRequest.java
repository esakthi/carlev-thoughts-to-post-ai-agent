package com.carlev.thoughtstopost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for generating search criteria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteriaRequest {
    private String category;
    private String description;
}
