package com.example.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.config.RagProperties;
import com.example.rag.dto.AskRequest;
import com.example.rag.dto.AskResponse;
import com.example.rag.dto.QuestionLogResponse;
import com.example.rag.dto.RetrievalHitResponse;
import com.example.rag.dto.TokenUsageResponse;
import com.example.rag.entity.TLlmCallLog;
import com.example.rag.entity.TRagQuestion;
import com.example.rag.entity.TRagRetrievalResult;
import com.example.rag.mapper.TLlmCallLogMapper;
import com.example.rag.mapper.TRagQuestionMapper;
import com.example.rag.mapper.TRagRetrievalResultMapper;
import com.example.rag.service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * RAG 问答服务实现。
 *
 * <p>完整流程：向量检索相关文档片段，组装 Prompt，调用聊天模型，
 * 最后把问题、答案、检索结果和 token 使用情况写入 MySQL。</p>
 */
@Service
public class RagServiceImpl implements RagService {

    private static final String SYSTEM_PROMPT = """
            你是一个严谨的企业知识库问答助手。
            只能根据用户问题和给定的【检索上下文】回答。
            如果上下文不足以回答，直接说明“根据当前知识库资料无法确定”。
            回答要简洁，必要时列出依据来源。
            """;

    @Autowired
    private TRagQuestionMapper questionMapper;

    @Autowired
    private TRagRetrievalResultMapper retrievalResultMapper;

    @Autowired
    private TLlmCallLogMapper callLogMapper;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private RagProperties properties;

    @Override
    @Transactional
    public AskResponse ask(AskRequest request) {
        int topK = request.topK() == null ? properties.defaultTopK() : request.topK();
        double threshold = request.similarityThreshold() == null
                ? properties.defaultSimilarityThreshold()
                : request.similarityThreshold();

        // 先使用用户问题在 Milvus 中检索候选上下文。
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(request.question())
                .topK(topK)
                .similarityThreshold(threshold)
                .build());

        // 将检索结果作为上下文拼进用户 Prompt，限制模型只基于知识库回答。
        String context = buildContext(documents);
        String userPrompt = """
                【用户问题】
                %s

                【检索上下文】
                %s
                """.formatted(request.question(), context);

        // ChatClient 使用 application.yml 中配置的 DeepSeek/OpenAI-compatible 参数。
        ChatResponse chatResponse = chatClientBuilder.build().prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .chatResponse();

        String answer = chatResponse.getResult().getOutput().getText();
        Usage usage = chatResponse.getMetadata().getUsage();

        // 问题、检索结果、模型调用日志分别落表，方便后续排查和成本统计。
        TRagQuestion question = new TRagQuestion();
        question.setQuestion(request.question());
        question.setAnswer(answer);
        question.setTopK(topK);
        question.setSimilarityThreshold(threshold);
        question.setCreatedAt(Instant.now());
        questionMapper.insert(question);

        List<RetrievalHitResponse> hits = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            TRagRetrievalResult result = toRetrievalResult(question.getId(), i + 1, document);
            retrievalResultMapper.insert(result);
            question.getRetrievalResults().add(result);
            hits.add(toRetrievalHit(result));
        }

        TLlmCallLog callLog = new TLlmCallLog();
        callLog.setQuestionId(question.getId());
        callLog.setModel(modelName(chatResponse));
        callLog.setPrompt(SYSTEM_PROMPT + "\n\n" + userPrompt);
        callLog.setAnswer(answer);
        callLog.setInputTokens(promptTokens(usage));
        callLog.setOutputTokens(completionTokens(usage));
        callLog.setTotalTokens(totalTokens(usage));
        callLog.setCreatedAt(Instant.now());
        question.setCallLog(callLog);
        callLogMapper.insert(callLog);

        return new AskResponse(
                question.getId(),
                question.getQuestion(),
                question.getAnswer(),
                new TokenUsageResponse(callLog.getInputTokens(), callLog.getOutputTokens(), callLog.getTotalTokens()),
                hits
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionLogResponse getQuestionLog(Long questionId) {
        // 日志查询聚合问题主表、LLM 调用日志和检索命中明细。
        TRagQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question log not found: " + questionId);
        }

        TLlmCallLog log = callLogMapper.selectOne(new LambdaQueryWrapper<TLlmCallLog>()
                .eq(TLlmCallLog::getQuestionId, questionId));
        if (log == null) {
            throw new IllegalArgumentException("LLM call log not found: " + questionId);
        }

        List<RetrievalHitResponse> hits = retrievalResultMapper.selectList(new LambdaQueryWrapper<TRagRetrievalResult>()
                        .eq(TRagRetrievalResult::getQuestionId, questionId)
                        .orderByAsc(TRagRetrievalResult::getRankNo))
                .stream()
                .sorted(Comparator.comparing(TRagRetrievalResult::getRankNo))
                .map(this::toRetrievalHit)
                .toList();

        return new QuestionLogResponse(
                question.getId(),
                question.getQuestion(),
                question.getAnswer(),
                log.getModel(),
                log.getPrompt(),
                new TokenUsageResponse(log.getInputTokens(), log.getOutputTokens(), log.getTotalTokens()),
                question.getCreatedAt(),
                hits
        );
    }

    /**
     * 把向量检索命中的文档片段组织为模型可读的上下文文本。
     */
    private String buildContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return "无匹配资料。";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            Map<String, Object> metadata = document.getMetadata();
            builder.append("来源 ").append(i + 1)
                    .append("：").append(metadata.getOrDefault("title", "unknown"))
                    .append("，片段 ").append(metadata.getOrDefault("chunkIndex", "unknown"))
                    .append("\n")
                    .append(document.getText())
                    .append("\n\n");
        }
        return builder.toString();
    }

    /**
     * 将 Spring AI 返回的向量文档转换为检索结果表记录。
     */
    private TRagRetrievalResult toRetrievalResult(Long questionId, int rank, Document document) {
        Map<String, Object> metadata = document.getMetadata();
        TRagRetrievalResult result = new TRagRetrievalResult();
        result.setQuestionId(questionId);
        result.setRankNo(rank);
        result.setScore(document.getScore());
        result.setSourceDocumentId(longMetadata(metadata.get("documentId")));
        result.setSourceTitle(stringMetadata(metadata.get("title")));
        result.setChunkIndex(intMetadata(metadata.get("chunkIndex")));
        result.setContent(document.getText());
        return result;
    }

    private RetrievalHitResponse toRetrievalHit(TRagRetrievalResult result) {
        return new RetrievalHitResponse(
                result.getRankNo(),
                result.getScore(),
                result.getSourceDocumentId(),
                result.getSourceTitle(),
                result.getChunkIndex(),
                result.getContent()
        );
    }

    private String modelName(ChatResponse response) {
        Object model = response.getMetadata().get("model");
        return model == null ? null : model.toString();
    }

    private Long promptTokens(Usage usage) {
        return usage == null ? null : longValue(usage.getPromptTokens());
    }

    private Long completionTokens(Usage usage) {
        return usage == null ? null : longValue(usage.getCompletionTokens());
    }

    private Long totalTokens(Usage usage) {
        return usage == null ? null : longValue(usage.getTotalTokens());
    }

    private Long longValue(Integer value) {
        return value == null ? null : value.longValue();
    }

    private Long longMetadata(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.valueOf(value.toString());
    }

    private Integer intMetadata(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(value.toString());
    }

    private String stringMetadata(Object value) {
        return value == null ? null : value.toString();
    }
}
