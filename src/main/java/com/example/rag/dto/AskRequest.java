package com.example.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 问答请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskRequest {

    /** 用户原始问题 */
    @NotBlank
    private String question;

    /** 检索返回数量，不传则使用默认值 */
    private Integer topK;

    /** 相似度阈值，不传则使用默认值 */
    private Double similarityThreshold;
}
