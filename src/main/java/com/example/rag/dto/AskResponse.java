package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 问答响应，含答案、token 用量和检索命中明细。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskResponse {

    /** 问题 ID */
    private Long questionId;

    /** 用户原始问题 */
    private String question;

    /** 大模型生成的答案 */
    private String answer;

    /** LLM 调用 token 用量 */
    private TokenUsageResponse tokenUsage;

    /** 向量检索命中结果列表 */
    private List<RetrievalHitResponse> retrievalResults;
}
