package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条检索命中结果，包含排序、分数、来源文档和片段内容。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalHitResponse {

    /** 检索结果排序，从 1 开始 */
    private int rank;

    /** 向量相似度分数 */
    private Double score;

    /** 来源文档 ID */
    private Long documentId;

    /** 来源文档标题 */
    private String title;

    /** 来源文档切块编号 */
    private Integer chunkIndex;

    /** 命中的切块文本 */
    private String content;
}
