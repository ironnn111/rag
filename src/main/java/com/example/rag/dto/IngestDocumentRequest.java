package com.example.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档入库请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestDocumentRequest {

    /** 文档标题 */
    @NotBlank
    private String title;

    /** 文档全文内容 */
    @NotBlank
    private String content;
}
