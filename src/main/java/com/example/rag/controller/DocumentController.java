package com.example.rag.controller;

import com.example.rag.dto.DocumentItemResponse;
import com.example.rag.dto.IngestDocumentRequest;
import com.example.rag.dto.IngestDocumentResponse;
import com.example.rag.service.KnowledgeDocumentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识文档接口。
 *
 * <p>Controller 只负责接收 HTTP 请求和返回响应，文档保存、切块和向量写入由 Service 完成。</p>
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private KnowledgeDocumentService documentService;

    /**
     * 接收一份原始知识文档，并写入 MySQL 与 Milvus。
     */
    @PostMapping
    public IngestDocumentResponse ingest(@Valid @RequestBody IngestDocumentRequest request) {
        return documentService.ingest(request);
    }

    /**
     * 上传 Word 文档（.docx / .doc），自动解析内容并入库。
     */
    @PostMapping("/upload")
    public IngestDocumentResponse upload(@RequestParam("file") MultipartFile file) {
        return documentService.ingest(file);
    }

    /**
     * 列出所有已入库的文档，按创建时间倒序。
     */
    @GetMapping
    public List<DocumentItemResponse> listDocuments() {
        return documentService.listDocuments();
    }

    /**
     * 删除指定文档，同时清理 MySQL 记录和 Milvus 向量。
     */
    @DeleteMapping("/{id}")
    public void deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
    }
}
