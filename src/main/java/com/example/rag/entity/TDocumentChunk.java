package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 文档切块表实体。
 *
 * <p>记录每个切块在文档中的位置、内容和 Milvus 向量关联。</p>
 */
@Data
@TableName("document_chunks")
public class TDocumentChunk {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 knowledge_documents.id */
    private Long documentId;

    /** 切块在文档中的序号，从 0 开始 */
    private Integer chunkIndex;

    /** 切块文本内容 */
    private String content;

    /** Milvus 中对应向量的文档 ID */
    private String milvusId;

    /** PDF 文档所在页码，非 PDF 文档为空 */
    private Integer page;

    /** 切块所属章节标题 */
    private String sectionTitle;

    /** 切块创建时间 */
    private Instant createdAt;
}
