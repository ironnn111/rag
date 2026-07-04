package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 文档列表中的单个文档摘要。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentItemResponse {

    /** 文档 ID */
    private Long documentId;

    /** 文档标题 */
    private String title;

    /** 文档切分出的片段数量 */
    private int chunkCount;

    /** 文档入库时间 */
    private Instant createdAt;
}
