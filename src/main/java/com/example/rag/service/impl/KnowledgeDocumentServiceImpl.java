package com.example.rag.service.impl;

import com.example.rag.dto.DocumentItemResponse;
import com.example.rag.dto.IngestDocumentRequest;
import com.example.rag.dto.IngestDocumentResponse;
import com.example.rag.entity.TDocumentChunk;
import com.example.rag.entity.TKnowledgeDocument;
import com.example.rag.mapper.TDocumentChunkMapper;
import com.example.rag.mapper.TKnowledgeDocumentMapper;
import com.example.rag.service.ChunkResult;
import com.example.rag.service.DocumentChunker;
import com.example.rag.service.KnowledgeDocumentService;
import com.example.rag.service.PdfDocumentParser;
import com.example.rag.service.WordDocumentParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private PdfDocumentParser pdfDocumentParser;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TDocumentChunkMapper chunkMapper;

    @Override
    @Transactional
    public IngestDocumentResponse ingest(IngestDocumentRequest request) {
        return doIngest(request.getTitle(), request.getContent());
    }

    @Override
    @Transactional
    public IngestDocumentResponse ingest(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String title = originalFilename != null ? stripExtension(originalFilename) : "未命名文档";
        String lowerName = originalFilename != null ? originalFilename.toLowerCase() : "";

        try {
            if (lowerName.endsWith(".pdf")) {
                return doIngestPdf(title, file);
            }
            String content = wordDocumentParser.extractText(file.getInputStream(),
                    originalFilename != null ? originalFilename : "");
            return doIngest(title, content);
        } catch (IOException e) {
            throw new RuntimeException("无法解析文档: " + e.getMessage(), e);
        }
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

        for (int i = 0; i < chunks.size(); i++) {
            TDocumentChunk chunk = new TDocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setMilvusId(vectorDocuments.get(i).getId());
            chunk.setCreatedAt(Instant.now());
            chunkMapper.insert(chunk);
        }

        List<String> milvusIds = vectorDocuments.stream()
                .map(Document::getId)
                .toList();
        document.setMilvusIds(toJson(milvusIds));
        documentMapper.updateById(document);

        return new IngestDocumentResponse(document.getId(), document.getTitle(), chunks.size());
    }

    private IngestDocumentResponse doIngestPdf(String title, MultipartFile file) throws IOException {
        PdfDocumentParser.PdfParseResult parseResult = pdfDocumentParser.parse(file.getInputStream());

        String cleanedText = pdfDocumentParser.mergeBrokenLines(parseResult.getFullText());

        List<ChunkResult> chunkResults = chunker.chunkWithMetadata(
                cleanedText, title, parseResult.getPages());

        TKnowledgeDocument document = new TKnowledgeDocument();
        document.setTitle(title);
        document.setContent(cleanedText);
        document.setChunkCount(chunkResults.size());
        document.setCreatedAt(Instant.now());
        documentMapper.insert(document);

        List<Document> vectorDocuments = new ArrayList<>();
        for (int i = 0; i < chunkResults.size(); i++) {
            ChunkResult cr = chunkResults.get(i);
            Map<String, Object> meta = new HashMap<>(cr.getMetadata());
            meta.put("documentId", document.getId());
            meta.put("title", document.getTitle());
            vectorDocuments.add(new Document(cr.getText(), meta));
        }
        vectorStore.add(vectorDocuments);

        for (int i = 0; i < chunkResults.size(); i++) {
            ChunkResult cr = chunkResults.get(i);
            TDocumentChunk chunk = new TDocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(cr.getText());
            chunk.setMilvusId(vectorDocuments.get(i).getId());
            Object pageObj = cr.getMetadata().get("page");
            if (pageObj instanceof Number) {
                chunk.setPage(((Number) pageObj).intValue());
            }
            Object sectionObj = cr.getMetadata().get("sectionTitle");
            if (sectionObj != null) {
                chunk.setSectionTitle(sectionObj.toString());
            }
            chunk.setCreatedAt(Instant.now());
            chunkMapper.insert(chunk);
        }

        List<String> milvusIds = vectorDocuments.stream()
                .map(Document::getId)
                .toList();
        document.setMilvusIds(toJson(milvusIds));
        documentMapper.updateById(document);

        return new IngestDocumentResponse(document.getId(), document.getTitle(), chunkResults.size());
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        TKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return;
        }

        String milvusIdsJson = document.getMilvusIds();
        if (milvusIdsJson != null && !milvusIdsJson.isEmpty()) {
            List<String> ids = fromJson(milvusIdsJson);
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
            }
        }

        documentMapper.deleteById(documentId);
        chunkMapper.delete(new LambdaQueryWrapper<TDocumentChunk>()
                .eq(TDocumentChunk::getDocumentId, documentId));
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
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
}
