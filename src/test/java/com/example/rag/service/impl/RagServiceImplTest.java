package com.example.rag.service.impl;

import com.example.rag.config.RagProperties;
import com.example.rag.dto.AskRequest;
import com.example.rag.dto.AskResponse;
import com.example.rag.dto.QuestionLogResponse;
import com.example.rag.entity.TLlmCallLog;
import com.example.rag.entity.TRagQuestion;
import com.example.rag.entity.TRagRetrievalResult;
import com.example.rag.mapper.TLlmCallLogMapper;
import com.example.rag.mapper.TRagQuestionMapper;
import com.example.rag.mapper.TRagRetrievalResultMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RagServiceImpl 单元测试。
 *
 * <p>验证 RAG 问答全流程：向量检索、Prompt 组装、模型调用、日志落库及 token 统计。
 * 重点验证每一次调用的 input/output/total token 记录完整性。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RagServiceImplTest {

    @Mock
    private TRagQuestionMapper questionMapper;

    @Mock
    private TRagRetrievalResultMapper retrievalResultMapper;

    @Mock
    private TLlmCallLogMapper callLogMapper;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private RagServiceImpl service;
    private final RagProperties properties = new RagProperties(800, 120, 5, 0.6);

    @BeforeEach
    void setUp() {
        service = new RagServiceImpl();
        ReflectionTestUtils.setField(service, "questionMapper", questionMapper);
        ReflectionTestUtils.setField(service, "retrievalResultMapper", retrievalResultMapper);
        ReflectionTestUtils.setField(service, "callLogMapper", callLogMapper);
        ReflectionTestUtils.setField(service, "vectorStore", vectorStore);
        ReflectionTestUtils.setField(service, "chatClientBuilder", chatClientBuilder);
        ReflectionTestUtils.setField(service, "properties", properties);

        // MyBatis-Plus insert 后回填 ID
        doAnswer(inv -> {
            TRagQuestion q = inv.getArgument(0);
            q.setId(1L);
            return 1;
        }).when(questionMapper).insert(any(TRagQuestion.class));

        doAnswer(inv -> {
            TRagRetrievalResult r = inv.getArgument(0);
            r.setId(100L + r.getRankNo());
            return 1;
        }).when(retrievalResultMapper).insert(any(TRagRetrievalResult.class));

        doAnswer(inv -> {
            TLlmCallLog log = inv.getArgument(0);
            log.setId(200L);
            return 1;
        }).when(callLogMapper).insert(any(TLlmCallLog.class));

        // ChatClient 调用链 mock
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    // ======================== ask() 正常流程测试 ========================

    @Test
    void shouldAskWithDefaultsAndLogTokens() {
        List<Document> retrievalDocs = List.of(
                createDoc("RAG 系统同时使用 MySQL 和 Milvus...", 1L, "技术选型说明", 0, 0.92),
                createDoc("MySQL 负责保存结构化业务数据和调用日志", 1L, "技术选型说明", 1, 0.85),
                createDoc("Milvus 负责保存向量数据并执行相似度检索", 1L, "技术选型说明", 2, 0.78)
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(retrievalDocs);

        ChatResponse chatResponse = createChatResponse(
                "RAG 系统使用 MySQL 和 Milvus 是为了发挥各自优势。",
                "deepseek4pro", 120, 30, 150
        );
        when(callSpec.chatResponse()).thenReturn(chatResponse);

        AskRequest request = new AskRequest("RAG 系统为什么要同时使用 MySQL 和 Milvus？", null, null);
        AskResponse response = service.ask(request);

        // 验证检索参数使用了默认值
        ArgumentCaptor<SearchRequest> searchCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(searchCaptor.capture());
        SearchRequest searchRequest = searchCaptor.getValue();
        assertThat(searchRequest.getQuery()).isEqualTo(request.question());
        assertThat(searchRequest.getTopK()).isEqualTo(5);
        assertThat(searchRequest.getSimilarityThreshold()).isEqualTo(0.6);

        // 验证 question 记录
        ArgumentCaptor<TRagQuestion> questionCaptor = ArgumentCaptor.forClass(TRagQuestion.class);
        verify(questionMapper).insert(questionCaptor.capture());
        TRagQuestion savedQuestion = questionCaptor.getValue();
        assertThat(savedQuestion.getQuestion()).isEqualTo(request.question());
        assertThat(savedQuestion.getAnswer()).isEqualTo("RAG 系统使用 MySQL 和 Milvus 是为了发挥各自优势。");
        assertThat(savedQuestion.getTopK()).isEqualTo(5);
        assertThat(savedQuestion.getSimilarityThreshold()).isEqualTo(0.6);
        assertThat(savedQuestion.getCreatedAt()).isNotNull();

        // 验证 retrieval_results 记录了 3 条
        ArgumentCaptor<TRagRetrievalResult> retrievalCaptor = ArgumentCaptor.forClass(TRagRetrievalResult.class);
        verify(retrievalResultMapper, times(3)).insert(retrievalCaptor.capture());
        List<TRagRetrievalResult> savedResults = retrievalCaptor.getAllValues();
        assertThat(savedResults).hasSize(3);
        assertThat(savedResults.get(0).getRankNo()).isEqualTo(1);
        assertThat(savedResults.get(0).getScore()).isEqualTo(0.92);
        assertThat(savedResults.get(0).getSourceTitle()).isEqualTo("技术选型说明");
        assertThat(savedResults.get(1).getRankNo()).isEqualTo(2);
        assertThat(savedResults.get(2).getRankNo()).isEqualTo(3);

        // === 核心验证：llm_call_logs 中的 token 记录 ===
        ArgumentCaptor<TLlmCallLog> logCaptor = ArgumentCaptor.forClass(TLlmCallLog.class);
        verify(callLogMapper).insert(logCaptor.capture());
        TLlmCallLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getQuestionId()).isEqualTo(1L);
        assertThat(savedLog.getModel()).isEqualTo("deepseek4pro");
        assertThat(savedLog.getInputTokens()).isEqualTo(120L);
        assertThat(savedLog.getOutputTokens()).isEqualTo(30L);
        assertThat(savedLog.getTotalTokens()).isEqualTo(150L);
        assertThat(savedLog.getAnswer()).isEqualTo("RAG 系统使用 MySQL 和 Milvus 是为了发挥各自优势。");
        assertThat(savedLog.getPrompt()).contains("用户问题");
        assertThat(savedLog.getPrompt()).contains("检索上下文");
        assertThat(savedLog.getPrompt()).contains("RAG 系统为什么要同时使用 MySQL 和 Milvus？");
        assertThat(savedLog.getPrompt()).contains("技术选型说明");

        // 验证响应中的 token 信息
        assertThat(response.tokenUsage().inputTokens()).isEqualTo(120L);
        assertThat(response.tokenUsage().outputTokens()).isEqualTo(30L);
        assertThat(response.tokenUsage().totalTokens()).isEqualTo(150L);

        // 验证响应中的检索结果
        assertThat(response.retrievalResults()).hasSize(3);
        assertThat(response.retrievalResults().get(0).rank()).isEqualTo(1);
        assertThat(response.retrievalResults().get(0).score()).isEqualTo(0.92);
    }

    @Test
    void shouldAskWithCustomTopKAndThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                createDoc("测试内容", 1L, "标题", 0, 0.95)
        ));
        ChatResponse customChatResponse = createChatResponse("答案", "model", 50, 10, 60);
        when(callSpec.chatResponse()).thenReturn(customChatResponse);

        AskRequest request = new AskRequest("测试问题", 10, 0.8);
        service.ask(request);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(10);
        assertThat(captor.getValue().getSimilarityThreshold()).isEqualTo(0.8);
    }

    @Test
    void shouldHandleEmptyRetrievalResults() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ChatResponse emptyChatResponse = createChatResponse("根据当前知识库资料无法确定", "model", 20, 5, 25);
        when(callSpec.chatResponse()).thenReturn(emptyChatResponse);

        AskRequest request = new AskRequest("不存在的问题", null, null);
        AskResponse response = service.ask(request);

        assertThat(response.retrievalResults()).isEmpty();

        ArgumentCaptor<TLlmCallLog> logCaptor = ArgumentCaptor.forClass(TLlmCallLog.class);
        verify(callLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getPrompt()).contains("无匹配资料");
    }

    @Test
    void shouldBuildContextWithMultipleSources() {
        List<Document> docs = List.of(
                createDoc("DocA 片段1", 10L, "文档A", 0, 0.9),
                createDoc("DocB 片段1", 20L, "文档B", 0, 0.8)
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(docs);
        ChatResponse multiSourceResp = createChatResponse("综合回答", "model", 100, 40, 140);
        when(callSpec.chatResponse()).thenReturn(multiSourceResp);

        service.ask(new AskRequest("多文档问题", null, null));

        ArgumentCaptor<TLlmCallLog> logCaptor = ArgumentCaptor.forClass(TLlmCallLog.class);
        verify(callLogMapper).insert(logCaptor.capture());
        String prompt = logCaptor.getValue().getPrompt();
        assertThat(prompt).contains("来源 1：文档A");
        assertThat(prompt).contains("来源 2：文档B");
    }

    @Test
    void shouldHandleNullTokenUsage() {
        List<Document> docs = List.of(createDoc("content", 1L, "title", 0, 0.9));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(docs);

        ChatResponse responseWithoutUsage = createChatResponseWithoutUsage("答案");
        when(callSpec.chatResponse()).thenReturn(responseWithoutUsage);

        AskResponse response = service.ask(new AskRequest("问题", null, null));

        assertThat(response.tokenUsage().inputTokens()).isNull();
        assertThat(response.tokenUsage().outputTokens()).isNull();
        assertThat(response.tokenUsage().totalTokens()).isNull();

        ArgumentCaptor<TLlmCallLog> logCaptor = ArgumentCaptor.forClass(TLlmCallLog.class);
        verify(callLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getInputTokens()).isNull();
    }

    // ======================== getQuestionLog() 测试 ========================

    @Test
    void shouldGetQuestionLog() {
        TRagQuestion question = new TRagQuestion();
        question.setId(1L);
        question.setQuestion("测试问题");
        question.setAnswer("测试答案");
        question.setTopK(5);
        question.setSimilarityThreshold(0.6);
        question.setCreatedAt(Instant.parse("2026-06-16T10:00:00Z"));

        when(questionMapper.selectById(1L)).thenReturn(question);

        TLlmCallLog callLog = new TLlmCallLog();
        callLog.setId(200L);
        callLog.setQuestionId(1L);
        callLog.setModel("deepseek4pro");
        callLog.setPrompt("完整 prompt 内容");
        callLog.setAnswer("测试答案");
        callLog.setInputTokens(100L);
        callLog.setOutputTokens(50L);
        callLog.setTotalTokens(150L);
        callLog.setCreatedAt(Instant.parse("2026-06-16T10:00:01Z"));

        when(callLogMapper.selectOne(any())).thenReturn(callLog);

        TRagRetrievalResult result1 = new TRagRetrievalResult();
        result1.setRankNo(1);
        result1.setScore(0.95);
        result1.setSourceDocumentId(10L);
        result1.setSourceTitle("来源文档");
        result1.setChunkIndex(0);
        result1.setContent("检索内容1");

        TRagRetrievalResult result2 = new TRagRetrievalResult();
        result2.setRankNo(2);
        result2.setScore(0.88);
        result2.setSourceDocumentId(10L);
        result2.setSourceTitle("来源文档");
        result2.setChunkIndex(1);
        result2.setContent("检索内容2");

        when(retrievalResultMapper.selectList(any())).thenReturn(List.of(result1, result2));

        QuestionLogResponse log = service.getQuestionLog(1L);

        assertThat(log.questionId()).isEqualTo(1L);
        assertThat(log.question()).isEqualTo("测试问题");
        assertThat(log.answer()).isEqualTo("测试答案");
        assertThat(log.model()).isEqualTo("deepseek4pro");
        assertThat(log.prompt()).isEqualTo("完整 prompt 内容");
        assertThat(log.tokenUsage().inputTokens()).isEqualTo(100L);
        assertThat(log.tokenUsage().outputTokens()).isEqualTo(50L);
        assertThat(log.tokenUsage().totalTokens()).isEqualTo(150L);
        assertThat(log.createdAt()).isEqualTo(Instant.parse("2026-06-16T10:00:00Z"));
        assertThat(log.retrievalResults()).hasSize(2);
        assertThat(log.retrievalResults().get(0).score()).isEqualTo(0.95);
        assertThat(log.retrievalResults().get(1).score()).isEqualTo(0.88);
    }

    @Test
    void shouldThrowExceptionWhenQuestionNotFound() {
        when(questionMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getQuestionLog(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    void shouldThrowExceptionWhenCallLogNotFound() {
        TRagQuestion question = new TRagQuestion();
        question.setId(1L);
        when(questionMapper.selectById(1L)).thenReturn(question);
        when(callLogMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getQuestionLog(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LLM call log not found");
    }

    // ======================== 辅助方法 ========================

    private static Document createDoc(String text, Long documentId, String title, int chunkIndex, double score) {
        return Document.builder()
                .text(text)
                .metadata(Map.of("documentId", documentId, "title", title, "chunkIndex", chunkIndex))
                .score(score)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static ChatResponse createChatResponse(String answer, String model,
                                                    long inputTokens, long outputTokens, long totalTokens) {
        ChatResponse response = org.mockito.Mockito.mock(ChatResponse.class);
        Generation generation = org.mockito.Mockito.mock(Generation.class);
        ChatResponseMetadata metadata = org.mockito.Mockito.mock(ChatResponseMetadata.class);
        Usage usage = org.mockito.Mockito.mock(Usage.class);

        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(answer));
        when(response.getMetadata()).thenReturn(metadata);
        when(metadata.get("model")).thenReturn(model);
        when(metadata.getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn((int) inputTokens);
        when(usage.getCompletionTokens()).thenReturn((int) outputTokens);
        when(usage.getTotalTokens()).thenReturn((int) totalTokens);

        return response;
    }

    @SuppressWarnings("unchecked")
    private static ChatResponse createChatResponseWithoutUsage(String answer) {
        ChatResponse response = org.mockito.Mockito.mock(ChatResponse.class);
        Generation generation = org.mockito.Mockito.mock(Generation.class);
        ChatResponseMetadata metadata = org.mockito.Mockito.mock(ChatResponseMetadata.class);

        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(answer));
        when(response.getMetadata()).thenReturn(metadata);
        when(metadata.get("model")).thenReturn("test-model");
        when(metadata.getUsage()).thenReturn(null);

        return response;
    }
}
