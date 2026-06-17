package com.example.rag.dto;

import java.util.List;

public record AskResponse(
        Long questionId,
        String question,
        String answer,
        TokenUsageResponse tokenUsage,
        List<RetrievalHitResponse> retrievalResults
) {
}
