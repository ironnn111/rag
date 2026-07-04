package com.example.rag.controller;

import com.example.rag.dto.EvalScoreResponse;
import com.example.rag.dto.QuestionSummaryResponse;
import com.example.rag.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评估接口。
 *
 * <p>提供 LLM-as-Judge 问答质量评估能力。</p>
 */
@RestController
@RequestMapping("/api/eval")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    /**
     * 列出可评估的问题。
     */
    @GetMapping("/questions")
    public List<QuestionSummaryResponse> listQuestions() {
        return evaluationService.listEvaluableQuestions();
    }

    /**
     * 对指定问题执行 faithfulness + relevancy 评估。
     */
    @PostMapping("/{questionId}")
    public EvalScoreResponse evaluate(@PathVariable Long questionId) {
        return evaluationService.evaluate(questionId);
    }

    /**
     * 使用 RAGAS 对指定问题执行评估。
     */
    @PostMapping("/{questionId}/ragas")
    public EvalScoreResponse evaluateRagas(@PathVariable Long questionId) {
        return evaluationService.evaluateRagas(questionId);
    }

    /**
     * 查询历史评估结果。
     */
    @GetMapping("/{questionId}")
    public EvalScoreResponse getEvalScore(@PathVariable Long questionId) {
        return evaluationService.getEvalScore(questionId);
    }
}
