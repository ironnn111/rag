package com.example.rag.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private int chunkSize;
    private int chunkOverlap;
    private int defaultTopK;
    private double defaultSimilarityThreshold;
}
