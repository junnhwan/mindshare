# MindShare Local Runbook

This runbook is for local development of the current phase-one-plus state.

## 1. Toolchain

- Java 21
- Maven 3.9+
- PowerShell on Windows is fine

## 2. Infrastructure Matrix

### Minimal test-only mode

Needed:

- nothing external beyond Java and Maven

Because:

- tests use H2
- test profile disables Redis
- test profile disables Elasticsearch

Command:

```powershell
mvn test
```

### Minimal local app runtime

Needed:

- MySQL

Optional:

- Redis
- Elasticsearch

Recommended for the simplest runtime:

- keep Redis disabled
- keep Elasticsearch disabled
- rely on search in-memory fallback

## 3. Runtime Config Checklist

Main config file:

- `src/main/resources/application.yml`

Useful environment variables:

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DB`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `ES_URIS`
- `ES_INDEX_NAME`
- `OSS_ENDPOINT`
- `OSS_BUCKET`
- `OSS_ACCESS_KEY_ID`
- `OSS_ACCESS_KEY_SECRET`
- `AUTH_PUBLIC_KEY_LOCATION`
- `AUTH_PRIVATE_KEY_LOCATION`

Feature toggles:

- `MINDSHARE_CACHE_REDIS_ENABLED=false`
- `MINDSHARE_ELASTICSEARCH_ENABLED=false`

## 4. MySQL Boot

Create database:

```sql
CREATE DATABASE mindshare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The app initializes schema from:

- `db/schema.sql`

## 5. Start Commands

### Run tests

```powershell
mvn test
```

### Run the focused phase-one smoke suite

```powershell
mvn -q "-Dtest=SearchServiceTest,SearchControllerTest,ProfileControllerTest,KnowPostFeedServiceTest,KnowPostServiceTest" test
```

### Run the app

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DB="mindshare"
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="root"
$env:MINDSHARE_CACHE_REDIS_ENABLED="false"
$env:MINDSHARE_ELASTICSEARCH_ENABLED="false"
mvn spring-boot:run
```

## 6. Recommended Validation Flow

1. Run `mvn test`
2. Start the app with Redis/ES disabled first
3. Verify auth login/register flow
4. Verify knowpost draft -> confirm -> patch -> publish flow
5. Verify search can hit published content
6. If needed, enable Redis and Elasticsearch one by one

## 7. Optional Elasticsearch Mode

If enabling Elasticsearch:

- set `mindshare.elasticsearch.enabled=true`
- ensure `spring.elasticsearch.uris` points to a reachable node
- keep index name aligned with `mindshare.elasticsearch.index-name`

Current search behavior if ES is unavailable:

- index writes degrade silently
- in-memory search fallback remains available in the app process

## 8. Optional Redis Mode

If enabling Redis:

- set `mindshare.cache.redis-enabled=true`
- ensure `spring.data.redis.*` is reachable

Current cache usage:

- public feed page cache
- mine feed page cache
- knowpost detail cache

## 9. Known Missing Pieces

Still not available in local runtime:

- counter Kafka aggregation / rebuild chain
- user-dimension counters
- relation APIs
- outbox/kafka/canal pipeline
- LLM description generation
- RAG indexing and SSE QA APIs

## 10. Reference Alignment Docs

- `docs/2026-04-21-mindshare-vs-zhiguang-gap-analysis.md`
- `docs/superpowers/specs/2026-04-19-mindshare-full-replica-design.md`
- `docs/superpowers/plans/2026-04-19-mindshare-full-replica-plan.md`
