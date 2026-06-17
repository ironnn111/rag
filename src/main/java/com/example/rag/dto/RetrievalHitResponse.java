package com.example.rag.dto;

public record RetrievalHitResponse(
        int rank,
        Double score,
        Long documentId,
        String title,
        Integer chunkIndex,
        String content
) {
}
