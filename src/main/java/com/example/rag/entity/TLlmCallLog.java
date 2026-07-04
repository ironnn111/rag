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

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 rag_questions.id，一次问题只对应一条模型调用日志 */
    private Long questionId;

    /** 实际调用的模型名 */
    private String model;

    /** 系统 Prompt、用户问题和检索上下文拼接后的完整输入 */
    private String prompt;

    /** 模型返回的最终答案 */
    private String answer;

    /** 输入 token 数，供应商未返回 usage 时允许为空 */
    private Long inputTokens;

    /** 输出 token 数，供应商未返回 usage 时允许为空 */
    private Long outputTokens;

    /** 总 token 数，供应商未返回 usage 时允许为空 */
    private Long totalTokens;

    /** 模型调用日志创建时间 */
    private Instant createdAt;
}
