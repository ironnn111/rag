#!/usr/bin/env python3
"""
RAGAS 评估脚本：读取 MySQL 中的问答和检索结果，使用 RAGAS 计算评估指标并写回 eval_scores 表。

用法:
    python ragas_eval.py --question-id 1
    python ragas_eval.py --question-id 1 --dry-run    # 只打印不写库
    python ragas_eval.py --all                        # 评估所有未评估的问题

环境变量（可选，覆盖默认配置）:
    MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE
    DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL, DEEPSEEK_MODEL
    EMBEDDING_BASE_URL, EMBEDDING_MODEL
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone

import pymysql
from ragas import evaluate, EvaluationDataset, SingleTurnSample
from ragas.llms import LangchainLLMWrapper
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

# ─── 数据库配置 ──────────────────────────────────────────────
DB_CONFIG = {
    "host": os.getenv("MYSQL_HOST", "127.0.0.1"),
    "port": int(os.getenv("MYSQL_PORT", "3306")),
    "user": os.getenv("MYSQL_USER", "root"),
    "password": os.getenv("MYSQL_PASSWORD", "Qq12345678"),
    "database": os.getenv("MYSQL_DATABASE", "rag_demo"),
    "charset": "utf8mb4",
}

# ─── LLM 配置（用于 RAGAS Judge） ────────────────────────────
LLM_CONFIG = {
    "api_key": os.getenv("DEEPSEEK_API_KEY", "your-deepseek-api-key"),
    "base_url": os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    "model": os.getenv("DEEPSEEK_MODEL", "deepseek-v4-pro"),
}

# ─── Embedding 配置（RAGAS AnswerRelevancy 需要） ─────────────
EMBEDDING_CONFIG = {
    "base_url": os.getenv("EMBEDDING_BASE_URL", "http://127.0.0.1:18080/v1"),
    "model": os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5"),
}


def get_db():
    """创建 MySQL 连接。"""
    return pymysql.connect(
        host=DB_CONFIG["host"],
        port=DB_CONFIG["port"],
        user=DB_CONFIG["user"],
        password=DB_CONFIG["password"],
        database=DB_CONFIG["database"],
        charset=DB_CONFIG["charset"],
        cursorclass=pymysql.cursors.DictCursor,
    )


def load_data(question_id):
    """加载指定 question_id 的问题、答案和检索上下文。"""
    db = get_db()
    try:
        with db.cursor() as cur:
            cur.execute(
                "SELECT id, question, answer FROM rag_questions WHERE id = %s",
                (question_id,),
            )
            question = cur.fetchone()
            if not question:
                print(f"[ERROR] 问题 {question_id} 不存在", file=sys.stderr)
                return None

            cur.execute(
                "SELECT content, source_title FROM rag_retrieval_results "
                "WHERE question_id = %s ORDER BY rank_no ASC",
                (question_id,),
            )
            retrieval_rows = cur.fetchall()

        contexts = [r["content"] for r in retrieval_rows]
        if not contexts:
            print(f"[WARN] 问题 {question_id} 没有检索结果", file=sys.stderr)

        return {
            "question": question["question"],
            "answer": question["answer"],
            "contexts": contexts,
        }
    finally:
        db.close()


def load_all_unscored():
    """加载所有未评估过的问题。"""
    db = get_db()
    try:
        with db.cursor() as cur:
            cur.execute(
                "SELECT id FROM rag_questions WHERE answer IS NOT NULL AND answer != '' "
                "AND id NOT IN (SELECT question_id FROM eval_scores WHERE faithfulness_reason LIKE 'RAGAS%') "
                "ORDER BY created_at DESC"
            )
            rows = cur.fetchall()
        return [r["id"] for r in rows]
    finally:
        db.close()


def to_score(value, scale=5):
    """将 0-1 浮点数映射到 1-scale 整数。"""
    if value is None:
        return 1
    return max(1, min(scale, round(value * scale)))


def run_ragas(data, question_id):
    """执行 RAGAS 评估。"""
    if not data["contexts"]:
        print(f"[SKIP] 问题 {question_id} 无检索上下文，跳过评估")
        return None

    # 构建 RAGAS LLM
    llm = LangchainLLMWrapper(ChatOpenAI(
        api_key=LLM_CONFIG["api_key"],
        base_url=LLM_CONFIG["base_url"],
        model=LLM_CONFIG["model"],
        temperature=0.1,
    ))

    # 构建 Embedding（AnswerRelevancy 需要）
    embeddings = OpenAIEmbeddings(
        api_key="not-needed",
        base_url=EMBEDDING_CONFIG["base_url"],
        model=EMBEDDING_CONFIG["model"],
    )

    from ragas.metrics import (
        Faithfulness,
        AnswerRelevancy,
        ContextPrecision,
        ContextRecall,
    )

    sample = SingleTurnSample(
        user_input=data["question"],
        response=data["answer"],
        retrieved_contexts=data["contexts"],
    )
    dataset = EvaluationDataset(samples=[sample])

    metrics = [
        Faithfulness(),
        AnswerRelevancy(llm=llm, embeddings=embeddings),
        ContextPrecision(),
        ContextRecall(),
    ]

    print(f"[INFO] 开始 RAGAS 评估 question_id={question_id}")
    print(f"  问题: {data['question'][:80]}...")
    print(f"  上下文数: {len(data['contexts'])}")
    print(f"  答案长度: {len(data['answer'])}")

    result = evaluate(dataset, metrics=metrics, llm=llm)

    # 提取结果
    scores = {}
    for col in result.columns:
        val = result[col].iloc[0] if hasattr(result[col], 'iloc') else result[col][0]

        # 处理 Fraction 类型（ContextPrecision/Recall 可能返回 Fraction）
        if hasattr(val, 'numerator') and hasattr(val, 'denominator'):
            val = float(val.numerator) / float(val.denominator) if val.denominator != 0 else 0.0
        else:
            val = float(val) if val is not None else 0.0

        scores[col] = val
        print(f"  {col}: {val:.4f}")

    return scores


def save_result(question_id, scores, dry_run=False):
    """将 RAGAS 结果写入 eval_scores 表。"""
    faithfulness_raw = scores.get("faithfulness", 0)
    relevancy_raw = scores.get("answer_relevancy", 0)
    precision_raw = scores.get("context_precision", 0)
    recall_raw = scores.get("context_recall", 0)

    faith_score = to_score(faithfulness_raw)
    faith_reason = (
        f"RAGAS faithfulness={faithfulness_raw:.4f}，"
        f"context_precision={precision_raw:.4f}，"
        f"context_recall={recall_raw:.4f}"
    )
    relevancy_score = to_score(relevancy_raw)
    relevancy_reason = (
        f"RAGAS answer_relevancy={relevancy_raw:.4f}，"
        f"context_precision={precision_raw:.4f}，"
        f"context_recall={recall_raw:.4f}"
    )
    eval_prompt = json.dumps(scores, ensure_ascii=False, indent=2)
    now = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S.%f")

    if dry_run:
        print(f"\n[DRY-RUN] 将写入 eval_scores:")
        print(f"  question_id={question_id}")
        print(f"  faithfulness_score={faith_score}, reason={faith_reason}")
        print(f"  relevancy_score={relevancy_score}, reason={relevancy_reason}")
        return

    db = get_db()
    try:
        with db.cursor() as cur:
            # 删除旧 RAGAS 评估
            cur.execute(
                "DELETE FROM eval_scores WHERE question_id = %s AND faithfulness_reason LIKE 'RAGAS%'",
                (question_id,),
            )
            cur.execute(
                "INSERT INTO eval_scores "
                "(question_id, faithfulness_score, faithfulness_reason, "
                "relevancy_score, relevancy_reason, eval_prompt, created_at) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s)",
                (question_id, faith_score, faith_reason,
                 relevancy_score, relevancy_reason, eval_prompt, now),
            )
        db.commit()
        print(f"[OK] 评估结果已写入 question_id={question_id}")
    except Exception as e:
        db.rollback()
        print(f"[ERROR] 写入数据库失败: {e}", file=sys.stderr)
        raise
    finally:
        db.close()


def main():
    parser = argparse.ArgumentParser(description="RAGAS RAG 评估脚本")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--question-id", type=int, help="评估指定 question_id")
    group.add_argument("--all", action="store_true", help="评估所有未评估的问题")
    parser.add_argument("--dry-run", action="store_true", help="只打印评估结果，不写入数据库")
    args = parser.parse_args()

    if args.all:
        question_ids = load_all_unscored()
        if not question_ids:
            print("[INFO] 没有待评估的问题")
            return
        print(f"[INFO] 找到 {len(question_ids)} 个待评估问题")
    else:
        question_ids = [args.question_id]

    success = 0
    fail = 0
    for qid in question_ids:
        try:
            data = load_data(qid)
            if data is None:
                fail += 1
                continue
            scores = run_ragas(data, qid)
            if scores is None:
                fail += 1
                continue
            save_result(qid, scores, dry_run=args.dry_run)
            success += 1
        except Exception as e:
            print(f"[ERROR] 问题 {qid} 评估失败: {e}", file=sys.stderr)
            fail += 1

    print(f"\n── 完成: 成功 {success}, 失败 {fail} ──")


if __name__ == "__main__":
    main()
