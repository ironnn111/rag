package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 评估分数表实体。
 *
 * <p>记录 LLM-as-Judge 对问答结果的 faithfulness 和 relevancy 评估。</p>
 */
@Data
@TableName("eval_scores")
public class TEvalScore {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 rag_questions.id */
    private Long questionId;

    /** 忠实度评分 1-5，答案是否基于检索上下文 */
    private Integer faithfulnessScore;

    /** 忠实度评分理由 */
    private String faithfulnessReason;

    /** 相关性评分 1-5，答案是否切题 */
    private Integer relevancyScore;

    /** 相关性评分理由 */
    private String relevancyReason;

    /** 发送给 LLM 的完整评估 prompt */
    private String evalPrompt;

    /** 评估时间 */
    private Instant createdAt;
}
