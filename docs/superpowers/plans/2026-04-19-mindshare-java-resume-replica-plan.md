# MindShare Java Resume Replica Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a resume-friendly Java replica of the ZhiGuang backend in `D:\dev\my_proj\java\mindshare`, covering `auth`, `profile`, `storage`, `knowpost`, `feed`, and `search`.

**Architecture:** Implement a Spring Boot 3 monolith with Java 21, MyBatis, MySQL, Redis, OSS, and Elasticsearch. Keep API shapes and package boundaries close to the source project, but replace distributed event-driven pieces with synchronous application-service flows in phase one.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, MyBatis, MySQL, Redis, Caffeine, Elasticsearch Java client, Aliyun OSS SDK, JUnit 5, Mockito

---

## File Structure Target

### Root

- Create: `D:\dev\my_proj\java\mindshare\pom.xml`
- Create: `D:\dev\my_proj\java\mindshare\.gitignore`
- Create: `D:\dev\my_proj\java\mindshare\README.md`
- Create: `D:\dev\my_proj\java\mindshare\db\schema.sql`

### Main Java

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\MindShareApplication.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\common\exception\BusinessException.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\common\exception\ErrorCode.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\common\web\GlobalExceptionHandler.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\config\*`

### Resources

- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\application.yml`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\mapper\*.xml`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\keys\public.pem`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\keys\private.pem`

### Test

- Create: `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\...`

## Task 0: Initialize Repository Skeleton

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\pom.xml`
- Create: `D:\dev\my_proj\java\mindshare\.gitignore`
- Create: `D:\dev\my_proj\java\mindshare\README.md`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\MindShareApplication.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\application.yml`
- Create: `D:\dev\my_proj\java\mindshare\db\schema.sql`

- [ ] **Step 1: Initialize git repository if still absent**

Run:

```powershell
git -C D:\dev\my_proj\java\mindshare init
```

Expected: repository initialized under `.git`

- [ ] **Step 2: Write the failing bootstrap test**

Create `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\MindShareApplicationTest.java`

```java
@SpringBootTest
class MindShareApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 3: Run test to verify it fails before scaffold is complete**

Run:

```powershell
mvn -q -Dtest=MindShareApplicationTest test
```

Expected: fail because `pom.xml` and Spring Boot bootstrap are not ready

- [ ] **Step 4: Add minimal Spring Boot scaffold**

Include in `pom.xml`:

- Spring Boot parent
- Java 21
- web, validation, jdbc, mybatis, mysql, security, oauth2-resource-server, redis, caffeine, elasticsearch, mail, oss, test dependencies

Include in `application.yml`:

- server port
- datasource
- redis
- mybatis mapper-locations
- auth JWT TTLs
- oss settings
- elasticsearch settings

- [ ] **Step 5: Run bootstrap test**

Run:

```powershell
mvn -q -Dtest=MindShareApplicationTest test
```

Expected: PASS

- [ ] **Step 6: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "chore: bootstrap mindshare spring backend"
```

## Task 1: Add Shared Error, Config, and Security Infrastructure

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\common\exception\ErrorCode.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\common\exception\BusinessException.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\common\web\GlobalExceptionHandler.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\config\AuthProperties.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\config\PemUtils.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\config\SecurityConfig.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\config\RedissonConfig.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\config\ElasticsearchConfig.java`

- [ ] **Step 1: Write failing tests for error response shape and JWT bean loading**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\common\web\GlobalExceptionHandlerTest.java`
- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\auth\config\SecurityConfigTest.java`

Example assertion:

```java
assertThat(responseJson).contains("code");
assertThat(responseJson).contains("message");
```

- [ ] **Step 2: Run targeted tests**

```powershell
mvn -q -Dtest=GlobalExceptionHandlerTest,SecurityConfigTest test
```

Expected: FAIL because handlers and beans do not yet exist

- [ ] **Step 3: Implement shared infrastructure**

