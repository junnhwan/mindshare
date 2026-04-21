# MindShare

`mindshare` is a Spring Boot 3 / Java 21 backend replica that is being aligned
step by step with the reference project `zhiguang_be`.

Current implemented scope:

- `auth`
- `profile`
- `storage`
- `knowpost`
- `feed`
- `search`

Current not-yet-implemented full-replica modules:

- `counter`
- `relation`
- `outbox + kafka + canal`
- `llm / rag`

## Current Phase-One Status

The repository is beyond bootstrap and already contains a runnable phase-one
backend with these behaviors:

- auth: send code, register, login, refresh token, logout, reset password, `/me`
- profile: current profile query, patch profile, avatar upload
- storage: knowpost ownership-guarded OSS presign upload
- knowpost: draft create, content confirm, metadata patch, publish, delete, detail
- feed: public feed, mine feed, detail cache, targeted public-feed invalidation
- search: published/public-only indexing, real content body indexing, cursor-style paging, suggest

Recent alignment work against the reference project:

- search body loading was extracted to `KnowPostContentLoader`
- search pagination was upgraded from offset-like tokens to sort-cursor tokens
- body matches now return a snippet instead of only the stored description
- profile now supports `POST /api/v1/profile/avatar`
- public feed invalidation is no longer always full-cache flush

## Repo Docs

- phase-one API summary: `docs/api-phase1.md`
- local runbook: `docs/runbook-local.md`
- reference-project gap analysis: `docs/2026-04-21-mindshare-vs-zhiguang-gap-analysis.md`
- full replica design: `docs/superpowers/specs/2026-04-19-mindshare-full-replica-design.md`
- full replica execution plan: `docs/superpowers/plans/2026-04-19-mindshare-full-replica-plan.md`

## Local Development

### 1. Requirements

- Java 21
- Maven 3.9+
- MySQL 8+ for app runtime
- Redis optional
- Elasticsearch optional

Notes:

- Tests use H2 and disable Redis/Elasticsearch by default.
- Runtime can start without Redis/Elasticsearch if you explicitly disable them.
- Search still has an in-memory fallback when Elasticsearch is unavailable.

### 2. Main Config

The main runtime config lives in `src/main/resources/application.yml`.

Important switches:

- `mindshare.cache.redis-enabled`
- `mindshare.elasticsearch.enabled`
- `oss.*`
- `spring.datasource.*`
- `auth.jwt.*`

### 3. Run Tests

Run the main phase-one smoke suite:

```powershell
mvn -q "-Dtest=SearchServiceTest,SearchControllerTest,ProfileControllerTest,KnowPostFeedServiceTest,KnowPostServiceTest" test
```

Run the full test suite:

```powershell
mvn test
```

### 4. Start the App

If MySQL is ready and you want a local runtime with Redis/ES disabled:

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DB="mindshare"
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="root"
$env:SPRING_PROFILES_ACTIVE=""
$env:MINDSHARE_CACHE_REDIS_ENABLED="false"
$env:MINDSHARE_ELASTICSEARCH_ENABLED="false"
mvn spring-boot:run
```

If you prefer YAML over env vars, set:

- `mindshare.cache.redis-enabled: false`
- `mindshare.elasticsearch.enabled: false`

## Database

Current schema file:

- `db/schema.sql`

Current main tables:

- `users`
- `login_logs`
- `know_posts`

Planned later tables for full alignment:

- `following`
- `follower`
- `outbox`

## Implementation Notes

- naming stays `mindshare`; we do not rename packages back to `tongji/zhiguang`
- phase-one still returns placeholder interaction counts because `counter` is not built yet
- `description/suggest` is still a lightweight placeholder and not an LLM-backed feature yet
- search ranking is closer to the reference project now, but still does not use real counter signals

## Next Alignment Targets

Recommended next order after the current phase-one improvements:

1. `counter`
2. `relation + outbox + kafka + canal`
3. `llm / rag`
