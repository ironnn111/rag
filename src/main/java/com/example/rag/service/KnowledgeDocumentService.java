package com.example.rag.service;

import com.example.rag.dto.DocumentItemResponse;
import com.example.rag.dto.IngestDocumentRequest;
import com.example.rag.dto.IngestDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识文档领域服务接口。
 *
 * <p>定义文档进入知识库的业务能力，具体实现放在 service.impl 包下。</p>
 */
public interface KnowledgeDocumentService {

    /**
     * 保存原始文档、执行切块，并将切块内容写入向量库。
     */
    IngestDocumentResponse ingest(IngestDocumentRequest request);

    /**
     * 上传 Word 文件并入库。
     *
     * <p>自动解析文稿内容，以文件名（去除扩展名）作为文档标题。</p>
     */
    IngestDocumentResponse ingest(MultipartFile file);

    /**
     * 列出所有已入库的文档。
     */
    List<DocumentItemResponse> listDocuments();

    /**
     * 删除文档，同时清理 MySQL 记录和 Milvus 向量。
     *
     * @param documentId 文档主键 ID
     */
    void deleteDocument(Long documentId);
}
