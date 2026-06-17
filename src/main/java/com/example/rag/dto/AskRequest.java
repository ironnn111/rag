package com.example.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(
        @NotBlank String question,
        Integer topK,
        Double similarityThreshold
) {
}
