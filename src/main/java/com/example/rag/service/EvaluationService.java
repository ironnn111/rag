package com.example.rag.service;

import com.example.rag.dto.EvalScoreResponse;
import com.example.rag.dto.QuestionSummaryResponse;

import java.util.List;

/**
 * LLM-as-Judge 评估服务。
 *
 * <p>对 RAG 问答结果进行 faithfulness 和 relevancy 两个维度的自动评估。</p>
 */
public interface EvaluationService {

    /**
     * 列出可评估的问题列表。
     */
    List<QuestionSummaryResponse> listEvaluableQuestions();

    /**
     * 对指定问题执行评估，返回评估结果。
     */
    EvalScoreResponse evaluate(Long questionId);

    /**
     * 查询某次问题的历史评估记录。
     */
    EvalScoreResponse getEvalScore(Long questionId);
}
