package com.example.rag.dto;

import java.time.Instant;
import java.util.List;

public record QuestionLogResponse(
        Long questionId,
        String question,
        String answer,
        String model,
        String prompt,
        TokenUsageResponse tokenUsage,
        Instant createdAt,
        List<RetrievalHitResponse> retrievalResults
) {
}
