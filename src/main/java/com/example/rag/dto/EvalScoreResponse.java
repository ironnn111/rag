package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评估分数响应。
 *
 * <p>返回 LLM-as-Judge 对某次问答的 faithfulness 和 relevancy 评估结果。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvalScoreResponse {

    /** 评估记录 ID */
    private Long evalId;

    /** 关联的问题 ID */
    private Long questionId;

    /** 忠实度评分 1-5 */
    private Integer faithfulnessScore;

    /** 忠实度评分理由 */
    private String faithfulnessReason;

    /** 相关性评分 1-5 */
    private Integer relevancyScore;

    /** 相关性评分理由 */
    private String relevancyReason;

    /** 评估时间 */
    private java.time.Instant createdAt;
}