Make sure:

- business exceptions map to stable JSON response
- unauthorized and forbidden responses are clean
- RSA key loading works from `resources/keys`
- Redis and Elasticsearch clients are centrally configured

- [ ] **Step 4: Re-run targeted tests**

```powershell
mvn -q -Dtest=GlobalExceptionHandlerTest,SecurityConfigTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(core): add shared error handling and security infrastructure"
```

## Task 2: Add User, Login Log, and Auth Persistence Layer

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\user\domain\User.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\user\mapper\UserMapper.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\mapper\UserMapper.xml`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\audit\LoginLog.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\audit\LoginLogMapper.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\mapper\LoginLogMapper.xml`
- Modify: `D:\dev\my_proj\java\mindshare\db\schema.sql`

- [ ] **Step 1: Write repository-level tests for user create/query and login log insert**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\user\mapper\UserMapperTest.java`
- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\auth\audit\LoginLogMapperTest.java`

- [ ] **Step 2: Run mapper tests**

```powershell
mvn -q -Dtest=UserMapperTest,LoginLogMapperTest test
```

Expected: FAIL before mapper interfaces and XML are complete

- [ ] **Step 3: Implement MyBatis mappings and schema**

Schema should include:

- `users`
- `login_logs`

Mapper methods should support:

- create user
- query by phone
- query by email
- query by id
- update password
- update profile fields
- insert login log

- [ ] **Step 4: Re-run mapper tests**

```powershell
mvn -q -Dtest=UserMapperTest,LoginLogMapperTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(user): add user and login log persistence"
```

## Task 3: Implement JWT, Refresh Token Store, and Verification Services

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\token\TokenPair.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\token\JwtService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\token\RefreshTokenStore.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\token\RedisRefreshTokenStore.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\verification\VerificationService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\verification\RedisVerificationCodeStore.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\verification\LoggingCodeSender.java`

- [ ] **Step 1: Write failing unit tests for JWT issue/parse and verification attempt limits**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\auth\token\JwtServiceTest.java`
- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\auth\verification\VerificationServiceTest.java`

Example JWT assertion:

```java
TokenPair pair = jwtService.issueTokenPair(user);
assertThat(pair.accessToken()).isNotBlank();
```

- [ ] **Step 2: Run targeted tests**

```powershell
mvn -q -Dtest=JwtServiceTest,VerificationServiceTest test
```

Expected: FAIL

- [ ] **Step 3: Implement token and verification services**

Make sure:

- access and refresh token have different claims
- refresh tokens are stored in Redis by whitelist key
- verification supports send and verify with expiry and attempt limit
- sender logs code instead of sending real SMS/email in phase one

- [ ] **Step 4: Re-run targeted tests**

```powershell
mvn -q -Dtest=JwtServiceTest,VerificationServiceTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(auth): add jwt token and verification services"
```

## Task 4: Implement Auth API End to End

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\service\AuthService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\api\AuthController.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\api\dto\*.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\auth\model\*.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\user\service\UserService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\user\service\impl\UserServiceImpl.java`

- [ ] **Step 1: Write failing controller tests for send-code, register, login, refresh, logout, reset, and me**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\auth\api\AuthControllerTest.java`

Expected scenarios:

- register success
- login by password success
- refresh invalid token rejected
- me without token unauthorized

- [ ] **Step 2: Run controller tests**

```powershell
mvn -q -Dtest=AuthControllerTest test
```

Expected: FAIL

- [ ] **Step 3: Implement auth service and controller**

Keep route shape close to source project:

- `POST /api/v1/auth/send-code`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/token/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/password/reset`
- `GET /api/v1/auth/me`

- [ ] **Step 4: Re-run controller tests**

```powershell
mvn -q -Dtest=AuthControllerTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(auth): add auth api flow"
```

