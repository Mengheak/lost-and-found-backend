# Security and Infrastructure Fixes

Date: 2026-09-22

## Purpose

This document records the security and deployment problems found during the Spring Boot project
review and describes the changes made to correct them. The review covered Spring Security, JWT
authentication, Flyway migrations, PostgreSQL configuration, and Docker Compose.

## Summary

| # | Problem | Risk | Resolution |
|---|---|---|---|
| 1 | WebSocket destinations lacked authorization | Users could read or inject messages in conversations they did not belong to | Added participant checks for subscriptions, sends, and outbound delivery |
| 2 | Built-in administrator used public credentials and existing users were promoted automatically | Immediate administrator compromise or unintended privilege escalation | Removed defaults and made administrator provisioning explicit and fail-closed |
| 3 | Role changes did not invalidate existing JWTs | A demoted administrator retained privileges until token expiry | Revoke all access and refresh tokens whenever a role changes |
| 4 | Refresh-token rotation used a read followed by a write | Concurrent requests could reuse one refresh token and mint multiple token pairs | Replaced it with one atomic conditional database update |
| 5 | JWT signing had a public fallback secret and login throttling was not implemented | Token forgery risk and unrestricted password guessing | Require a strong explicit secret and enforce temporary login lockouts |
| 6 | Complete bearer tokens were stored indefinitely | A database read leak exposed reusable credentials and the table grew continuously | Store SHA-256 fingerprints with expiration metadata and purge expired records |
| 7 | The application connects with the PostgreSQL bootstrap account | Application compromise can provide schema and cluster-level privileges | Deferred; the role-separation change was intentionally rolled back |

## 1. WebSocket conversation authorization

### Problem

JWT authentication was checked only during the STOMP `CONNECT` frame. After connecting, a user
could subscribe to `/topic/conversations/{id}` without proving that they belonged to that
conversation. A client could also send directly to broker destinations and bypass the participant
check performed by the chat service. Long-lived connections were not rechecked after token expiry
or logout.

### Fix

- Store the authenticated user ID and access token against the WebSocket session.
- Permit subscriptions only to the exact `/topic/conversations/{uuid}` format.
- Permit sends only to the exact `/app/conversations/{uuid}/send` format.
- Query `ConversationRepository.existsForParticipant` before accepting either operation.
- Reject direct client sends to `/topic` and `/queue` broker destinations.
- Revalidate token signature, expiry, type, and revocation state on later inbound frames.
- Revalidate authorization before each outbound conversation delivery.
- Remove session authentication state on STOMP or transport disconnect.

### Result

Only conversation participants can send or receive that conversation's messages. Logging out,
revoking a token, or allowing it to expire prevents further use of an existing socket session.

Key files:

- `src/main/java/com/group5/lostandfoundjava/security/AuthChannelInterceptor.java`
- `src/main/java/com/group5/lostandfoundjava/config/WebSocketConfig.java`
- `src/main/java/com/group5/lostandfoundjava/repository/ConversationRepository.java`
- `src/test/java/com/group5/lostandfoundjava/security/AuthChannelInterceptorTest.java`

## 2. Secure initial administrator provisioning

### Problem

The application contained a known administrator email and password. Startup also promoted an
existing regular account when its email matched `ADMIN_EMAIL`. This could expose administrator
access and silently elevate the wrong user.

### Fix

- Remove the built-in administrator email and password from `application.yaml` and `.env.example`.
- Disable administrator bootstrap when both values are empty.
- Require `ADMIN_EMAIL` and `ADMIN_PASSWORD` to be supplied together.
- Require an administrator password containing at least 12 characters.
- Fail startup when `ADMIN_EMAIL` belongs to an existing non-admin account.
- Never promote a regular user during bootstrap.
- Restrict password reset behavior to accounts that already have the `ADMIN` role.
- Detect the retired `12345678` password and require an explicit one-boot reset.

