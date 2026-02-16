package com.carlev.thoughtstopost.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entity for thought categories with search description and model role.
 */
@Document(collection = "thought_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtCategory {

    @Id
    private String id;

    private String category;

    private String searchDescription;

    private String modelRole;
}
