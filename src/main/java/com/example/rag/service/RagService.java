package com.example.rag.service;

import com.example.rag.dto.AskRequest;
import com.example.rag.dto.AskResponse;
import com.example.rag.dto.QuestionLogResponse;

/**
 * RAG 问答领域服务接口。
 *
 * <p>对外暴露问答调用和调用日志查询能力，隐藏向量检索、模型调用和日志落库细节。</p>
 */
public interface RagService {

    /**
     * 执行一次 RAG 问答。
     */
    AskResponse ask(AskRequest request);

    /**
     * 根据问题 ID 查询完整调用日志。
     */
    QuestionLogResponse getQuestionLog(Long questionId);
}
