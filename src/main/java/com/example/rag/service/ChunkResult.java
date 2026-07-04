package com.example.rag.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResult {
    private String text;
    private Map<String, Object> metadata;
}
