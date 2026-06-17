package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 大模型调用日志表实体。
 *
 * <p>保存最终发送给模型的 Prompt、模型回答以及 token 使用情况。</p>
 */
@Data
@TableName("llm_call_logs")
public class TLlmCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private String model;

    private String prompt;

    private String answer;

    private Long inputTokens;

    private Long outputTokens;

    private Long totalTokens;

    private Instant createdAt;
}
