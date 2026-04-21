# MindShare 完整复刻设计文档

## 1. 目标

本次目标不再停留在 phase-one 主链路复刻，而是将 `D:\dev\my_proj\java\mindshare` 继续演进为尽量对齐参考项目 `D:\dev\learn_proj\liunian\zhiguang_be` 的完整后端复刻版本。

目标模块包括：

- `auth`
- `profile`
- `storage`
- `knowpost`
- `cache`
- `search`
- `counter`
- `relation`
- `llm/rag`
- `config/common/user`
- 事件驱动链路：`outbox + kafka + canal`

本次复刻遵循既定原则：

- 包结构、接口命名、表结构、模块职责尽量贴近参考项目
- 在 `mindshare` 中保持你自己的项目命名，不再回退到 `tongji/zhiguang`
- 先做“真实能力补齐”，再考虑二次美化
- 保持现有 phase-one 测试资产，同时逐步补充对新增模块的验证

## 2. 当前状态与差距

### 2.1 当前 `mindshare` 已有模块

当前项目已经完成第一版：

- `auth`
- `profile`
- `storage`
- `knowpost`
- `feed`
- `search`
- 基础 `cache`
- 基础 `common/config/user`

当前代码量大致为：

- 主 Java 代码约 `3657` 行
- MyBatis XML 约 `288` 行
- 测试代码约 `1162` 行

### 2.2 相对参考项目缺失的核心能力

相对参考项目，当前 `mindshare` 还缺：

- `counter` 整套互动计数系统
- `relation` 关注关系系统
- `outbox + canal + kafka` 事件链路
- `llm/rag` 模块
- 更接近原版的 `search` 实现
- 更接近原版的 `feed` 缓存层级与一致性策略
- 更完整的本地运行文档与模块文档

### 2.3 当前已有模块中的关键简化点

即便已有的模块，也还存在与参考项目明显不一致的地方：

- 搜索没有索引正文内容
- 搜索在失败时会退化为内存索引
- 详情/Feed 的 liked/faved/count 仍为占位值
- `/description/suggest` 仍是字符串截断，不是模型能力
- Feed 目前只是 `Caffeine + Redis` 的简化版缓存
- 没有基于事件的索引同步和关系同步

## 3. 完整复刻的总体策略

本次不采用“一次性全量大改”，而采用四阶段演进方案。

推荐顺序：

1. `Phase 1`：升级 `search + knowpost + feed`，补齐已有模块的关键能力缺口
2. `Phase 2`：新增 `counter` 模块，打通点赞/收藏计数与状态
3. `Phase 3`：新增 `relation + outbox + kafka + canal`，补齐用户关系与事件驱动链路
4. `Phase 4`：新增 `llm/rag`，补齐 AI 描述生成与单篇知文问答

这样拆分的原因：

- 先修当前已经暴露出来的功能缺口，能最快提升真实完成度
- `counter` 是 `feed/search/detail` 的核心依赖，优先级高于 `relation`
- `relation` 与 `outbox/kafka/canal` 紧耦合，适合一起实现
- `rag` 对已有内容链路依赖最重，应放在最后

## 4. 四阶段设计

### Phase 1：Search / KnowPost / Feed 升级

目标：把现有第一版中“看起来有、其实还比较简化”的部分升级到接近参考项目的水平。

本阶段要解决的问题：

- 只有 `published + public` 的内容才允许进入搜索索引
- 搜索正文要基于真实内容而不是 description 占位
- 补齐 `search_after` 风格的稳定分页
- 补齐搜索排序策略中的业务字段预留
- 升级 feed/detail 缓存策略，使其更接近参考项目结构
- 重写 README 和 phase-one 文档，让仓库状态和实现一致

本阶段产物：

- 一个“能真正搜正文”的搜索系统
- 一个更稳定的 Feed/Detail 缓存层
- 更可靠的索引同步逻辑
- 更准确的项目说明文档

### Phase 2：Counter 模块

目标：将参考项目中的互动计数系统完整迁入 `mindshare`。

能力范围：

- 内容维度：点赞、收藏
- 用户维度：关注数、粉丝数、发文数、获赞数、获收藏数
- 位图事实层
- Kafka 事件聚合
- Redis SDS 汇总结构
- 缺失/异常下的按需重建
- 批量读取接口，供 Feed/搜索/详情使用

本阶段设计原则：

- 复用参考项目的键设计和聚合逻辑思想
- 包结构对齐 `counter/service/impl`, `counter/event`, `counter/schema`, `counter/api`
- 先保证正确性，再考虑高阶压测优化

本阶段产物：

- `counter` 完整包
- 点赞/收藏接口和查询接口
- `FeedItemResponse` / `KnowPostDetailResponse` 中真实计数和 liked/faved 状态