### Result

The application starts without a default administrator unless explicit credentials are provided.
Configuration mistakes fail visibly instead of granting privileges silently.

Key files:

- `src/main/java/com/group5/lostandfoundjava/bootstrap/AdminBootstrap.java`
- `src/main/java/com/group5/lostandfoundjava/config/AdminProperties.java`
- `src/test/java/com/group5/lostandfoundjava/bootstrap/AdminBootstrapTest.java`

## 3. Immediate JWT invalidation after role changes

### Problem

The user's role is embedded in each access token. Updating the database role did not change tokens
that had already been issued, so a demoted administrator could continue using administrator
endpoints until the access token expired.

### Fix

`AdminUserServiceImpl.updateRole` now updates the role and revokes every access and refresh token
belonging to that user in the same transaction. A no-op role update does not revoke tokens.

### Result

Old HTTP and WebSocket credentials stop working immediately after a promotion or demotion. The
affected user must sign in again to receive a token containing the current role.

Key files:

- `src/main/java/com/group5/lostandfoundjava/service/impl/AdminUserServiceImpl.java`
- `src/test/java/com/group5/lostandfoundjava/service/impl/AdminUserServiceImplTest.java`
- `src/test/java/com/group5/lostandfoundjava/integration/RbacIntegrationTest.java`

## 4. Atomic refresh-token rotation

### Problem

Refresh originally performed these operations separately:

1. Read whether the refresh token was active.
2. Revoke it.
3. Issue a replacement pair.

Two concurrent requests could both pass the active check before either request committed its
revocation, allowing one token to be exchanged more than once.

### Fix

Rotation now executes one conditional database update:

```sql
UPDATE tokens
SET revoked = TRUE, expired = TRUE
WHERE token_hash = ?
  AND revoked = FALSE
  AND expired = FALSE
  AND expires_at > CURRENT_TIMESTAMP;
```

Only the request that changes one row may issue a replacement pair. Competing requests change zero
rows and receive `401 Unauthorized`.

### Result

A refresh token has exactly one successful consumer, including when requests arrive concurrently.

Key files:

- `src/main/java/com/group5/lostandfoundjava/repository/TokenRepository.java`
- `src/main/java/com/group5/lostandfoundjava/service/impl/TokenServiceImpl.java`
- `src/main/java/com/group5/lostandfoundjava/service/impl/AuthServiceImpl.java`
- `src/test/java/com/group5/lostandfoundjava/integration/AuthTokenRevocationIntegrationTest.java`

## 5. JWT secret enforcement and login throttling

### Problem

If `JWT_SECRET` was absent, the application used a published development value. The README and API
documentation also described login lockout settings, but no source code enforced them.

### Fix

- Make `JWT_SECRET` mandatory.
- Reject signing secrets shorter than 32 bytes before the application accepts traffic.
- Add typed `LoginThrottleProperties` configuration.
- Track failed attempts using normalized email addresses.
- Lock an email after five failures by default.
- Reject every password, including the correct one, during the default 15-minute lockout.
- Reset the failure streak after a successful login or expired lockout.
- Keep the same invalid-credentials response for unknown emails and incorrect passwords.

Configuration:

| Variable | Default | Purpose |
|---|---:|---|
| `JWT_SECRET` | Required | HS256 signing key; at least 32 bytes |
| `LOGIN_MAX_ATTEMPTS` | `5` | Failures before lockout |
| `LOGIN_LOCKOUT` | `15m` | Lockout duration |
| `LOGIN_ATTEMPT_WINDOW` | `15m` | Maximum age of one failure streak |

### Result

The service cannot start with a missing or weak JWT secret, and repeated login failures receive
`429 Too Many Requests` after the configured threshold.

The login throttle is held in application memory. It resets on restart and applies independently
to each application instance. A shared store such as Redis would be required for a multi-instance
deployment.

Key files:

