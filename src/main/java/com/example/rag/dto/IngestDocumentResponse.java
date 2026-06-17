package com.example.rag.dto;

public record IngestDocumentResponse(
        Long documentId,
        String title,
        int chunkCount
) {
}
