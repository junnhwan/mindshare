# MindShare API 接口文档

Base: `/api/v1`  ·  认证: `Authorization: Bearer <access_token>`

## 认证 (Auth)

### `POST /api/v1/auth/send-code`  — 发送验证码

公开接口。

请求：
- `identifierType`: `PHONE` | `EMAIL`
- `identifier`: 手机号或邮箱
- `scene`: `REGISTER` | `LOGIN` | `RESET_PASSWORD`

限频：同标识符 60 秒内不可重复发送，单日上限 10 次。

### `POST /api/v1/auth/register`  — 注册

请求：
- `identifierType`, `identifier`, `code`: 验证码
- `password`: 密码（≥8 位，含字母+数字）
- `nickname`: 可选

返回：`{ user: {...}, token: { accessToken, refreshToken, tokenType, expiresIn } }`

### `POST /api/v1/auth/login`  — 登录

支持密码登录或验证码登录（二选一）。

请求：
- `identifierType`, `identifier`
- `password` 或 `code`

返回：同注册。

### `POST /api/v1/auth/token/refresh`  — 刷新令牌

请求：`{ refreshToken }`

返回：新的 Token 对。

### `POST /api/v1/auth/logout`  — 登出

请求：`{ refreshToken }`

### `POST /api/v1/auth/password/reset`  — 重置密码

请求：`{ identifierType, identifier, code, newPassword }`

### `GET /api/v1/auth/me`  — 当前用户

需认证。返回完整用户信息：id, nickname, avatar, phone, email, zgId, gender, birthday, school, bio, tagsJson。

---

## 用户资料 (Profile)

### `GET /api/v1/profile`  — 查看资料

需认证。

### `PATCH /api/v1/profile`  — 修改资料

需认证。可修改字段：nickname, bio, gender, birthday, school, tagJson, avatar。

### `POST /api/v1/profile/avatar`  — 上传头像

需认证。multipart/form-data，字段名 `file`。

---

## 内容发布 (KnowPost)

### `POST /api/v1/knowposts/drafts`  — 创建草稿

需认证。返回 `{ id }`。

### `POST /api/v1/knowposts/{id}/content/confirm`  — 确认上传内容

需认证。请求：
- `objectKey`: OSS 对象 Key
- `etag`, `size`, `sha256`: 文件校验信息

### `PATCH /api/v1/knowposts/{id}`  — 编辑元数据

需认证。可修改：title, tagId, tags, imgUrls, visible, isTop, description。

### `POST /api/v1/knowposts/{id}/publish`  — 发布

需认证。草稿状态 → 已发布。同时写入 outbox 事件触发异步索引。

### `PATCH /api/v1/knowposts/{id}/top`  — 切换置顶

需认证。

### `PATCH /api/v1/knowposts/{id}/visibility`  — 修改可见性

需认证。可选值：public, followers, school, private, unlisted。

### `DELETE /api/v1/knowposts/{id}`  — 删除

需认证。软删除。

### `GET /api/v1/knowposts/feed`  — 公开信息流

公开接口。参数：page, size。返回：帖子列表（含 likeCount, favoriteCount, liked, faved）。

### `GET /api/v1/knowposts/mine`  — 我的发布

需认证。参数：page, size。

### `GET /api/v1/knowposts/detail/{id}`  — 帖子详情

公开（已发布+公开）或作者可见。返回完整内容含计数。

### `POST /api/v1/knowposts/description/suggest`  — 描述建议

需认证。当前为截断实现，后续接入 LLM。

---

## 互动计数 (Counter)

### `POST /api/v1/action/like`  — 点赞

需认证。请求：`{ entityType, entityId }`。支持实体类型：`knowpost`。

幂等：同一用户重复点赞不产生副作用。

### `POST /api/v1/action/unlike`  — 取消点赞

需认证。

### `POST /api/v1/action/fav`  — 收藏

需认证。幂等。

### `POST /api/v1/action/unfav`  — 取消收藏

需认证。

### `GET /api/v1/counter/{etype}/{eid}`  — 查询计数

公开接口。参数：`?metrics=like,fav`。返回指定实体的各指标计数。

---

## 搜索 (Search)

### `GET /api/v1/search`  — 全文搜索

公开接口。参数：
- `q`: 搜索关键词
- `size`: 页大小（≤50）
- `tags`: 标签过滤，逗号分隔
- `after`: 游标 token，用于翻页

只索引已发布且公开的内容。结果按相关性 + 互动计数排序，含正文命中片段高亮。

### `GET /api/v1/search/suggest`  — 搜索建议

公开接口。参数：prefix, size。返回标题补全建议。

---

## 社交关系 (Relation)

### `POST /api/v1/relation/follow`  — 关注

需认证。参数：`?toUserId=`。

### `POST /api/v1/relation/unfollow`  — 取关

需认证。参数：`?toUserId=`。

### `GET /api/v1/relation/status`  — 关系状态

需认证。参数：`?userId=`。返回：`self` / `following` / `followedBy` / `mutual` / `none`。

### `GET /api/v1/relation/following`  — 关注列表

需认证。参数：userId（可选，默认自己）, page, size。返回含用户资料的列表。

### `GET /api/v1/relation/followers`  — 粉丝列表

需认证。参数同上。

### `GET /api/v1/relation/counter`  — 关注/粉丝计数

公开接口。参数：`?userId=`。返回 `{ following, followers }`。

---

## 对象存储 (Storage)

### `POST /api/v1/storage/presign`  — 生成预签名上传 URL

需认证。请求：`{ postId, scene, contentType, ext? }`。

场景：
- `knowpost_content`: 正文文件（.md / .html / .txt / .json）
- `knowpost_image`: 图片文件（.jpg / .png / .webp / .gif）

校验：请求者必须是目标帖子的创建者。

---

## 错误响应格式

所有错误统一返回 HTTP 400（业务异常）或 500（服务异常），响应体：

```json
{ "code": "IDENTIFIER_EXISTS", "message": "账号已存在" }
```

错误码：`IDENTIFIER_EXISTS`, `IDENTIFIER_NOT_FOUND`, `ZGID_EXISTS`, `VERIFICATION_RATE_LIMIT`, `VERIFICATION_DAILY_LIMIT`, `VERIFICATION_NOT_FOUND`, `VERIFICATION_MISMATCH`, `VERIFICATION_TOO_MANY_ATTEMPTS`, `INVALID_CREDENTIALS`, `PASSWORD_POLICY_VIOLATION`, `TERMS_NOT_ACCEPTED`, `REFRESH_TOKEN_INVALID`, `BAD_REQUEST`, `INTERNAL_ERROR`。

---

## 架构说明

- **计数系统**: Redis Bitmap 幂等存储 → Kafka 异步聚合 → Redis SDS 紧凑计数 → Redisson 分布式锁重建
- **缓存策略**: Caffeine L1 + Redis L2 + HotKey 热点探测动态 TTL + SingleFlight 防击穿 + 定向失效
- **搜索链路**: 发布时写入 Outbox → 定时轮询消费 → ES 索引更新，最终一致
- **社交关系**: 双表（following / follower）+ 用户维度计数器联动
