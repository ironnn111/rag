package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG 问题表实体。
 *
 * <p>记录一次用户提问的主信息，包括问题、答案和本次检索参数。</p>
 */
@Data
@TableName("rag_questions")
public class TRagQuestion {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户原始问题 */
    private String question;

    /** 大模型基于检索上下文生成的答案 */
    private String answer;

    /** 本次检索使用的 topK */
    private Integer topK;

    /** 本次检索使用的相似度阈值 */
    private Double similarityThreshold;

    /** 提问时间 */
    private Instant createdAt;

    /** 非数据库字段：一次问答关联的检索命中明细 */
    @TableField(exist = false)
    private List<TRagRetrievalResult> retrievalResults = new ArrayList<>();

    /** 非数据库字段：一次问答关联的模型调用日志 */
    @TableField(exist = false)
    private TLlmCallLog callLog;
}
