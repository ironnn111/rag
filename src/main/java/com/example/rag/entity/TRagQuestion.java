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

    @TableId(type = IdType.AUTO)
    private Long id;

    private String question;

    private String answer;

    private Integer topK;

    private Double similarityThreshold;

    private Instant createdAt;

    /**
     * 非数据库字段：一次问答关联的检索命中明细。
     */
    @TableField(exist = false)
    private List<TRagRetrievalResult> retrievalResults = new ArrayList<>();

    /**
     * 非数据库字段：一次问答关联的模型调用日志。
     */
    @TableField(exist = false)
    private TLlmCallLog callLog;
}
