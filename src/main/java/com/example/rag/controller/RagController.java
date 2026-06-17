package com.example.rag.controller;

import com.example.rag.dto.AskRequest;
import com.example.rag.dto.AskResponse;
import com.example.rag.dto.QuestionLogResponse;
import com.example.rag.service.RagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 问答接口。
 *
 * <p>提供问题检索问答和历史调用日志查询能力，不在 Controller 中编写业务流程。</p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    /**
     * 执行一次 RAG 问答，并返回答案、检索命中和 token 统计。
     */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return ragService.ask(request);
    }

    /**
     * 查询一次问答的完整日志，包括 prompt、答案、token 和检索结果。
     */
    @GetMapping("/questions/{questionId}")
    public QuestionLogResponse getQuestionLog(@PathVariable Long questionId) {
        return ragService.getQuestionLog(questionId);
    }
}
