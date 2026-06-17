#!/usr/bin/env bash

# 启动 RAG 项目依赖的基础服务。
# 包括：
# 1. MySQL：保存文档、问题、检索结果和调用日志。
# 2. Milvus：保存向量并执行相似度检索。
# 3. etcd / MinIO：Milvus standalone 模式所需依赖。

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

cd "$PROJECT_DIR"

echo "正在启动 MySQL、Milvus、etcd、MinIO..."
docker compose up -d

echo "依赖服务启动命令已执行。"
echo "可使用以下命令查看状态："
echo "docker compose ps"
