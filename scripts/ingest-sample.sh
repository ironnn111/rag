#!/usr/bin/env bash

# 导入示例知识文档。
# 这个脚本会调用 POST /api/documents，把 samples/document.json 写入 MySQL，
# 并将切块后的内容写入 Milvus 向量库。

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"

cd "$PROJECT_DIR"

echo "正在导入示例文档到知识库..."
curl -sS -X POST "$BASE_URL/api/documents" \
  -H 'Content-Type: application/json' \
  -d @samples/document.json

echo
echo "示例文档导入请求已完成。"
