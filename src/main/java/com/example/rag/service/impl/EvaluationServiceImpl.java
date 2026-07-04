package com.example.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.dto.EvalScoreResponse;
import com.example.rag.dto.QuestionSummaryResponse;
import com.example.rag.entity.TEvalScore;
import com.example.rag.entity.TLlmCallLog;
import com.example.rag.entity.TRagQuestion;
import com.example.rag.entity.TRagRetrievalResult;
import com.example.rag.mapper.TEvalScoreMapper;
import com.example.rag.mapper.TLlmCallLogMapper;
import com.example.rag.mapper.TRagQuestionMapper;
import com.example.rag.mapper.TRagRetrievalResultMapper;
import com.example.rag.service.EvaluationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final String FAITHFULNESS_PROMPT = """
            你是一个 RAG 系统评估专家。请评估以下答案是否忠实于检索到的上下文，即答案中的信息是否能从上下文中找到依据。

            【用户问题】
            %s

            【检索到的上下文】
            %s

            【生成的答案】
            %s

            请以 JSON 格式返回评估结果，不要输出其他内容：
            {"score": 1-5的整数, "reason": "评分理由"}

            评分标准：
            1- 答案完全编造，与上下文无关
            2- 答案大部分编造，仅少量与上下文相关
            3- 答案部分忠实、部分编造
            4- 答案基本忠实于上下文，有轻微不一致
            5- 答案完全忠实于上下文
            """;

    private static final String RELEVANCY_PROMPT = """
            你是一个 RAG 系统评估专家。请评估以下答案是否与用户问题相关，即答案是否有效回应了用户的问题。

            【用户问题】
            %s

            【生成的答案】
            %s

            请以 JSON 格式返回评估结果，不要输出其他内容：
            {"score": 1-5的整数, "reason": "评分理由"}

            评分标准：
            1- 答案完全不相关，答非所问
            2- 答案基本不相关，仅少量回应问题
            3- 答案部分相关，部分偏离
            4- 答案基本相关，有轻微不完整
            5- 答案完全切题，完整回应问题
            """;

    @Autowired
    private TRagQuestionMapper questionMapper;

    @Autowired
    private TRagRetrievalResultMapper retrievalResultMapper;

    @Autowired
    private TLlmCallLogMapper callLogMapper;

    @Autowired
    private TEvalScoreMapper evalScoreMapper;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionSummaryResponse> listEvaluableQuestions() {
        List<TRagQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<TRagQuestion>()
                        .isNotNull(TRagQuestion::getAnswer)
                        .orderByDesc(TRagQuestion::getCreatedAt));
        return questions.stream()
                .map(q -> new QuestionSummaryResponse(q.getId(), q.getQuestion(), q.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public EvalScoreResponse evaluate(Long questionId) {
        TRagQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("问题不存在: " + questionId);
        }
        if (question.getAnswer() == null || question.getAnswer().isEmpty()) {
            throw new IllegalArgumentException("该问题尚未生成答案");
        }

        String context = buildContext(questionId);
        ChatClient chatClient = chatClientBuilder.build();

        // faithfulness 评估
        String faithPrompt = String.format(FAITHFULNESS_PROMPT, question.getQuestion(), context, question.getAnswer());
        JsonNode faithResult = callJudge(chatClient, faithPrompt);

        // relevancy 评估
        String relevancyPrompt = String.format(RELEVANCY_PROMPT, question.getQuestion(), question.getAnswer());
        JsonNode relevancyResult = callJudge(chatClient, relevancyPrompt);

        String fullPrompt = "=== faithfulness ===\n" + faithPrompt + "\n\n=== relevancy ===\n" + relevancyPrompt;

        // 删除旧评估记录
        evalScoreMapper.delete(new LambdaQueryWrapper<TEvalScore>()
                .eq(TEvalScore::getQuestionId, questionId));

        TEvalScore evalScore = new TEvalScore();
        evalScore.setQuestionId(questionId);
        evalScore.setFaithfulnessScore(faithResult.get("score").asInt());
        evalScore.setFaithfulnessReason(faithResult.get("reason").asText());
        evalScore.setRelevancyScore(relevancyResult.get("score").asInt());
        evalScore.setRelevancyReason(relevancyResult.get("reason").asText());
        evalScore.setEvalPrompt(fullPrompt);
        evalScore.setCreatedAt(Instant.now());
        evalScoreMapper.insert(evalScore);

        return toResponse(evalScore);
    }

    @Override
    @Transactional
    public EvalScoreResponse evaluateRagas(Long questionId) {
        TRagQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("问题不存在: " + questionId);
        }
        if (question.getAnswer() == null || question.getAnswer().isEmpty()) {
            throw new IllegalArgumentException("该问题尚未生成答案");
        }

        String projectDir = System.getProperty("user.dir");
        Path scriptPath = Path.of(projectDir, "scripts", "ragas_eval.py");
        if (!scriptPath.toFile().exists()) {
            throw new RuntimeException("RAGAS 脚本不存在: " + scriptPath);
        }

        ProcessBuilder pb = new ProcessBuilder(
                "python3", scriptPath.toString(), "--question-id", questionId.toString());
        pb.directory(Path.of(projectDir).toFile());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("RAGAS 评估超时（120 秒）");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("RAGAS 评估失败，退出码 " + exitCode
                        + "\n输出:\n" + output);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法执行 RAGAS 脚本: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("RAGAS 评估被中断", e);
        }

        return getEvalScore(questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public EvalScoreResponse getEvalScore(Long questionId) {
        TEvalScore evalScore = evalScoreMapper.selectOne(
                new LambdaQueryWrapper<TEvalScore>()
                        .eq(TEvalScore::getQuestionId, questionId)
                        .orderByDesc(TEvalScore::getCreatedAt)
                        .last("LIMIT 1"));
        return evalScore == null ? null : toResponse(evalScore);
    }

    private JsonNode callJudge(ChatClient chatClient, String prompt) {
        String response = chatClient.prompt()
                .system("你是一个严谨的评估专家，必须严格按 JSON 格式返回结果。")
                .user(prompt)
                .call()
                .content();

        String json = extractJson(response);
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("评估结果解析失败: " + response, e);
        }
    }

    private String extractJson(String response) {
        String trimmed = response.strip();
        if (trimmed.startsWith("```json")) {
            int end = trimmed.indexOf("```", 7);
            return end > 0 ? trimmed.substring(7, end).strip() : trimmed.substring(7).strip();
        }
        if (trimmed.startsWith("```")) {
            int end = trimmed.indexOf("```", 3);
            return end > 0 ? trimmed.substring(3, end).strip() : trimmed.substring(3).strip();
        }
        return trimmed;
    }

    private String buildContext(Long questionId) {
        List<TRagRetrievalResult> results = retrievalResultMapper.selectList(
                new LambdaQueryWrapper<TRagRetrievalResult>()
                        .eq(TRagRetrievalResult::getQuestionId, questionId)
                        .orderByAsc(TRagRetrievalResult::getRankNo));
        if (results.isEmpty()) {
            return "无检索结果";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            TRagRetrievalResult r = results.get(i);
            builder.append("来源 ").append(i + 1)
                    .append("：").append(r.getSourceTitle() != null ? r.getSourceTitle() : "unknown")
                    .append("，片段 ").append(r.getChunkIndex())
                    .append("\n").append(r.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    private EvalScoreResponse toResponse(TEvalScore e) {
        return new EvalScoreResponse(
                e.getId(), e.getQuestionId(),
                e.getFaithfulnessScore(), e.getFaithfulnessReason(),
                e.getRelevancyScore(), e.getRelevancyReason(),
                e.getCreatedAt());
    }
}
