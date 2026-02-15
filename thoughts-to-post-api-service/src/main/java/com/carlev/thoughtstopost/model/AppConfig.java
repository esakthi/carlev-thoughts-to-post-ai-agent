package com.carlev.thoughtstopost.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Entity for application configuration settings.
 */
@Document(collection = "app_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {

    @Id
    private String id;

    private String key;

    private List<String> value;
}