### Phase 3：Relation + Outbox + Kafka + Canal

目标：补齐用户关系系统和参考项目中的异步一致性链路。

能力范围：

- follow / unfollow
- followers / followings / relation status
- following 主表
- follower 伪从表
- outbox 表与 mapper
- canal 订阅 outbox binlog
- kafka bridge
- relation event processor
- 用户计数与关系列表缓存同步

本阶段设计原则：

- 以 following 表为主事实源
- 关系写入与 outbox 插入放在同一事务
- follower、计数、缓存作为异步伪从更新
- 优先贴近参考项目的数据流，不做额外架构发明

本阶段产物：

- `relation` 包完整落地
- `outbox`、`processor`、`event` 目录落地
- canal + kafka 基础链路跑通

### Phase 4：LLM / RAG

目标：补齐 AI 描述生成和单篇知文问答系统。

能力范围：

- 生成知文 description 的模型服务
- 向量索引构建
- 知文正文切块
- 指纹判断与幂等重建
- 单篇知文 SSE 问答接口
- 预索引与按需索引结合

本阶段设计原则：

- 包结构对齐参考项目的 `llm/service`, `llm/service/impl`, `llm/rag`
- 以 Elasticsearch VectorStore 作为第一选择
- 配置上保留切换模型提供方的空间

本阶段产物：

- 模型描述生成接口替代现有占位实现
- 单篇知文问答接口可跑通
- `llm/rag` 包完整落地

## 5. 目标代码结构

完成后的目标目录大致应包含：

- `src/main/java/com/mindshare/auth`
- `src/main/java/com/mindshare/cache`
- `src/main/java/com/mindshare/common`
- `src/main/java/com/mindshare/config`
- `src/main/java/com/mindshare/counter`
- `src/main/java/com/mindshare/knowpost`
- `src/main/java/com/mindshare/llm`
- `src/main/java/com/mindshare/profile`
- `src/main/java/com/mindshare/relation`
- `src/main/java/com/mindshare/search`
- `src/main/java/com/mindshare/storage`
- `src/main/java/com/mindshare/user`

资源目录需要逐步补齐：

- `src/main/resources/mapper/*.xml`
- `src/main/resources/application.yml`
- `src/main/resources/keys/*.pem`

数据库对象需要补齐：

- `users`
- `login_logs`
- `know_posts`
- `following`
- `follower`
- `outbox`

## 6. 测试与验证策略

每个阶段都必须有自己的验证闭环。

最低要求：

- 单元测试：关键服务、编码/解码、脚本逻辑
- SpringBoot 集成测试：controller + service + mapper 主链路
- 环境验证：MySQL / Redis / ES / Kafka / Canal 分阶段接入
- 每个阶段结束时运行一次 `mvn test`

阶段差异：

- Phase 1：H2 + mock/禁用外部依赖即可推进大部分测试
- Phase 2：建议增加 Redis 真实依赖或更强的嵌入式替代
- Phase 3：需要至少做 Kafka/Canal 配置验证和关键链路 smoke test
- Phase 4：需要做模型/向量索引相关的最小集成验证

## 7. 提交策略

完整复刻不适合超大提交，继续沿用现在这种按能力点拆 commit 的风格。

建议风格：

- `feat(search): index real content body and restrict publishable documents`
- `feat(feed): upgrade cache invalidation and page assembly`
- `feat(counter): add bitmap toggle and counter query api`
- `feat(counter): add kafka aggregation and sds rebuild`
- `feat(relation): add follow and unfollow flow`
- `feat(relation): add outbox and canal kafka bridge`
- `feat(llm): add description generation service`
- `feat(rag): add knowpost rag index and qa stream api`
- `docs: refresh mindshare project status and setup guide`

## 8. 风险与边界

主要风险：

- 一次性同时引入 Kafka、Canal、ES、Redis、LLM 后，本地调试复杂度显著上升
- phase-one 里已有的一些“测试绿但依赖被禁用”的情况，会掩盖真实集成问题
- 完整复刻后，仓库复杂度将接近参考项目，需要同步补文档和运行指南

风险控制策略：

- 严格按四阶段推进，不跨阶段偷做
- 每阶段结束时先让主链路变绿，再进入下一阶段
- 对外部依赖的接入从“能跑 smoke test”开始，不一口气追求最优实现

## 9. 完成标准

当以下条件都满足时，才可以称为接近完整复刻参考项目：

- `mindshare` 的模块结构与参考项目接近
- `auth/profile/storage/knowpost/feed/search/counter/relation/llm-rag` 全部落地
- 搜索、计数、关系、AI 问答均为真实能力，不再是占位实现
- `outbox + kafka + canal` 事件链路至少能在本地或测试环境跑通最小闭环
- README 与 docs 已更新到可独立运行和讲解的水平
