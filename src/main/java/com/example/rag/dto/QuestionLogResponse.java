package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 问答日志详情，包含问题、答案、模型信息、token 用量和检索命中明细。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionLogResponse {

    /** 问题 ID */
    private Long questionId;

    /** 用户原始问题 */
    private String question;

    /** 大模型生成的答案 */
    private String answer;

    /** 实际调用的模型名 */
    private String model;

    /** 完整 Prompt 内容 */
    private String prompt;

    /** LLM 调用 token 用量 */
    private TokenUsageResponse tokenUsage;

    /** 提问时间 */
    private Instant createdAt;

    /** 向量检索命中结果列表 */
    private List<RetrievalHitResponse> retrievalResults;
}
