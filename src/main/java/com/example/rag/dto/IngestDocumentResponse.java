package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档入库响应，返回入库后的文档 ID、标题和切块数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestDocumentResponse {

    /** 文档 ID */
    private Long documentId;

    /** 文档标题 */
    private String title;

    /** 文档切分出的片段数量 */
    private int chunkCount;
}
