# MindShare 与参考项目差距分析

## 1. 背景

本文档用于回答一个具体问题：

- 当前 `D:\dev\my_proj\java\mindshare`
- 相比参考项目 `D:\dev\learn_proj\liunian\zhiguang_be`

到底还差什么，以及后续应该按什么顺序继续对齐和完善。

结论先说：

- 当前 `mindshare` 已经完成了 phase-one 主链路骨架，具备可演示的 `auth / profile / storage / knowpost / feed / search`
- 但和“完整原项目”相比，仍然存在三类明显差距：
  - 整个模块还没做
  - 已做模块仍然是 phase-one 简化版
  - 工程化、文档、联调环境还没有对齐到原项目级别

## 2. 当前已对齐到什么程度

### 2.1 已经具备的主链路

当前项目已经具备以下能力：

- `auth`：验证码发送、注册、登录、刷新、登出、重置密码、`/me`
- `profile`：当前用户资料查询与资料修改
- `storage`：基于帖子所有权校验的 OSS 预签名上传
- `knowpost`：草稿创建、内容确认、元数据修改、发布、删除、详情、公开 feed、我的 feed
- `feed`：`Caffeine + 可选 Redis` 的基础缓存
- `search`：基础索引、搜索、suggest，以及和 knowpost 写链路的同步

### 2.2 当前已经缩小的差距

结合当前工作树状态，和最初的 phase-one 版本相比，又补了一些和原项目更接近的行为：

- 搜索索引已限制为 `published + public`
- 搜索索引已支持从 `contentUrl` 读取正文，而不再只靠 `description`
- 搜索分页已从 offset 式 token 升级为基于排序字段的 cursor token
- 搜索命中正文时已返回正文片段，而不只是原始 description
- `knowpost` 的 publish / update / delete 已会同步触发搜索索引更新
- `profile` 已补齐 `POST /api/v1/profile/avatar`
- `feed` / `detail` 已有基础缓存、single-flight 与更细粒度的 public feed 失效机制

这意味着项目已经不是“只有接口壳子”的状态，而是进入了“可以演示，但还没完整复刻”的阶段。

## 3. 和完整原项目相比还缺什么

## 3.1 整个模块还没做

这些模块在参考项目中已经存在，但 `mindshare` 当前还没有落地：

### 3.1.1 `counter`

参考项目已有：

- `counter/api/ActionController.java`
- `counter/api/CounterController.java`
- `counter/service/CounterService.java`
- `counter/service/UserCounterService.java`
- `counter/event/*`
- `counter/schema/*`

当前缺失：

- 点赞/收藏动作接口
- 内容维度计数读取接口
- 用户维度计数体系
- 位图事实层
- Kafka 聚合与重建机制

直接影响：

- `feed`、`detail`、`search` 里的 `likeCount / favoriteCount / liked / faved` 仍然是占位值
- 还没有原项目的高并发计数设计亮点

### 3.1.2 `relation`

参考项目已有：

- `relation/api/RelationController.java`
- `relation/service/RelationService.java`
- `relation/mapper/RelationMapper.java`
- `relation/processor/RelationEventProcessor.java`

当前缺失：

- 关注/取关
- 关注状态查询
- followers / followings 查询
- 关系缓存和关系异步更新

直接影响：

- 当前项目还不具备原项目的社交关系主链路

### 3.1.3 `llm / rag`

参考项目已有：

- `llm/service/KnowPostDescriptionService.java`
- `llm/rag/RagIndexService.java`
- `llm/rag/RagQueryService.java`
- `knowpost/api/KnowPostAiController.java`
- `knowpost/api/KnowPostRagController.java`

当前缺失：

- 真实 AI 描述生成
- 单篇知文 RAG 索引
- SSE 流式问答
- 手动重建索引

直接影响：

- `/api/v1/knowposts/description/suggest` 目前只是字符串截断，不是原项目的 AI 能力
- 还没有原项目里很重要的问答系统展示面

### 3.1.4 `outbox + kafka + canal`

参考项目已有：

- `relation/outbox/OutboxMapper.java`
- `relation/outbox/CanalKafkaBridge.java`
- `relation/outbox/CanalOutboxConsumer.java`
- `search/outbox/CanalOutboxConsumerSearch.java`

当前缺失：

- `outbox` 表和 mapper
- 同事务写 outbox
- Canal 订阅 binlog
- Kafka bridge
- 搜索与关系系统的异步事件消费链路

直接影响：

- 当前搜索同步仍是应用内同步调用，不是原项目的事件驱动风格
- 缺少原项目在一致性与解耦上的关键架构亮点

## 3.2 已做模块，但仍然是简化版

## 3.2.1 `profile` 仍缺更完整字段语义

当前项目已经补齐：

- `POST /api/v1/profile/avatar`

但参考项目的用户资料语义更完整，当前也还没完全对齐：

- `zgId` 相关字段和唯一性校验未落地
- profile patch 目前偏最小实现，没有把原项目的资料约束全部迁入

## 3.2.2 `storage` 仍然是 phase-one 级别封装

当前 `storage` 已经支持：

- `POST /api/v1/storage/presign`
- 头像上传服务在业务流中的接入

但和原项目完整能力相比，还缺：

- 更细的上传场景扩展
- 更接近原项目的对象存储访问封装

## 3.2.3 `knowpost` 缺 AI/RAG 侧接口与事件侧钩子

当前 `knowpost` 已经有主链路，但相比原项目仍缺：

- `KnowPostAiController`
- `KnowPostRagController`
- 预索引 / 重建索引等 RAG 钩子
- outbox 事件写入
- 和 `counter`、`relation` 的联动

