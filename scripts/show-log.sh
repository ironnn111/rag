#!/usr/bin/env bash

# 查询一次 RAG 调用日志。
# 用法：
#   ./scripts/show-log.sh 1
# 其中 1 是 /api/rag/ask 返回的 questionId。

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
QUESTION_ID="${1:-}"

if [[ -z "$QUESTION_ID" ]]; then
  echo "缺少 questionId。用法：./scripts/show-log.sh 1" >&2
  exit 1
fi

echo "正在查询 questionId=${QUESTION_ID} 的调用日志..."
curl -sS "$BASE_URL/api/rag/questions/$QUESTION_ID"

echo
echo "调用日志查询完成。"
