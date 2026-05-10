SET NAMES utf8mb4;

DELETE FROM follower;
DELETE FROM following;
DELETE FROM outbox;
DELETE FROM know_posts;
DELETE FROM login_logs;
DELETE FROM users;

-- ==================== Users (10) ====================
-- Password for all: Test1234

INSERT INTO users (id, phone, email, password_hash, nickname, avatar, bio, zg_id, tags_json, birthday, school, gender)
VALUES
(1, '13800000001', 'alice@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Alice Chen', '', '全栈工程师，热爱技术分享与开源社区', 'alice_chen', '["Java","Spring","React","System Design"]', '1998-05-20', '浙江大学', 'FEMALE'),
(2, '13800000002', 'bob@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Bob Wang', '', '深耕AI与大数据，曾在阿里云从事机器学习平台研发', 'bob_wang', '["AI","BigData","Python","MLOps"]', '1997-10-15', '北京大学', 'MALE'),
(3, '13800000003', 'carol@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Carol Zhang', '', '前端架构师，设计系统和用户体验爱好者', 'carol_zhang', '["Frontend","TypeScript","Design Systems","CSS"]', '1999-03-08', '上海交通大学', 'FEMALE'),
(4, '13800000004', 'david@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'David Liu', '', 'Golang 和云原生布道师，Kubernetes contributor', 'david_liu', '["Go","Kubernetes","Cloud Native","DevOps"]', '1996-07-22', '华中科技大学', 'MALE'),
(5, '13800000005', 'emma@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Emma Li', '', '数据库内核开发，PostgreSQL 贡献者', 'emma_li', '["Database","PostgreSQL","C","Distributed Systems"]', '1998-12-01', '清华大学', 'FEMALE'),
(6, '13800000006', 'frank@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Frank Zhao', '', '移动端开发专家，Flutter & Swift 双修', 'frank_zhao', '["Mobile","Flutter","iOS","Android"]', '1995-04-18', '哈尔滨工业大学', 'MALE'),
(7, '13800000007', 'grace@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Grace Wu', '', '安全工程师，专注 Web 安全和渗透测试', 'grace_wu', '["Security","Web Security","Penetration Testing","CTF"]', '1997-09-30', '电子科技大学', 'FEMALE'),
(8, '13800000008', 'henry@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Henry Yang', '', '算法工程师，推荐系统和搜索排序方向', 'henry_yang', '["Algorithm","Recommendation","Search","Machine Learning"]', '1996-02-14', '中国科学技术大学', 'MALE'),
(9, '13800000009', 'ivy@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Ivy Huang', '', '技术管理者，前大厂工程效能负责人', 'ivy_huang', '["Engineering Management","Agile","Team Building","OKR"]', '1993-11-05', '复旦大学', 'FEMALE'),
(10, '13800000010', 'jack@test.com', '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K', 'Jack Xu', '', '独立开发者，全栈产品经理思维', 'jack_xu', '["Indie Dev","SaaS","Startup","Product"]', '1994-06-28', '南京大学', 'MALE');

-- ==================== KnowPosts (30) ====================
INSERT INTO know_posts (id, creator_id, status, type, visible, is_top, tag_id, title, description, tags, img_urls, publish_time, create_time, update_time)
VALUES
(1, 1, 'published', 'article', 'public', TRUE, NULL, 'Spring Boot 3.3 新特性全景解读', 'Spring Boot 3.3 带来了虚拟线程正式支持、CDS 优化、Observability 增强三大核心改进。本文从源码级别深入分析这些特性的实现原理，并结合实际项目给出迁移指南。', '["Spring Boot","Java","后端开发"]', '[]', '2026-05-01 09:00:00', '2026-05-01 09:00:00', '2026-05-01 09:00:00'),
(2, 1, 'published', 'article', 'public', FALSE, NULL, 'React 19 并发特性实战：useTransition 的正确用法', 'React 19 的并发渲染机制让 UI 响应速度有了质的飞跃。本文通过一个电商搜索场景，演示 useTransition 和 useDeferredValue 如何协同工作，以及常见的性能陷阱。', '["React","前端","JavaScript","Performance"]', '[]', '2026-05-02 10:30:00', '2026-05-02 10:30:00', '2026-05-02 10:30:00'),
(3, 1, 'published', 'article', 'public', FALSE, NULL, 'JWT 认证最佳实践：从理论到生产', '深入探讨 JWT 的签名算法选择、Token 刷新策略、黑名单机制、以及常见的 CSRF 和 XSS 防护方案。附 Spring Security OAuth2 完整配置示例。', '["Security","JWT","Spring Security","Auth"]', '[]', '2026-05-03 14:00:00', '2026-05-03 14:00:00', '2026-05-03 14:00:00'),
(4, 1, 'published', 'article', 'public', FALSE, NULL, 'MySQL 索引优化：从 Explain 到实际调优', '一条慢 SQL 的优化之旅。从 EXPLAIN 分析开始，到联合索引设计、索引下推、MRR 优化，最后性能提升 100 倍的完整过程。', '["MySQL","数据库","性能优化","SQL"]', '[]', '2026-05-04 16:30:00', '2026-05-04 16:30:00', '2026-05-04 16:30:00'),
(5, 1, 'published', 'article', 'public', FALSE, NULL, 'Redis 缓存策略全景：从 Cache-Aside 到 Write-Behind', '详解六种缓存模式（Cache-Aside、Read-Through、Write-Through、Write-Behind、Refresh-Ahead、Write-Around），以及缓存穿透、击穿、雪崩的防护方案。', '["Redis","缓存","分布式","System Design"]', '[]', '2026-05-05 08:00:00', '2026-05-05 08:00:00', '2026-05-05 08:00:00'),
(6, 1, 'published', 'article', 'public', FALSE, NULL, '从零构建 CI/CD 流水线：GitHub Actions 完整指南', '覆盖前端、后端、移动端的自动化构建、测试、部署流程。包括 Docker 镜像构建、K8s 部署、自动化版本发布和 Changelog 生成。', '["CI/CD","GitHub Actions","DevOps","Docker"]', '[]', '2026-05-06 11:00:00', '2026-05-06 11:00:00', '2026-05-06 11:00:00'),
(7, 2, 'published', 'article', 'public', TRUE, NULL, '大模型微调实战：LoRA + QLoRA 完整教程', '手把手教你用 LoRA 微调 Llama 3，从数据准备到模型部署。包含显存优化技巧、训练参数调优、以及使用 vLLM 部署推理服务的全流程。', '["AI","LLM","LoRA","Fine-tuning"]', '[]', '2026-05-01 10:00:00', '2026-05-01 10:00:00', '2026-05-01 10:00:00'),
(8, 2, 'published', 'article', 'public', FALSE, NULL, 'RAG 系统设计：从 Naive RAG 到 Advanced RAG', '详细介绍 RAG 的演进路线：Basic RAG → Sentence Window → Auto-Merging → Agentic RAG。附 LangChain + Milvus 完整实现。', '["AI","RAG","LangChain","Vector DB"]', '[]', '2026-05-02 13:00:00', '2026-05-02 13:00:00', '2026-05-02 13:00:00'),
(9, 2, 'published', 'article', 'public', FALSE, NULL, 'Kafka 从入门到精通：生产环境避坑指南', '分享在生产环境运维 Kafka 三年踩过的坑：分区重分配、消费者 Rebalance 优化、磁盘满了怎么办、ISR 机制深入理解。', '["Kafka","消息队列","后端","BigData"]', '[]', '2026-05-03 15:00:00', '2026-05-03 15:00:00', '2026-05-03 15:00:00'),
(10, 2, 'published', 'article', 'public', FALSE, NULL, 'Spark 性能调优：从 2 小时到 5 分钟的优化历程', '一个数据倾斜场景的完整优化记录：从数据源分析、Shuffle 调整、广播变量、到内存管理，每一步优化背后的原理。', '["Spark","BigData","性能优化","Distributed Computing"]', '[]', '2026-05-04 09:00:00', '2026-05-04 09:00:00', '2026-05-04 09:00:00'),
(11, 2, 'published', 'article', 'public', FALSE, NULL, 'MLOps 实践：模型版本管理与 AB 测试平台', '构建企业级 MLOps 平台的实战经验：模型注册中心、特征存储、自动化 AB 实验、模型监控与自动回滚。', '["MLOps","Model Management","AB Testing","Platform"]', '[]', '2026-05-05 16:00:00', '2026-05-05 16:00:00', '2026-05-05 16:00:00'),
(12, 2, 'published', 'article', 'public', FALSE, NULL, 'Python 异步编程深度解析：asyncio 源码剖析', '从生成器到协程，从事件循环到任务调度，从 Future 到 Task，逐层剖析 Python asyncio 的核心实现。', '["Python","Async","asyncio","Source Code"]', '[]', '2026-05-06 14:00:00', '2026-05-06 14:00:00', '2026-05-06 14:00:00'),
(13, 3, 'published', 'article', 'public', FALSE, NULL, 'CSS Container Queries 实战：组件级响应式设计', 'Container Queries 彻底改变了响应式设计思路。本文通过一个 Dashboard 案例，展示如何使用 Container Queries 构建真正可复用的组件。', '["CSS","Frontend","Responsive","Design Systems"]', '[]', '2026-05-01 11:00:00', '2026-05-01 11:00:00', '2026-05-01 11:00:00'),
(14, 3, 'published', 'article', 'public', FALSE, NULL, 'TypeScript 5.5 类型体操：从入门到放弃到精通', '一文掌握 TypeScript 高级类型：Conditional Types、Template Literal Types、Mapped Types 的进阶用法，用实际案例讲解类型推导。', '["TypeScript","Frontend","Type System"]', '[]', '2026-05-03 10:00:00', '2026-05-03 10:00:00', '2026-05-03 10:00:00'),
(15, 3, 'published', 'article', 'public', FALSE, NULL, '构建企业级设计系统：从 Figma 到代码', '分享在千人团队落地 Design System 的经验：Token 体系设计、组件 API 规范、多框架适配、以及自动化工具链。', '["Design Systems","Frontend","Figma","Component Library"]', '[]', '2026-05-05 15:00:00', '2026-05-05 15:00:00', '2026-05-05 15:00:00'),
(16, 3, 'published', 'article', 'public', FALSE, NULL, 'WebAssembly 实战：在浏览器中运行图像处理算法', '使用 Rust 编写图像滤镜算法，编译为 WASM 在浏览器中运行。性能对比：WASM vs 纯 JS vs Web Worker，附完整代码。', '["WebAssembly","Rust","Frontend","Performance"]', '[]', '2026-05-07 09:00:00', '2026-05-07 09:00:00', '2026-05-07 09:00:00'),
(17, 4, 'published', 'article', 'public', FALSE, NULL, 'Kubernetes Operator 开发入门：用 Go 编写你的第一个 Operator', '从 CRD 定义到 Controller 实现，手把手教你用 Kubebuilder 开发一个 Redis Cluster Operator。包含状态管理和故障恢复逻辑。', '["Kubernetes","Go","Operator","Cloud Native"]', '[]', '2026-05-02 08:00:00', '2026-05-02 08:00:00', '2026-05-02 08:00:00'),
(18, 4, 'published', 'article', 'public', FALSE, NULL, 'eBPF 入门与实践：零侵入的可观测性', 'eBPF 让内核级监控成为可能。本文通过 trace TCP 连接、分析系统调用延迟、监控容器网络三个实例，带你入门 eBPF 编程。', '["eBPF","Linux","Observability","Cloud Native"]', '[]', '2026-05-04 14:00:00', '2026-05-04 14:00:00', '2026-05-04 14:00:00'),
(19, 4, 'published', 'article', 'public', FALSE, NULL, 'Service Mesh 选型：Istio vs Linkerd vs Cilium', '从性能、架构复杂度、功能完整性、社区活跃度四个维度对比主流服务网格方案，附生产环境迁移建议。', '["Service Mesh","Istio","Cilium","Cloud Native"]', '[]', '2026-05-06 10:00:00', '2026-05-06 10:00:00', '2026-05-06 10:00:00'),
(20, 5, 'published', 'article', 'public', FALSE, NULL, 'PostgreSQL 查询优化器原理：从 Query Tree 到执行计划', '深入 pg 查询优化器源码：Query Rewrite → Path Generation → Cost Estimation → Plan Selection，每一步的算法和数据结构。', '["PostgreSQL","Database","Query Optimizer","Source Code"]', '[]', '2026-05-01 16:00:00', '2026-05-01 16:00:00', '2026-05-01 16:00:00'),
(21, 5, 'published', 'article', 'public', FALSE, NULL, '分布式事务：从 2PC 到 Saga 到 TCC', '对比三种分布式事务方案的原理、优缺点和适用场景。附 Seata 和 DTMP 的实战代码，以及最终一致性的监控方案。', '["Distributed Systems","Transaction","Seata","Architecture"]', '[]', '2026-05-03 12:00:00', '2026-05-03 12:00:00', '2026-05-03 12:00:00'),
(22, 5, 'published', 'article', 'public', FALSE, NULL, 'Rust 异步运行时深度对比：tokio vs async-std vs smol', '从 Reactor 模式、调度器设计、IO 模型、生态系统四个维度对比 Rust 三大异步运行时，帮你做出正确的技术选型。', '["Rust","Async Runtime","Systems Programming"]', '[]', '2026-05-06 08:00:00', '2026-05-06 08:00:00', '2026-05-06 08:00:00'),
(23, 6, 'published', 'article', 'public', FALSE, NULL, 'Flutter 状态管理终极指南：Provider → Riverpod → Bloc', '从简单到复杂，全面对比 Flutter 三大状态管理方案。每种方案的适用场景、性能考量、以及实际项目中的代码组织方式。', '["Flutter","Mobile","State Management","Dart"]', '[]', '2026-05-02 15:00:00', '2026-05-02 15:00:00', '2026-05-02 15:00:00'),
(24, 6, 'published', 'article', 'public', FALSE, NULL, 'iOS SwiftUI 性能优化：避免视图不必要的重绘', 'SwiftUI 的声明式语法容易写出性能陷阱。本文讲解 View Identity、@State vs @StateObject、EquatableView 的正确使用方式。', '["iOS","SwiftUI","Performance","Mobile"]', '[]', '2026-05-05 10:00:00', '2026-05-05 10:00:00', '2026-05-05 10:00:00'),
(25, 7, 'published', 'article', 'public', FALSE, NULL, 'OWASP Top 10 2026：Web 安全攻防实战', '逐条分析 OWASP Top 10 最新榜单：攻击原理、漏洞代码示例、以及修复方案。包括 Broken Access Control、Injection、SSRF 等。', '["Security","OWASP","Web Security","Best Practices"]', '[]', '2026-05-03 09:00:00', '2026-05-03 09:00:00', '2026-05-03 09:00:00'),
(26, 7, 'published', 'article', 'public', FALSE, NULL, '零信任架构落地实践：BeyondCorp 思想在企业中的应用', '传统边界安全已死。零信任架构的核心理念、技术组件（身份认证、设备信任、微隔离）、以及从传统 VPN 迁移到零信任的路线图。', '["Security","Zero Trust","Architecture","Enterprise"]', '[]', '2026-05-06 13:00:00', '2026-05-06 13:00:00', '2026-05-06 13:00:00'),
(27, 8, 'published', 'article', 'public', FALSE, NULL, '推荐系统从零搭建：召回 → 粗排 → 精排 → 重排', '工业级推荐系统的完整技术栈：多路召回策略、向量化召回、CTR 预估模型、多目标排序、以及实时特征工程。', '["Recommendation","Machine Learning","System Design","Algorithm"]', '[]', '2026-05-04 11:00:00', '2026-05-04 11:00:00', '2026-05-04 11:00:00'),
(28, 8, 'published', 'article', 'public', FALSE, NULL, 'Elasticsearch 搜索优化全攻略：从 IK 分词到相关性调优', 'ES 搜索质量的完整优化路径：IK 分词器自定义词典、同义词处理、BM25 参数调优、Function Score Query 以及搜索日志分析方法。', '["Elasticsearch","Search","Performance","Backend"]', '[]', '2026-05-07 14:00:00', '2026-05-07 14:00:00', '2026-05-07 14:00:00'),
(29, 9, 'published', 'article', 'public', FALSE, NULL, '从 IC 到 EM：技术管理转型的 10 个关键认知', '从一线工程师到管理 30 人团队的心路历程。聊一聊授权、1-on-1、绩效考核、技术决策、以及如何在管理和技术之间找到平衡。', '["Engineering Management","Career","Leadership","Team Building"]', '[]', '2026-05-03 08:00:00', '2026-05-03 08:00:00', '2026-05-03 08:00:00'),
(30, 10, 'published', 'article', 'public', FALSE, NULL, '独立开发者出海指南：从 Idea 到 MRR $10K', '分享我的 SaaS 出海历程：如何验证想法、选择技术栈、定价策略、SEO 优化、Stripe 集成、以及如何处理跨时区客户支持。', '["Indie Dev","SaaS","Startup","Business"]', '[]', '2026-05-05 07:00:00', '2026-05-05 07:00:00', '2026-05-05 07:00:00');

-- ==================== Follow Relations (32) ====================
INSERT INTO following (id, from_user_id, to_user_id, rel_status, created_at, updated_at) VALUES
(1,1,2,1,NOW(),NOW()),(2,2,1,1,NOW(),NOW()),
(3,1,3,1,NOW(),NOW()),(4,3,1,1,NOW(),NOW()),
(5,1,4,1,NOW(),NOW()),(6,1,5,1,NOW(),NOW()),
(7,2,3,1,NOW(),NOW()),(8,2,4,1,NOW(),NOW()), (9,2,8,1,NOW(),NOW()),
(10,3,2,1,NOW(),NOW()),(11,3,10,1,NOW(),NOW()),
(12,4,1,1,NOW(),NOW()),(13,4,5,1,NOW(),NOW()),(14,5,4,1,NOW(),NOW()),
(15,4,7,1,NOW(),NOW()),(16,5,1,1,NOW(),NOW()),
(17,5,8,1,NOW(),NOW()),(18,6,3,1,NOW(),NOW()), (19,6,10,1,NOW(),NOW()),
(20,7,1,1,NOW(),NOW()),(21,7,5,1,NOW(),NOW()),
(22,8,2,1,NOW(),NOW()),(23,8,5,1,NOW(),NOW()),
(24,9,1,1,NOW(),NOW()),(25,9,3,1,NOW(),NOW()), (26,9,6,1,NOW(),NOW()),
(27,10,3,1,NOW(),NOW()),(28,10,6,1,NOW(),NOW()), (29,10,9,1,NOW(),NOW());

INSERT INTO follower (id, to_user_id, from_user_id, rel_status, created_at, updated_at) VALUES
(1,2,1,1,NOW(),NOW()),(2,1,2,1,NOW(),NOW()),
(3,3,1,1,NOW(),NOW()),(4,1,3,1,NOW(),NOW()),
(5,4,1,1,NOW(),NOW()),(6,5,1,1,NOW(),NOW()),
(7,3,2,1,NOW(),NOW()),(8,4,2,1,NOW(),NOW()), (9,8,2,1,NOW(),NOW()),
(10,2,3,1,NOW(),NOW()),(11,10,3,1,NOW(),NOW()),
(12,1,4,1,NOW(),NOW()),(13,5,4,1,NOW(),NOW()),(14,4,5,1,NOW(),NOW()),
(15,7,4,1,NOW(),NOW()),(16,1,5,1,NOW(),NOW()),
(17,8,5,1,NOW(),NOW()),(18,3,6,1,NOW(),NOW()), (19,10,6,1,NOW(),NOW()),
(20,1,7,1,NOW(),NOW()),(21,5,7,1,NOW(),NOW()),
(22,2,8,1,NOW(),NOW()),(23,5,8,1,NOW(),NOW()),
(24,1,9,1,NOW(),NOW()),(25,3,9,1,NOW(),NOW()), (26,6,9,1,NOW(),NOW()),
(27,3,10,1,NOW(),NOW()),(28,6,10,1,NOW(),NOW()), (29,9,10,1,NOW(),NOW());
