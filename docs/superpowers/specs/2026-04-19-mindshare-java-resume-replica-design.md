# MindShare Java Resume Replica Design

## Background

`mindshare` is a Java reimplementation project placed under `D:\dev\my_proj\java\mindshare`.
The target is to recreate the core experience and engineering style of the existing ZhiGuang Java backend, while keeping the first phase constrained to a resume-friendly scope.

This design follows the user's chosen constraints:

- Replica style: stay close to the original project's API shape, table naming, package split, and module naming
- Scope: `auth`, `profile`, `storage`, `knowpost`, `feed`, `search`
- Infra: `MySQL + Redis + OSS + Elasticsearch`
- Excluded from phase one: `counter`, `relation`, `Kafka`, `Canal`, `Outbox`, `RAG`

## Goal

Build a standalone Spring Boot backend named `mindshare` that:

- looks structurally close to the original Java project
- can run the main publishing and retrieval flows end to end
- is strong enough for resume usage and interview explanation
- leaves clean extension points for `counter`, `relation`, and `rag` in phase two

## Non-Goals

Phase one deliberately does not include:

- distributed event-driven consistency
- high-concurrency bitmap/Lua counter system
- follow/follower graph and fan-out style read paths
- AI summary generation and RAG QA
- Canal/Kafka-based search sync

Those systems should not be half-implemented in phase one. The code should instead expose clear boundaries so they can be added later without large rewrites.

## Recommended Architecture

`mindshare` should be implemented as a single Spring Boot 3 monolith using Java 21 and MyBatis, mirroring the original repository's organization closely enough that the migration path stays obvious.

Suggested top-level Java packages:

- `com.mindshare.auth`
- `com.mindshare.profile`
- `com.mindshare.storage`
- `com.mindshare.knowpost`
- `com.mindshare.search`
- `com.mindshare.user`
- `com.mindshare.common`
- `com.mindshare.config`

Recommended runtime components:

- MySQL for users, logs, and knowpost data
- Redis for verification codes, refresh token whitelist, and phase-one cache
- OSS for direct upload via presigned PUT
- Elasticsearch for content search and suggest

Phase-one sync model:

- profile and knowpost writes go directly to MySQL
- publish/update/delete flows update Elasticsearch synchronously inside application service methods
- feed/detail cache invalidation happens in-process and via Redis deletes, not via MQ

This keeps the system simple enough to finish while still preserving the same business shape as the source project.

## Module Boundaries

### 1. Auth

Responsibilities:

- send verification code
- register with email or phone
- login by password or code
- refresh token
- logout
- password reset
- query current user

Design choices:

- keep JWT dual-token flow
- keep access token short-lived and refresh token long-lived
- use Redis refresh-token whitelist
- use Spring Security resource server style bearer auth

Phase-one simplification:

- use RS256 if practical, otherwise allow an initial HS256 bootstrap only if schedule risk becomes high
- verification sender can start as logging sender

### 2. Profile

Responsibilities:

- get current profile
- patch profile fields
- upload avatar metadata via profile update

Boundary:

- profile owns user-facing editable fields
- auth owns registration/login state

### 3. Storage

Responsibilities:

- generate presigned PUT URLs for knowpost content and images
- validate post ownership before issuing upload URLs

Boundary:

- storage does not own publish workflow
- knowpost confirms uploaded content after client upload succeeds

### 4. KnowPost

Responsibilities:

- create draft
- confirm uploaded content
- patch metadata
- publish
- soft delete
- patch top flag
- patch visibility
- get detail
- list public feed
- list current user's published posts

Phase-one simplification:

- no outbox
- no RAG pre-index hook
- no counter aggregation dependency

### 5. Feed

Responsibilities:

- public feed listing
- my published feed listing
- knowpost detail cache

Phase-one cache policy:

- keep a lighter version of the source design
- use `Caffeine + Redis` for page/detail caching
- keep invalidation explicit from knowpost service methods
- skip hotkey detector and single-flight in the first delivery unless time remains

### 6. Search

Responsibilities:

- create search index on startup if missing
- sync post documents on publish/update/delete
- keyword search
- prefix suggest

Phase-one simplification:

