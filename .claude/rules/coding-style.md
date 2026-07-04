---
paths:
  - "**/*.java"
---

# 编码风格

- DTO、VO、Entity 命名必须明确区分。
- 直接映射数据库表的 Entity 类名必须以 `T` 开头，例如 `TKnowledgeDocument`。
- DTO、VO、Service、Controller、Mapper 不需要使用 `T` 前缀，除非它们本身是数据库实体。
- 数据库 Entity 类必须使用 Lombok `@Data`。
- 数据库 Entity 类不要手写 getter / setter。
- 数据库 Entity 类必须添加类级 Javadoc 注释，说明对应的数据库表及用途。
- 数据库 Entity 类的每个字段必须添加注释，说明字段含义。
- DTO 类必须添加类级 Javadoc 注释，说明数据结构用途。
- DTO 类的每个字段必须添加注释，说明字段含义。
- MyBatis-Plus Mapper 命名应与 Entity 对应，例如 `TKnowledgeDocumentMapper`。
- Spring Bean 依赖注入统一使用字段注入，在字段上显式标注 `@Autowired`。
- 不使用构造方法注入，避免为依赖注入单独编写构造方法。
- 优先复用现有 util、client、service，不要重复新增相似工具类。
- 单个方法超过 80 行时，优先考虑拆分为更小的私有方法或领域服务方法。
- 不要新增与现有能力重复的封装层。
- 新增类、方法、字段命名要表达业务含义，避免使用含糊缩写。
- 注释只写必要的业务约束、复杂流程或非显然原因，不写解释代码表面的注释。
- `@ConfigurationProperties` 类必须使用 Lombok `@Data`，不使用 Java record。
- 所有 DTO、VO、Entity 以及数据承载类（包括 record）必须使用 Lombok `@Data`，禁止使用 Java record。