## Task 5: Implement Profile Module

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\profile\service\ProfileService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\profile\service\impl\ProfileServiceImpl.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\profile\api\ProfileController.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\profile\api\dto\*.java`

- [ ] **Step 1: Write failing tests for profile get and patch**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\profile\api\ProfileControllerTest.java`

- [ ] **Step 2: Run tests**

```powershell
mvn -q -Dtest=ProfileControllerTest test
```

Expected: FAIL

- [ ] **Step 3: Implement profile service and controller**

Routes:

- `GET /api/v1/profile`
- `PATCH /api/v1/profile`

- [ ] **Step 4: Re-run tests**

```powershell
mvn -q -Dtest=ProfileControllerTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(profile): add profile query and patch api"
```

## Task 6: Implement OSS Storage Presign API

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\storage\config\OssProperties.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\storage\OssStorageService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\storage\api\StorageController.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\storage\api\dto\*.java`

- [ ] **Step 1: Write failing service test for presign ownership validation and ext/content-type rules**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\storage\api\StorageControllerTest.java`

- [ ] **Step 2: Run tests**

```powershell
mvn -q -Dtest=StorageControllerTest test
```

Expected: FAIL

- [ ] **Step 3: Implement storage service and controller**

Route:

- `POST /api/v1/storage/presign`

Rules:

- validate current user owns the target post
- support `knowpost_content` and `knowpost_image`
- return `objectKey`, `putUrl`, `headers`, `expiresIn`

- [ ] **Step 4: Re-run tests**

```powershell
mvn -q -Dtest=StorageControllerTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(storage): add oss presign upload api"
```

## Task 7: Implement KnowPost Persistence and Draft-to-Publish Flow

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\model\KnowPost.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\model\KnowPostDetailRow.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\model\KnowPostFeedRow.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\mapper\KnowPostMapper.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\resources\mapper\KnowPostMapper.xml`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\service\KnowPostService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\service\impl\KnowPostServiceImpl.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\api\KnowPostController.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\api\dto\*.java`
- Modify: `D:\dev\my_proj\java\mindshare\db\schema.sql`

- [ ] **Step 1: Write failing tests for draft creation, content confirm, metadata patch, publish, delete, and detail**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\knowpost\service\KnowPostServiceTest.java`
- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\knowpost\api\KnowPostControllerTest.java`

- [ ] **Step 2: Run tests**

```powershell
mvn -q -Dtest=KnowPostServiceTest,KnowPostControllerTest test
```

Expected: FAIL

- [ ] **Step 3: Implement knowpost schema, mapper, service, and controller**

Routes:

- `GET /api/v1/knowposts/feed`
- `GET /api/v1/knowposts/mine`
- `GET /api/v1/knowposts/detail/{id}` or source-compatible equivalent
- `POST /api/v1/knowposts/drafts`
- `POST /api/v1/knowposts/{id}/content/confirm`
- `PATCH /api/v1/knowposts/{id}`
- `PATCH /api/v1/knowposts/{id}/top`
- `PATCH /api/v1/knowposts/{id}/visibility`
- `DELETE /api/v1/knowposts/{id}`
- `POST /api/v1/knowposts/{id}/publish`
- `POST /api/v1/knowposts/description/suggest`

Phase-one rule:

- `description/suggest` may start as deterministic truncation utility to keep scope small, then be upgraded later

- [ ] **Step 4: Re-run tests**

```powershell
mvn -q -Dtest=KnowPostServiceTest,KnowPostControllerTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(knowpost): add draft publish and detail flow"
```

## Task 8: Add Feed and Detail Cache Layer

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\cache\config\CacheProperties.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\cache\config\CacheConfig.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\service\KnowPostFeedService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\service\impl\KnowPostFeedServiceImpl.java`
- Modify: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\service\impl\KnowPostServiceImpl.java`

