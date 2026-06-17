package com.example.rag.service.impl;

import com.example.rag.dto.DocumentItemResponse;
import com.example.rag.dto.IngestDocumentRequest;
import com.example.rag.dto.IngestDocumentResponse;
import com.example.rag.entity.TKnowledgeDocument;
import com.example.rag.mapper.TKnowledgeDocumentMapper;
import com.example.rag.service.DocumentChunker;
import com.example.rag.service.KnowledgeDocumentService;
import com.example.rag.service.WordDocumentParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 知识文档入库服务实现。
 *
 * <p>职责包括保存原始文档、切分文档、构造向量文档，并写入 Milvus。</p>
 */
@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Autowired
    private TKnowledgeDocumentMapper documentMapper;

    @Autowired
    private DocumentChunker chunker;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private WordDocumentParser wordDocumentParser;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public IngestDocumentResponse ingest(IngestDocumentRequest request) {
        return doIngest(request.title(), request.content());
    }

    @Override
    @Transactional
    public IngestDocumentResponse ingest(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String title = originalFilename != null ? stripExtension(originalFilename) : "未命名文档";
        String content;
        try {
            content = wordDocumentParser.extractText(file.getInputStream(),
                    originalFilename != null ? originalFilename : "");
        } catch (IOException e) {
            throw new RuntimeException("无法解析 Word 文档: " + e.getMessage(), e);
        }
        return doIngest(title, content);
    }

    private IngestDocumentResponse doIngest(String title, String content) {
        List<String> chunks = chunker.chunk(content);

        TKnowledgeDocument document = new TKnowledgeDocument();
        document.setTitle(title);
        document.setContent(content);
        document.setChunkCount(chunks.size());
        document.setCreatedAt(Instant.now());
        documentMapper.insert(document);

        List<Document> vectorDocuments = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            vectorDocuments.add(new Document(chunks.get(i), Map.of(
                    "documentId", document.getId(),
                    "title", document.getTitle(),
                    "chunkIndex", i
            )));
        }
        vectorStore.add(vectorDocuments);

        // 收集 Spring AI 自动生成的 Milvus 文档 ID，后续删除时需要用到
        List<String> milvusIds = vectorDocuments.stream()
                .map(Document::getId)
                .toList();
        document.setMilvusIds(toJson(milvusIds));
        documentMapper.updateById(document);

        return new IngestDocumentResponse(document.getId(), document.getTitle(), chunks.size());
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        TKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return;
        }

        // 从 Milvus 中删除该文档的所有向量
        String milvusIdsJson = document.getMilvusIds();
        if (milvusIdsJson != null && !milvusIdsJson.isEmpty()) {
            List<String> ids = fromJson(milvusIdsJson);
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
            }
        }

        // 从 MySQL 中删除文档记录
        documentMapper.deleteById(documentId);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 Milvus ID 失败", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化 Milvus ID 失败", e);
        }
    }

    private String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    @Override
    public List<DocumentItemResponse> listDocuments() {
        List<TKnowledgeDocument> docs = documentMapper.selectList(null);
        return docs.stream()
                .map(d -> new DocumentItemResponse(d.getId(), d.getTitle(), d.getChunkCount(), d.getCreatedAt()))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }
}
