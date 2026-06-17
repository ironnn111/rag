package com.example.rag.dto;

import java.time.Instant;

public record DocumentItemResponse(
        Long documentId,
        String title,
        int chunkCount,
        Instant createdAt
) {
}
