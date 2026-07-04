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

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 rag_questions.id */
    private Long questionId;

    /** 检索结果排序，从 1 开始 */
    private Integer rankNo;

    /** 向量相似度分数，由向量库返回 */
    private Double score;

    /** 来源文档 ID，对应 knowledge_documents.id */
    private Long sourceDocumentId;

    /** 来源文档标题，冗余保存便于日志查询 */
    private String sourceTitle;

    /** 来源文档切块编号 */
    private Integer chunkIndex;

    /** 命中的切块文本 */
    private String content;
}
