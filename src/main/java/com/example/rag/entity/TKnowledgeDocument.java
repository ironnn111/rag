package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 知识文档表实体。
 *
 * <p>保存用户准备的原始文档内容，向量库中的切块通过文档 ID 关联回本表。</p>
 */
@Data
@TableName("knowledge_documents")
public class TKnowledgeDocument {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题，用于检索结果展示 */
    private String title;

    /** 原始文档全文，Milvus 只保存切块后的向量文本 */
    private String content;

    /** 当前文档切分出的片段数量 */
    private Integer chunkCount;

    /**
     * Milvus 中向量文档的 ID 列表，JSON 数组格式。
     * 删除文档时需要同步删除 Milvus 中的向量。
     */
    private String milvusIds;

    /** 文档入库时间 */
    private Instant createdAt;
}