- synchronous index update from application service
- no outbox consumer
- no function_score dependency on external counter service; ranking can use stored fields such as publish time and static interaction placeholders

## Data Model

Minimum phase-one tables in MySQL:

- `users`
- `login_logs`
- `know_posts`

Optional but useful early:

- `schema_version` if you want lightweight migration tracking

Recommended `users` fields:

- `id`
- `phone`
- `email`
- `password_hash`
- `nickname`
- `avatar`
- `bio`
- `tags_json`
- `birthday`
- `school`
- `gender`
- `create_time`
- `update_time`

Recommended `login_logs` fields:

- `id`
- `user_id`
- `identifier`
- `channel`
- `ip`
- `user_agent`
- `result`
- `create_time`

Recommended `know_posts` fields:

- `id`
- `creator_id`
- `status`
- `type`
- `visible`
- `is_top`
- `title`
- `description`
- `tag_id`
- `tags`
- `img_urls`
- `content_object_key`
- `content_etag`
- `content_size`
- `content_sha256`
- `content_url`
- `publish_time`
- `create_time`
- `update_time`

Recommended Elasticsearch document fields:

- `content_id`
- `title`
- `description`
- `body`
- `tags`
- `img_urls`
- `author_avatar`
- `author_nickname`
- `author_tag_json`
- `status`
- `publish_time`
- `title_suggest`

## API Strategy

Use source-compatible naming as much as possible for easier replication and future comparison.

Recommended phase-one API groups:

- `/api/v1/auth/*`
- `/api/v1/profile`
- `/api/v1/storage/presign`
- `/api/v1/knowposts/*`
- `/api/v1/search/*`

Important replica rule:

- keep DTO naming and route semantics close to the source project
- rename only package prefix, artifact name, docs name, index name, and business branding where needed

## Error Handling

Use a shared business exception model similar to the source project:

- `BusinessException`
- `ErrorCode`
- `GlobalExceptionHandler`

This keeps controller code thin and lets the future second-phase modules integrate without response-shape rewrites.

## Testing Strategy

Phase-one testing should be intentionally stronger than the source project in order to make `mindshare` easier to demonstrate as your own polished replica.

Recommended minimum:

- unit tests for JWT service, verification logic, search cursor codec, storage presign validation
- service tests for auth, knowpost, and search sync logic
- controller tests for auth and knowpost core routes

## Delivery Strategy

Build in six milestones:

1. Project bootstrap and shared infrastructure
2. Auth and user domain
3. Profile and storage
4. Knowpost draft-to-publish flow
5. Feed and cache
6. Search and docs cleanup

Each milestone should end in a runnable state and map to a small batch of focused commits.

## Commit Strategy

Commit granularity should mimic the source project's module-oriented evolution rather than giant weekly dumps.

Recommended commit style:

- `chore: bootstrap mindshare spring backend`
- `feat(auth): add jwt token issuing and refresh flow`
- `feat(auth): add verification code login and reset password`
- `feat(profile): add profile query and patch api`
- `feat(storage): add oss presign upload api`
- `feat(knowpost): add draft and content confirm flow`
- `feat(knowpost): add metadata patch and publish flow`
- `feat(feed): add public feed and mine feed cache`
- `feat(search): add elasticsearch index sync and search api`
- `test: add auth knowpost and search coverage`
- `docs: add local setup and api notes`

The key rule is one commit per meaningful capability or one tightly-coupled mechanism. Avoid mixed commits that combine auth, search, and cache changes together.

## Risks and Tradeoffs

Main risks:

- over-copying the source project too literally and losing ownership clarity
- underbuilding tests and ending up with another hard-to-maintain clone
- introducing Elasticsearch too early without keeping a small, stable document schema

Chosen tradeoff:

- stay visually and structurally close to the source project
- simplify distributed consistency systems
- invest the saved time into clearer tests, docs, and cleaner boundaries

## Success Criteria

`mindshare` phase one is complete when:

- the project boots locally with MySQL, Redis, OSS config, and Elasticsearch
- auth/profile/storage/knowpost/feed/search APIs are callable end to end
- publish/update/delete can keep ES in sync without manual intervention
- the repository includes enough docs and tests that another engineer can run it without reverse engineering