此外当前实现仍是 phase-one 风格：

- `description/suggest` 不是 LLM 服务
- 详情响应没有真实互动态
- 发布链路没有接入更完整的异步后处理

## 3.2.4 `feed` 缓存还没有对齐到原项目的复杂度

参考项目的 feed / detail 缓存包含更多机制：

- 本地 Caffeine + Redis 页面缓存 + Redis 片段缓存
- hotkey 检测与 TTL 延长
- single-flight 防止并发回源
- 更细粒度的 page/item/index 反向索引

当前项目还没有完全对齐：

- 当前缓存层是“最小可运行版”，不是原项目级别的三级缓存设计
- 目前只补了 public feed 定向失效和 detail/feed 的 single-flight，尚未补齐 Redis 片段缓存
- 没有 hotkey 探测
- 还没接入真实计数状态

## 3.2.5 `search` 还没有对齐到原项目的真实行为

虽然当前搜索已经比最初版本更接近原项目，但仍然差很多：

### 已经对齐的部分

- 有索引初始化
- 有搜索 API
- 有 suggest API
- 已限制只索引 `published + public`
- 已支持正文内容进入索引
- 已支持基于排序值的 cursor token 分页
- 已支持正文命中片段回显

### 仍然存在的差距

- 没有原项目的 `function_score` 业务加权
- 没有真实的互动计数字段参与排序
- 当前存在“ES 不可用时回退内存索引”的实现，这对 phase-one 很友好，但不属于原项目风格
- 还没有 `CanalOutboxConsumerSearch` 这类异步索引消费链路

换句话说：

- 当前搜索“能用”
- 但还不是原项目那套“相关性 + 业务权重 + 深分页稳定 + 异步索引同步”的完整实现

## 3.3 数据模型还没补齐

和参考项目完整形态相比，当前数据库对象仍不完整。

当前已经有：

- `users`
- `login_logs`
- `know_posts`

仍缺：

- `following`
- `follower`
- `outbox`

此外，现有表结构也仍有差距：

- `users` 还没对齐到原项目更完整的资料字段集合
- `know_posts` 还没承接完整的关系、计数、RAG、事件协作语义

## 3.4 工程化与文档还没有对齐

这部分不是业务功能，但如果目标是“完整复刻原项目并可继续长期维护”，当前也明显不够。

当前差距包括：

- `README.md` 仍然非常简短，基本还是 bootstrap 级别说明
- 没有完整的本地运行文档
- 没有 phase-one API 文档
- 没有 Kafka / Canal / ES / Redis / MySQL / LLM 的整体运行说明
- 缺少更强的集成测试分层

测试现状是：

- phase-one 主链路已经有不错的 SpringBoot/H2 测试
- 但 Redis / ES / Kafka / Canal / LLM 相关真实集成还没有建立完整验证闭环

## 4. 当前最关键的“未完成清单”

如果把“和完整原项目的差距”压缩成一份最关键待办，可以归纳为下面 10 条：

1. 补齐 `counter` 模块
2. 补齐 `relation` 模块
3. 补齐 `outbox + kafka + canal` 链路
4. 补齐 `llm / rag`
5. 把 `search` 排序升级为“相关性 + 业务权重”
6. 把 `feed/detail` 缓存升级到原项目级别的三级缓存
7. 把 `profile` 的更完整资料字段和约束补齐
8. 把 `like/favorite/follow` 等真实互动状态接入返回模型
9. 把搜索、关系、计数切到异步事件驱动风格
10. 持续补齐 README、运行手册、接口文档和环境说明

## 5. 建议的后续对齐顺序

如果后续要继续让我对齐原项目，我建议按下面顺序做，而不是乱序补功能。

### P0：先把当前已做模块从“能跑”升级到“更像原项目”

优先做：

- 搜索权重与高亮
- feed/detail 缓存策略升级
- profile 完整字段语义
- README / runbook / API 文档的持续维护

原因：

- 这部分都基于现有模块，收益高、上下文连续、返工成本低

### P1：补 `counter`

原因：

- `feed/detail/search` 的真实表现都依赖 `counter`
- 这是后续很多模块的基础设施

### P2：补 `relation + outbox + kafka + canal`

原因：

- 这是原项目中最重要的架构亮点之一
- 和 `counter`、缓存、用户维度数据强相关

### P3：补 `llm / rag`

原因：

- 这是最依赖完整内容链路和索引链路的模块
- 放在最后最稳妥

## 6. 一个更实用的判断

如果只从“简历项目可讲”角度看，当前项目已经能讲 phase-one 主链路。

如果从“尽量完整复刻原项目”角度看，当前项目还处在：

- 主链路已具备
- 中高级架构亮点尚未补齐
- 高并发、异步一致性、AI 问答三大特色还没真正到位

也就是说，当前项目更接近：

- “可运行的第一阶段复刻版”

而不是：

- “完整对齐原项目的复刻版”

## 7. 后续执行建议

后面继续完善时，建议采用下面的工作方式：

- 每一轮只对齐一个能力域，不跨模块发散
- 先补“与原项目差异最大的已做模块”，再补全缺失模块
- 每做完一个能力域，就同步补测试和文档
- 保留当前 `mindshare` 的命名，不回退到 `tongji/zhiguang`

如果按这个方向继续推进，下一轮最合适的起点是：

- 先补一轮 `search + feed + profile` 的原项目对齐优化

然后再进入：

- `counter`
- `relation + outbox + kafka + canal`
- `llm / rag`
