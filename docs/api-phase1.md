# MindShare Phase-One API Summary

This document summarizes the currently implemented phase-one HTTP APIs.

Base path:

- `/api/v1`

Authentication:

- protected endpoints expect a Bearer token issued by `/api/v1/auth/login` or `/api/v1/auth/register`
- tests currently use JWT mocks; real runtime uses the configured RSA keys

## Counter

### `POST /api/v1/action/like`

Protected action endpoint.

### `POST /api/v1/action/unlike`

Protected action endpoint.

### `POST /api/v1/action/fav`

Protected action endpoint.

### `POST /api/v1/action/unfav`

Protected action endpoint.

Request body for all four action endpoints:

- `entityType`
- `entityId`

Current supported entity type:

- `knowpost`

### `GET /api/v1/counter/{etype}/{eid}`

Public read endpoint for current counter totals.

Query params:

- `metrics` optional, comma-separated, currently supports `like,fav`

## Auth

### `POST /api/v1/auth/send-code`

Send a verification code to phone or email.

### `POST /api/v1/auth/register`

Register a new user and return access/refresh tokens.

### `POST /api/v1/auth/login`

Login with password and return access/refresh tokens.

### `POST /api/v1/auth/token/refresh`

Exchange a refresh token for a new access token pair.

### `POST /api/v1/auth/logout`

Invalidate a refresh token.

### `POST /api/v1/auth/password/reset`

Reset password by verification-code flow.

### `GET /api/v1/auth/me`

Return the current authenticated user.

## Profile

### `GET /api/v1/profile`

Return the current profile snapshot.

### `PATCH /api/v1/profile`

Patch profile fields:

- `nickname`
- `bio`
- `gender`
- `birthday`
- `school`
- `tagJson`
- `avatar`

### `POST /api/v1/profile/avatar`

Multipart avatar upload endpoint.

Request:

- `multipart/form-data`
- field name: `file`

Behavior:

- upload file to OSS
- resolve public URL
- write URL back to `users.avatar`

## Storage

### `POST /api/v1/storage/presign`

Generate a presigned PUT URL for knowpost uploads.

Supported scenes:

- `knowpost_content`
- `knowpost_image`

Guard:

- authenticated user must own the target knowpost draft

## KnowPost

### `POST /api/v1/knowposts/drafts`

Create an empty draft and return its ID.

### `POST /api/v1/knowposts/{id}/content/confirm`

Confirm uploaded content object metadata for a draft.

### `PATCH /api/v1/knowposts/{id}`

Patch knowpost metadata:

- `title`
- `tagId`
- `tags`
- `imgUrls`
- `visible`
- `isTop`
- `description`

### `PATCH /api/v1/knowposts/{id}/top`

Update `isTop`.

### `PATCH /api/v1/knowposts/{id}/visibility`

Update `visible`.

### `POST /api/v1/knowposts/{id}/publish`

Publish a draft.

### `DELETE /api/v1/knowposts/{id}`

Soft-delete a knowpost.

### `GET /api/v1/knowposts/feed`

Public feed.

Query params:

- `page`
- `size`

### `GET /api/v1/knowposts/mine`

Current user's published posts.

### `GET /api/v1/knowposts/detail/{id}`

Knowpost detail.

Rules:

- public viewers can only access `published + public`
- owner can view their own non-public drafts

### `POST /api/v1/knowposts/description/suggest`

Current placeholder description suggestion API.

Note:

- this is not LLM-backed yet

## Search

### `GET /api/v1/search`

Query params:

- `q`
- `size`
- `tags`
- `after`

Current behavior:

- only indexes `published + public`
- reads real body text from `content_url`
- uses cursor-style `after` token rather than page number
- returns a snippet when body content matches the keyword
- returns real `likeCount / favoriteCount / liked / faved`

### `GET /api/v1/search/suggest`

Query params:

- `prefix`
- `size`

Current behavior:

- title completion suggestions only

## Known Phase-One Limits

- counter currently uses the smallest replica slice, not the full Kafka aggregation chain yet
- user-dimension counters are not implemented yet
- follow/relation APIs do not exist yet
- AI/RAG APIs do not exist yet
- search ranking is not yet wired to real counter signals
