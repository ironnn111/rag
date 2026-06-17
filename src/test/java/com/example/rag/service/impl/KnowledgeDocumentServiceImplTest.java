package com.example.rag.service.impl;

import com.example.rag.dto.IngestDocumentRequest;
import com.example.rag.dto.IngestDocumentResponse;
import com.example.rag.entity.TKnowledgeDocument;
import com.example.rag.mapper.TKnowledgeDocumentMapper;
import com.example.rag.service.DocumentChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeDocumentServiceImpl 单元测试。
 *
 * <p>验证文档入库流程：保存原始文档、切块、写入向量库。</p>
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceImplTest {

    @Mock
    private TKnowledgeDocumentMapper documentMapper;

    @Mock
    private DocumentChunker chunker;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private KnowledgeDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus insert 后自动回填 ID 的行为
        doAnswer(invocation -> {
            TKnowledgeDocument entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        }).when(documentMapper).insert(any(TKnowledgeDocument.class));
    }

    @Test
    void shouldIngestDocumentWithMultipleChunks() {
        String title = "RAG 技术选型说明";
        String content = "这是一份关于 RAG 系统中 MySQL 与 Milvus 分工设计的说明文档。";

        when(chunker.chunk(content)).thenReturn(List.of(
                "这是一份关于 RAG 系统中 MySQL",
                "与 Milvus 分工设计的说明文档。"
        ));

        IngestDocumentResponse response = service.ingest(new IngestDocumentRequest(title, content));

        // 验证返回值
        assertThat(response.documentId()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo(title);
        assertThat(response.chunkCount()).isEqualTo(2);

        // 验证 MySQL insert 被调用且数据正确
        ArgumentCaptor<TKnowledgeDocument> docCaptor = ArgumentCaptor.forClass(TKnowledgeDocument.class);
        verify(documentMapper).insert(docCaptor.capture());
        TKnowledgeDocument saved = docCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo(title);
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getChunkCount()).isEqualTo(2);
        assertThat(saved.getCreatedAt()).isNotNull();

        // 验证 VectorStore 被调用且携带正确元数据
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> vectorCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(vectorCaptor.capture());
        List<Document> vectorDocs = vectorCaptor.getValue();
        assertThat(vectorDocs).hasSize(2);

        Document firstDoc = vectorDocs.get(0);
        assertThat(firstDoc.getText()).isEqualTo("这是一份关于 RAG 系统中 MySQL");
        Map<String, Object> metadata = firstDoc.getMetadata();
        assertThat(metadata).containsEntry("documentId", 100L);
        assertThat(metadata).containsEntry("title", title);
        assertThat(metadata).containsEntry("chunkIndex", 0);

        Document secondDoc = vectorDocs.get(1);
        Map<String, Object> metadata2 = secondDoc.getMetadata();
        assertThat(metadata2).containsEntry("chunkIndex", 1);
    }

    @Test
    void shouldIngestDocumentWithSingleChunk() {
        String title = "短文档";
        String content = "只有一段内容。";

        when(chunker.chunk(content)).thenReturn(List.of(content));

        IngestDocumentResponse response = service.ingest(new IngestDocumentRequest(title, content));

        assertThat(response.chunkCount()).isEqualTo(1);
        assertThat(response.documentId()).isEqualTo(100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> vectorCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(vectorCaptor.capture());
        assertThat(vectorCaptor.getValue()).hasSize(1);
    }

    @Test
    void shouldHandleEmptyChunks() {
        String title = "空文档";
        String content = "";

        when(chunker.chunk(content)).thenReturn(List.of());

        IngestDocumentResponse response = service.ingest(new IngestDocumentRequest(title, content));

        assertThat(response.chunkCount()).isEqualTo(0);
        assertThat(response.documentId()).isEqualTo(100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> vectorCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(vectorCaptor.capture());
        // 空切块列表也应正常处理
        assertThat(vectorCaptor.getValue()).isEmpty();
    }
}
