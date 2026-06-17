package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 业务参数。
 *
 * @param chunkSize 文档切块大小
 * @param chunkOverlap 相邻切块重叠字符数
 * @param defaultTopK 默认检索片段数量
 * @param defaultSimilarityThreshold 默认相似度阈值
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        int chunkSize,
        int chunkOverlap,
        int defaultTopK,
        double defaultSimilarityThreshold
) {
}