- `src/main/java/com/group5/lostandfoundjava/security/JwtProvider.java`
- `src/main/java/com/group5/lostandfoundjava/config/LoginThrottleProperties.java`
- `src/main/java/com/group5/lostandfoundjava/service/LoginAttemptService.java`
- `src/test/java/com/group5/lostandfoundjava/security/JwtProviderTest.java`
- `src/test/java/com/group5/lostandfoundjava/service/LoginAttemptServiceTest.java`

## 6. Hashed token storage and expiration cleanup

### Problem

Access and refresh tokens were stored verbatim. Anyone who obtained read access to the database
could reuse active bearer credentials. The table had no JWT expiration timestamp and no cleanup
mechanism, so naturally expired records remained indefinitely.

### Fix

- Fingerprint every token with SHA-256 before any repository operation.
- Store the 64-character fingerprint instead of reusable token text.
- Store the signed JWT expiration as `expires_at`.
- Include `expires_at` in active-token and refresh-consumption checks.
- Add an hourly cleanup job controlled by `TOKEN_CLEANUP_CRON`.
- Add Flyway migration `V5__secure_token_storage.sql` to replace the plaintext column and add an
  expiration index.

### Migration impact

V5 deletes existing token rows before changing the schema. Existing rows contain plaintext
credentials and do not contain trusted database expiry metadata, so converting them safely is not
possible. Every user must sign in again once after V5 is deployed.

### Result

A database read leak exposes only one-way token fingerprints. Expired records are rejected even
before scheduled deletion and are removed automatically.

Key files:

- `src/main/resources/db/migration/V5__secure_token_storage.sql`
- `src/main/java/com/group5/lostandfoundjava/entity/Token.java`
- `src/main/java/com/group5/lostandfoundjava/service/TokenCleanupJob.java`
- `src/test/java/com/group5/lostandfoundjava/service/impl/TokenServiceImplTest.java`

## 7. PostgreSQL least-privilege deployment (deferred)

### Problem

Docker used `DB_USER` as `POSTGRES_USER`. The official PostgreSQL image creates that role as the
bootstrap superuser, and the application then connected with the same credentials. Application
code therefore had cluster administration and schema migration privileges.

### Current state

The least-privilege role change was rolled back to keep the development setup simple. Compose now
uses one `DB_USER` and `DB_PASSWORD` for PostgreSQL, Spring Flyway, and the application. The
`db-init`, standalone `migrate`, and `db-grants` services and their shell scripts were removed.
Spring Boot runs the Flyway migrations during application startup.

This issue remains open and can be addressed later when separate production database identities
are needed.

## Required deployment configuration

Before deploying the revised Compose stack, set all of these values in `.env`:

```dotenv
DB_NAME=lostfound
DB_USER=lostfound
DB_PASSWORD=<database password>
JWT_SECRET=<at least 32 bytes of random data>
GH_REPO=<owner>/<repository>
```

If the existing administrator account still uses the retired password, also configure a new
`ADMIN_PASSWORD`, set `ADMIN_RESET_PASSWORD=true` for one successful startup, and then return the
flag to `false`.

## Validation performed

- WebSocket authorization: 13 focused tests passed.
- Administrator bootstrap: 9 focused tests passed.
- Role-change revocation and WebSocket regression checks: 19 focused tests passed.
- Atomic refresh rotation: 17 focused tests passed; a real PostgreSQL concurrency test was added.
- JWT and login throttling: 22 focused tests passed.
- Hashed token storage and authentication regressions: 34 focused tests passed.
- `docker compose config --quiet` passed before the role-separation rollback; the simplified
  Compose configuration is validated separately after this update.
- `git diff --check` passed after each change.

The numbers above come from focused runs and overlap; they should not be added together. The full
Testcontainers suite and Flyway V5 execution could not be run locally
because the Docker daemon was unavailable. Those checks should run in CI or on a host with Docker
before production deployment.