- [ ] **Step 1: Write failing tests for cached public feed, cached my feed, and detail invalidation after publish/update/delete**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\knowpost\service\KnowPostFeedServiceTest.java`

- [ ] **Step 2: Run tests**

```powershell
mvn -q -Dtest=KnowPostFeedServiceTest test
```

Expected: FAIL

- [ ] **Step 3: Implement Caffeine + Redis cache flow**

Phase-one cache scope:

- public feed page cache
- my published feed cache
- knowpost detail cache

Keep it simple:

- no hotkey detector
- no single-flight unless there is still time after baseline pass

- [ ] **Step 4: Re-run tests**

```powershell
mvn -q -Dtest=KnowPostFeedServiceTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(feed): add feed and detail cache layer"
```

## Task 9: Implement Elasticsearch Indexing, Search, and Suggest

**Files:**

- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\search\index\SearchIndexInitializer.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\search\index\SearchIndexService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\search\service\SearchService.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\search\service\impl\SearchServiceImpl.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\search\api\SearchController.java`
- Create: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\search\api\dto\*.java`
- Modify: `D:\dev\my_proj\java\mindshare\src\main\java\com\mindshare\knowpost\service\impl\KnowPostServiceImpl.java`

- [ ] **Step 1: Write failing tests for cursor search, suggest, and publish/update/delete sync hooks**

Create:

- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\search\service\SearchServiceTest.java`
- `D:\dev\my_proj\java\mindshare\src\test\java\com\mindshare\search\api\SearchControllerTest.java`

- [ ] **Step 2: Run tests**

```powershell
mvn -q -Dtest=SearchServiceTest,SearchControllerTest test
```

Expected: FAIL

- [ ] **Step 3: Implement search index and API**

Routes:

- `GET /api/v1/search`
- `GET /api/v1/search/suggest`

Sync rules:

- publish -> upsert index document
- metadata update -> upsert index document
- delete -> remove index document

- [ ] **Step 4: Re-run tests**

```powershell
mvn -q -Dtest=SearchServiceTest,SearchControllerTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "feat(search): add elasticsearch search and suggest api"
```

## Task 10: Strengthen Docs, Local Setup, and Smoke Verification

**Files:**

- Modify: `D:\dev\my_proj\java\mindshare\README.md`
- Modify: `D:\dev\my_proj\java\mindshare\db\schema.sql`
- Create: `D:\dev\my_proj\java\mindshare\docs\api-phase1.md`
- Create: `D:\dev\my_proj\java\mindshare\docs\runbook-local.md`

- [ ] **Step 1: Add local run documentation**

Document:

- required services
- local env variables
- startup order
- sample curl calls

- [ ] **Step 2: Run full backend test suite**

```powershell
mvn test
```

Expected: all tests PASS

- [ ] **Step 3: Run smoke flow manually**

Suggested smoke path:

- send code
- register
- login
- create draft
- presign content upload
- confirm content
- patch metadata
- publish
- query feed
- query detail
- query search

- [ ] **Step 4: Commit**

```powershell
git -C D:\dev\my_proj\java\mindshare add .
git -C D:\dev\my_proj\java\mindshare commit -m "docs: add local setup and phase one api notes"
```

## Suggested Commit Batch Summary

Keep commit boundaries close to capability boundaries:

1. `chore: bootstrap mindshare spring backend`
2. `feat(core): add shared error handling and security infrastructure`
3. `feat(user): add user and login log persistence`
4. `feat(auth): add jwt token and verification services`
5. `feat(auth): add auth api flow`
6. `feat(profile): add profile query and patch api`
7. `feat(storage): add oss presign upload api`
8. `feat(knowpost): add draft publish and detail flow`
9. `feat(feed): add feed and detail cache layer`
10. `feat(search): add elasticsearch search and suggest api`
11. `docs: add local setup and phase one api notes`

## Notes for Phase Two

Do not merge these into phase one:

- `counter`
- `relation`
- `outbox/kafka/canal`
- `rag`

Phase-two work should attach to the boundaries created here rather than reopening the phase-one design.
