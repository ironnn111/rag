package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问题摘要响应，用于评估列表页展示可选问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSummaryResponse {

    /** 问题 ID */
    private Long questionId;

    /** 用户原始问题 */
    private String question;

    /** 问答时间 */
    private java.time.Instant createdAt;
}
