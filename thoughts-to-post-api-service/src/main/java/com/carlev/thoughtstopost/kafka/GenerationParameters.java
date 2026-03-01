package com.carlev.thoughtstopost.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationParameters {
    private String resolution; // e.g., "1024x1024"
    private Integer steps;
    private Float cfgScale;
    private Long seed;
    private String modelType; // e.g., "SDXL", "SD1.5"
    private String sampler;
    private Integer batchSize;
    private Boolean asyncMode;
    private Integer duration; // For video
    private Integer fps; // For video
}
