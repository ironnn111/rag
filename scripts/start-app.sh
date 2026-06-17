#!/usr/bin/env bash

# 启动 Spring Boot RAG 应用。
# 启动前请先在 src/main/resources/application.yml 中配置：
# 1. DeepSeek API Key 和模型参数。
# 2. Embedding API Key 和模型参数。
# 3. MySQL、Milvus 连接参数。

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

cd "$PROJECT_DIR"

echo "正在启动 Spring Boot 应用..."
mvn spring-boot:run
