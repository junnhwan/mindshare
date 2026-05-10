-- Seed data: 2 test users + 5 posts + relations + likes/favs
-- Passwords are BCrypt of "Test1234"

-- Users
INSERT INTO users (id, phone, email, password_hash, nickname, avatar, bio, zg_id, tags_json, birthday, school, gender)
VALUES
(1, '13800000001', 'alice@test.com',
 '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K',
 'Alice', '', '全栈工程师，热爱技术分享', 'alice01',
 '["Java","Spring","React"]', '1998-05-20', '浙江大学', 'FEMALE'),
(2, '13800000002', 'bob@test.com',
 '$2a$12$Nm0e8ESoeLGMrKg/32k7yOFDKfiyzw7eRWs4HeqPl7wDdIgu4BO0K',
 'Bob', '', '深耕AI与大数据领域', 'bob_ai',
 '["AI","BigData","Python"]', '1997-10-15', '北京大学', 'MALE');

-- KnowPosts
INSERT INTO know_posts (id, creator_id, status, type, visible, is_top, tag_id, title, description, tags, img_urls, publish_time, create_time, update_time)
VALUES
(1, 1, 'published', 'article', 'public', TRUE, NULL,
 'Spring Boot 3.3 新特性详解',
 '深入探讨 Spring Boot 3.3 的核心改进：虚拟线程支持、CDS 优化、Observability 增强',
 '["Spring Boot","Java","后端开发"]',
 '[]', NOW(), NOW(), NOW()),

(2, 1, 'published', 'article', 'public', FALSE, NULL,
 'React 19 并发特性实践指南',
 'React 19 带来了全新的并发渲染机制，本文通过实际案例讲解如何使用 useTransition 和 useDeferredValue',
 '["React","前端","JavaScript"]',
 '[]', NOW(), NOW(), NOW()),

(3, 2, 'published', 'article', 'public', FALSE, NULL,
 '大模型微调实战：从零搭建 RAG 系统',
 '详细介绍 Retrieval-Augmented Generation 的原理与实现，使用 LangChain 和向量数据库',
 '["AI","LLM","RAG","LangChain"]',
 '[]', NOW(), NOW(), NOW()),

(4, 2, 'published', 'article', 'public', FALSE, NULL,
 '深入理解 Elasticsearch 8.x 搜索优化',
 'ES 8.x 的 IK 分词器优化、倒排索引原理、查询性能调优全攻略',
 '["Elasticsearch","搜索引擎","后端"]',
 '[]', NOW(), NOW(), NOW()),

(5, 1, 'published', 'article', 'public', FALSE, NULL,
 '数据库索引设计最佳实践',
 'MySQL 联合索引、覆盖索引、索引下推等核心概念的深度解析与实战',
 '["MySQL","数据库","性能优化"]',
 '[]', NOW(), NOW(), NOW());

-- Relations: Alice follows Bob, Bob follows Alice (mutual)
INSERT INTO following (id, from_user_id, to_user_id, rel_status, created_at, updated_at)
VALUES (1, 1, 2, 1, NOW(), NOW()),
       (2, 2, 1, 1, NOW(), NOW());

INSERT INTO follower (id, to_user_id, from_user_id, rel_status, created_at, updated_at)
VALUES (1, 2, 1, 1, NOW(), NOW()),
       (2, 1, 2, 1, NOW(), NOW());
