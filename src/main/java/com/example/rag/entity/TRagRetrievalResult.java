package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * RAG 检索结果表实体。
 *
 * <p>保存一次问题命中的向量片段，包含分数、来源文档和片段内容。</p>
 */
@Data
@TableName("rag_retrieval_results")
public class TRagRetrievalResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Integer rankNo;

    private Double score;

    private Long sourceDocumentId;

    private String sourceTitle;

    private Integer chunkIndex;

    private String content;
}
