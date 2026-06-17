#!/usr/bin/env bash

# 执行一次示例 RAG 问答。
# 这个脚本会调用 POST /api/rag/ask，问题内容来自 samples/question.json。
# 返回结果里会包含 answer、tokenUsage 和 retrievalResults。

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"

cd "$PROJECT_DIR"

echo "正在执行示例 RAG 问答..."
curl -sS -X POST "$BASE_URL/api/rag/ask" \
  -H 'Content-Type: application/json' \
  -d @samples/question.json

echo
echo "示例问答请求已完成。"
