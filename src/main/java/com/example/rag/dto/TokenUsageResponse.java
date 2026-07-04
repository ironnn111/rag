package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 调用 token 用量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageResponse {

    /** 输入 token 数 */
    private Long inputTokens;

    /** 输出 token 数 */
    private Long outputTokens;

    /** 总 token 数 */
    private Long totalTokens;
}
