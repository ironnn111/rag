package com.example.rag.dto;

public record TokenUsageResponse(
        Long inputTokens,
        Long outputTokens,
        Long totalTokens
) {
}
