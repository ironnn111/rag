-- 原始知识文档表：保存用户准备的完整文档内容。
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    title VARCHAR(256) NOT NULL COMMENT '文档标题，用于检索结果展示',
    content LONGTEXT NOT NULL COMMENT '原始文档全文，Milvus 只保存切块后的向量文本',
    chunk_count INT NOT NULL COMMENT '当前文档切分出的片段数量',
    milvus_ids TEXT NULL COMMENT 'Milvus 中向量文档的 ID 列表（JSON 数组），用于删除时同步清理向量库',
    created_at DATETIME(6) NOT NULL COMMENT '文档入库时间'
) COMMENT='原始知识文档表，保存用户准备的完整文档内容';

-- RAG 问题主表：记录一次用户提问和最终答案。
CREATE TABLE IF NOT EXISTS rag_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    question TEXT NOT NULL COMMENT '用户原始问题',
    answer LONGTEXT NULL COMMENT '大模型基于检索上下文生成的答案',
    top_k INT NOT NULL COMMENT '本次检索使用的 topK',
    similarity_threshold DOUBLE NOT NULL COMMENT '本次检索使用的相似度阈值',
    created_at DATETIME(6) NOT NULL COMMENT '提问时间'
) COMMENT='RAG 问题主表，记录一次用户提问和最终答案';

-- RAG 检索结果明细表：保存一次问题命中的向量片段。
CREATE TABLE IF NOT EXISTS rag_retrieval_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    question_id BIGINT NOT NULL COMMENT '关联 rag_questions.id',
    rank_no INT NOT NULL COMMENT '检索结果排序，从 1 开始',
    score DOUBLE NULL COMMENT '向量相似度分数，由向量库返回',
    source_document_id BIGINT NULL COMMENT '来源文档 ID，对应 knowledge_documents.id',
    source_title VARCHAR(256) NULL COMMENT '来源文档标题，冗余保存便于日志查询',
    chunk_index INT NULL COMMENT '来源文档切块编号',
    content LONGTEXT NOT NULL COMMENT '命中的切块文本',
    INDEX idx_rag_retrieval_question_id (question_id)
) COMMENT='RAG 检索结果明细表，保存一次问题命中的向量片段';

-- 大模型调用日志表：保存 Prompt、答案和 token 用量。
CREATE TABLE IF NOT EXISTS llm_call_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    question_id BIGINT NOT NULL COMMENT '关联 rag_questions.id，一次问题只对应一条模型调用日志',
    model VARCHAR(128) NULL COMMENT '实际调用的模型名',
    prompt LONGTEXT NOT NULL COMMENT '系统 Prompt、用户问题和检索上下文拼接后的完整输入',
    answer LONGTEXT NOT NULL COMMENT '模型返回的最终答案',
    input_tokens BIGINT NULL COMMENT '输入 token 数，供应商未返回 usage 时允许为空',
    output_tokens BIGINT NULL COMMENT '输出 token 数，供应商未返回 usage 时允许为空',
    total_tokens BIGINT NULL COMMENT '总 token 数，供应商未返回 usage 时允许为空',
    created_at DATETIME(6) NOT NULL COMMENT '模型调用日志创建时间',
    UNIQUE KEY uk_llm_call_question_id (question_id)
) COMMENT='大模型调用日志表，保存 Prompt、答案和 token 用量';
