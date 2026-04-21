# MindShare 完整复刻总实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `D:\dev\my_proj\java\mindshare` 从当前 phase-one 版本继续补齐为尽量对齐参考仓库 `D:\dev\learn_proj\liunian\zhiguang_be` 的完整 Java 后端复刻版。

**Architecture:** 保持 Spring Boot 单体后端架构，按四阶段渐进补齐 `search/knowpost/feed`、`counter`、`relation+outbox+kafka+canal`、`llm/rag`。每阶段都要求能独立通过测试并形成清晰 commit 边界。

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, MyBatis, MySQL, Redis, Caffeine, Elasticsearch, Kafka, Canal, Spring AI / DeepSeek or OpenAI-compatible model provider, Aliyun OSS, JUnit 5, Mockito

---

## 总体文件结构目标

### 现有目录保留并扩展

- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\cache`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\common`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\config`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\profile`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\search`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\storage`
- 保留：`D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\user`

### 需要新增的包

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\counter\...`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\relation\...`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\llm\...`

### 需要新增/扩展的资源

- Modify: `D:\dev\my_proj\java\mindshare\src\main\resources\application.yml`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\mapper\RelationMapper.xml`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\mapper\OutboxMapper.xml`
- Extend: `D:\dev\my_proj\java\mindshare\db\schema.sql`

---

## Phase 1: 升级 Search / KnowPost / Feed

### 目标

把当前“能跑但偏简化”的 `search/knowpost/feed` 升级到接近参考项目的真实能力。

### 关键结果

- 只索引 `published + public` 内容
- 搜索索引包含真实正文内容
- 支持更稳定的游标分页，逐步向 `search_after` 语义靠拢
- 修正搜索排序与 suggest 逻辑
- feed/detail 缓存策略更接近参考项目
- README 和本地运行说明更新到当前实现

### 主要文件

- Modify: `src/main/java/com/mindshare/search/index/SearchIndexService.java`
- Modify: `src/main/java/com/mindshare/search/service/impl/SearchServiceImpl.java`
- Modify: `src/main/java/com/mindshare/knowpost/service/impl/KnowPostServiceImpl.java`
- Modify: `src/main/java/com/mindshare/knowpost/service/impl/KnowPostFeedServiceImpl.java`
- Modify: `src/main/java/com/mindshare/cache/config/CacheProperties.java`
- Modify: `src/main/java/com/mindshare/cache/config/CacheConfig.java`
- Create: `src/main/java/com/mindshare/knowpost/service/KnowPostContentLoader.java`
- Create: `src/main/java/com/mindshare/knowpost/service/impl/HttpKnowPostContentLoader.java`
- Modify: `README.md`
- Create: `docs/api-phase1.md`
- Create: `docs/runbook-local.md`

### 建议提交顺序

- [ ] `feat(search): index only published public knowposts`
- [ ] `feat(search): index real content body from content url`
- [ ] `feat(search): upgrade cursor pagination and ranking behavior`
- [ ] `feat(feed): refine cache invalidation and detail caching`
- [ ] `docs: refresh phase one readme and runbook`

### 阶段完成标准

- 搜索能命中正文关键词
- 删除/隐藏内容后索引行为正确
- `mvn test` 通过
- README 与当前实现一致

---

## Phase 2: 新增 Counter 模块

### 目标

补齐参考项目的内容互动计数和用户维度计数系统。

### 关键结果

- 点赞/收藏行为接口
- 位图事实层
- 计数事件模型
- Kafka 聚合桶消费与 SDS 汇总
- 计数异常时的按需重建
- Feed/详情/搜索响应中的真实计数与 liked/faved 状态

### 主要文件

- Create: `src/main/java/com/mindshare/counter/api/ActionController.java`
- Create: `src/main/java/com/mindshare/counter/api/CounterController.java`
- Create: `src/main/java/com/mindshare/counter/api/dto/*.java`
- Create: `src/main/java/com/mindshare/counter/service/CounterService.java`
- Create: `src/main/java/com/mindshare/counter/service/UserCounterService.java`
- Create: `src/main/java/com/mindshare/counter/service/impl/CounterServiceImpl.java`
- Create: `src/main/java/com/mindshare/counter/service/impl/UserCounterServiceImpl.java`
- Create: `src/main/java/com/mindshare/counter/event/*.java`
- Create: `src/main/java/com/mindshare/counter/schema/*.java`
- Modify: `src/main/java/com/mindshare/knowpost/service/impl/KnowPostServiceImpl.java`
- Modify: `src/main/java/com/mindshare/knowpost/service/impl/KnowPostFeedServiceImpl.java`
- Modify: `src/main/java/com/mindshare/search/index/SearchIndexService.java`
- Modify: `src/main/resources/application.yml`

### 建议提交顺序

- [ ] `feat(counter): add action api and bitmap toggle service`
- [ ] `feat(counter): add counter query api and batch reads`
- [ ] `feat(counter): add kafka aggregation and sds writeback`
- [ ] `feat(counter): add rebuild and user counter support`
- [ ] `feat(feed): wire real counts and liked faved state`

### 阶段完成标准

- 点赞/收藏接口能跑通
- Feed/详情/搜索返回真实互动数据
- Redis 事实层与汇总层均可工作
- `mvn test` 通过，新增 counter 相关测试存在

---

## Phase 3: 新增 Relation + Outbox + Kafka + Canal

### 目标

补齐参考项目中的关注关系系统和事件驱动同步链路。

### 关键结果

- follow/unfollow/status/followers/followings 接口
- `following` 主表和 `follower` 伪从表
- `outbox` 表与 mapper
- 同事务写入关系和 outbox
- Canal 订阅 outbox binlog
- Kafka bridge 分发关系事件
- relation processor 异步更新缓存和用户计数

### 主要文件

- Create: `src/main/java/com/mindshare/relation/api/RelationController.java`
- Create: `src/main/java/com/mindshare/relation/service/RelationService.java`
- Create: `src/main/java/com/mindshare/relation/service/impl/RelationServiceImpl.java`
- Create: `src/main/java/com/mindshare/relation/mapper/RelationMapper.java`
- Create: `src/main/java/com/mindshare/relation/event/RelationEvent.java`
- Create: `src/main/java/com/mindshare/relation/processor/RelationEventProcessor.java`
- Create: `src/main/java/com/mindshare/relation/outbox/OutboxMapper.java`
- Create: `src/main/java/com/mindshare/relation/outbox/OutboxTopics.java`
- Create: `src/main/java/com/mindshare/relation/outbox/CanalKafkaBridge.java`
- Create: `src/main/java/com/mindshare/relation/outbox/CanalOutboxConsumer.java`
- Create: `src/main/resources/mapper/RelationMapper.xml`
- Create: `src/main/resources/mapper/OutboxMapper.xml`
- Modify: `db/schema.sql`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/java/com/mindshare/counter/service/impl/UserCounterServiceImpl.java`

### 建议提交顺序

- [ ] `feat(relation): add relation schema mapper and service`
- [ ] `feat(relation): add follow unfollow and relation query api`
- [ ] `feat(outbox): add relation outbox persistence`
- [ ] `feat(relation): add kafka and canal bridge for outbox events`
- [ ] `feat(relation): add async processor for counters and cache`

### 阶段完成标准

- 关注取关接口可用
- followers/followings 可查
- outbox 链路最小闭环能跑通
- 用户计数和缓存可由事件驱动更新

---

## Phase 4: 新增 LLM / RAG

### 目标

补齐 AI 描述生成与单篇知文问答系统。

### 关键结果

- 真实 description 生成服务替代当前截断占位
- 单篇知文内容切块并向量索引
- 根据指纹判断是否需要重建索引
- `/api/v1/knowposts/{id}/qa/stream` SSE 问答接口
- 手动重建索引接口

### 主要文件

- Create: `src/main/java/com/mindshare/llm/LlmConfig.java`
- Create: `src/main/java/com/mindshare/llm/service/KnowPostDescriptionService.java`
- Create: `src/main/java/com/mindshare/llm/service/impl/KnowPostDescriptionServiceImpl.java`
- Create: `src/main/java/com/mindshare/llm/rag/RagIndexService.java`
- Create: `src/main/java/com/mindshare/llm/rag/RagQueryService.java`
- Create: `src/main/java/com/mindshare/knowpost/api/KnowPostAiController.java`
- Create: `src/main/java/com/mindshare/knowpost/api/KnowPostRagController.java`
- Modify: `src/main/java/com/mindshare/knowpost/service/impl/KnowPostServiceImpl.java`
- Modify: `src/main/resources/application.yml`
- Modify: `pom.xml`

### 建议提交顺序

- [ ] `feat(llm): add knowpost description generation service`
- [ ] `feat(rag): add rag index build service`
- [ ] `feat(rag): add qa stream api`
- [ ] `feat(knowpost): trigger pre-index on publish and content confirm`

### 阶段完成标准

- `/description/suggest` 使用模型生成
- 单篇知文问答链路可以跑通
- 向量索引可重建且具备幂等判断

---

## 统一测试与验证要求

### 每阶段必须做

- [ ] 补充该阶段新增行为的测试
- [ ] 运行 `mvn test`
- [ ] 检查 `git status`
- [ ] 形成单独提交

### 跨阶段建议

- [ ] 增加环境说明文档，明确 MySQL/Redis/ES/Kafka/Canal 启动顺序
- [ ] 在 `README.md` 中维护当前阶段状态，而不是一次写死
- [ ] 对外部依赖相关测试至少补 smoke test 或最小集成测试

## 当前执行策略

本轮开始只进入 `Phase 1` 的首个子任务：

- 先修正搜索索引准入条件
- 再补齐正文索引内容
- 之后继续升级搜索分页和 feed 缓存

这样能最快消除当前实现与参考项目最明显的差距，同时为后续 `counter` 和 `rag` 铺路。
