---
paths:
  - "**/*.java"
---

# 架构规则

- 本项目基于 Spring Boot + Spring AI 构建。
- Controller 不写业务逻辑，只负责参数接收、校验入口和响应返回。
- Service 负责业务编排，包括 RAG 检索、Prompt 组装、模型调用、日志落库等流程组织。
- Service 层必须拆分为抽象接口和实现类。
- Service 接口放在 `service` 包下。
- Service 实现类放在 `service.impl` 包下。
- Service 实现类命名必须以 `Impl` 结尾，例如 `RagServiceImpl`。
- Controller 和其他模块应依赖 Service 接口，不直接依赖 `service.impl` 下的实现类。
- Repository / Mapper 只负责数据访问，不承载业务判断。
- 关系型数据库访问统一使用 MyBatis-Plus。
- 不允许新增 Spring Data JPA、Hibernate Entity 注解、`JpaRepository` 或 `jakarta.persistence` 相关代码。
- MyBatis-Plus Mapper 必须放在 `com.example.rag.mapper` 包下。
- 不允许跨模块直接访问内部实现，应通过公开的 service、client、facade 或明确边界接口协作。
- 新逻辑优先复用已有领域服务，不要绕过已有 Service 直接访问底层 Mapper、Client 或工具实现。
- MySQL 用于保存结构化业务数据和调用日志。
- Milvus 用于保存向量数据并执行相似度检索。
