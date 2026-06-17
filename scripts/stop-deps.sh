#!/usr/bin/env bash

# 停止 RAG 项目的本地依赖服务。
# 默认不会删除 volumes 目录，因此 MySQL、Milvus、MinIO、etcd 的数据会保留。

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

cd "$PROJECT_DIR"

echo "正在停止本地依赖服务..."
docker compose down

echo "依赖服务已停止。"
