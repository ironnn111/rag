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
import com.example.rag.service.RerankerClient;
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

    @Autowired
    private RerankerClient rerankerClient;

    @Override
    @Transactional
    public AskResponse ask(AskRequest request) {
        int topK = request.getTopK() == null ? properties.getDefaultTopK() : request.getTopK();
        double threshold = request.getSimilarityThreshold() == null
                ? properties.getDefaultSimilarityThreshold()
                : request.getSimilarityThreshold();

        List<Document> documents = retrieve(request.getQuestion(), topK, threshold);
        LlmResult llmResult = callLlm(request.getQuestion(), documents);

        TRagQuestion question = persistQuestion(request, topK, threshold, llmResult);
        List<RetrievalHitResponse> hits = persistRetrievalResults(question.getId(), documents);
        TLlmCallLog callLog = persistCallLog(question.getId(), llmResult);

        return new AskResponse(
                question.getId(),
                question.getQuestion(),
                question.getAnswer(),
                new TokenUsageResponse(callLog.getInputTokens(), callLog.getOutputTokens(), callLog.getTotalTokens()),
                hits
        );
    }

    private record LlmResult(ChatResponse chatResponse, String prompt) {}

    private List<Document> retrieve(String question, int topK, double threshold) {
        int coarseTopK = topK * 3;
        List<Document> coarseDocs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(coarseTopK)
                .similarityThreshold(threshold)
                .build());

        List<String> rerankedTexts = rerankerClient.rerank(
                question,
                coarseDocs.stream().map(Document::getText).toList(),
                topK);

        return coarseDocs.stream()
                .filter(d -> rerankedTexts.contains(d.getText()))
                .limit(topK)
                .toList();
    }

    private LlmResult callLlm(String question, List<Document> documents) {
        String context = buildContext(documents);
        String userPrompt = """
                【用户问题】
                %s

                【检索上下文】
                %s
                """.formatted(question, context);

        ChatResponse chatResponse = chatClientBuilder.build().prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .chatResponse();

        return new LlmResult(chatResponse, SYSTEM_PROMPT + "\n\n" + userPrompt);
    }

    private TRagQuestion persistQuestion(AskRequest request, int topK, double threshold, LlmResult llmResult) {
        TRagQuestion question = new TRagQuestion();
        question.setQuestion(request.getQuestion());
        question.setAnswer(llmResult.chatResponse().getResult().getOutput().getText());
        question.setTopK(topK);
        question.setSimilarityThreshold(threshold);
        question.setCreatedAt(Instant.now());
        questionMapper.insert(question);
        return question;
    }

    private List<RetrievalHitResponse> persistRetrievalResults(Long questionId, List<Document> documents) {
        List<RetrievalHitResponse> hits = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            TRagRetrievalResult result = toRetrievalResult(questionId, i + 1, document);
            retrievalResultMapper.insert(result);
            hits.add(toRetrievalHit(result));
        }
        return hits;
    }

    private TLlmCallLog persistCallLog(Long questionId, LlmResult llmResult) {
        ChatResponse chatResponse = llmResult.chatResponse();
        String answer = chatResponse.getResult().getOutput().getText();
        Usage usage = chatResponse.getMetadata().getUsage();

        TLlmCallLog callLog = new TLlmCallLog();
        callLog.setQuestionId(questionId);
        callLog.setModel(modelName(chatResponse));
        callLog.setPrompt(llmResult.prompt());
        callLog.setAnswer(answer);
        callLog.setInputTokens(promptTokens(usage));
        callLog.setOutputTokens(completionTokens(usage));
        callLog.setTotalTokens(totalTokens(usage));
        callLog.setCreatedAt(Instant.now());
        callLogMapper.insert(callLog);
        return callLog;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionLogResponse getQuestionLog(Long questionId) {
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
