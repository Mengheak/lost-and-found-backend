# Lost &amp; Found — Backend API

> A Spring Boot REST + WebSocket API where people report items they **lost** or **found**, search
> what others reported, save interesting ones, chat about them, and rate each other once something
> is handed back.

<p align="left">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-16-blue">
  <img alt="Build" src="https://img.shields.io/badge/build-Maven-red">
</p>

**Looking for how the system actually behaves, step by step?** → [SYSTEM_FLOW.md](SYSTEM_FLOW.md)

---

## Table of contents

1. [What it does](#what-it-does)
2. [Tech stack](#tech-stack)
3. [Quick start](#quick-start)
4. [Project layout](#project-layout)
5. [API reference](#api-reference)
6. [Real-time chat](#real-time-chat)
7. [Authentication &amp; roles](#authentication--roles)
8. [Database](#database)
9. [Configuration](#configuration)
10. [Testing](#testing)
11. [CI/CD](#cicd)
12. [Troubleshooting](#troubleshooting)

---

## What it does

| Feature | Summary |
| --- | --- |
| 🔐 **Accounts** | Register, log in, refresh tokens. Passwords hashed with bcrypt (strength 12). |
| 🛡️ **Brute-force protection** | 5 failed logins on one email → that email is locked for 15 minutes. |
| 📦 **Items** | Report a `LOST` or `FOUND` item, edit it, change its status, delete it. |
| 🔎 **Search** | Public paged search over 8 optional filters (type, status, category, keyword, brand, colour, date range). |
| ⭐ **Saved items** | Personal shortlist; the item's owner is notified when someone saves it. |
| 💬 **Chat** | One thread per (item, pair of users), over REST **and** STOMP WebSocket. |
| 🏅 **Ratings** | 1–5 stars, one per (rater, rated user, item); the rated user's average is kept up to date. |
| 🔔 **Notifications** | In-app feed + optional Firebase Cloud Messaging push. |
| 👑 **Admin** | List/search users, promote and demote roles, manage the category taxonomy. |

Every endpoint answers with the **same envelope**, success or failure:

```json
{ "success": true, "message": "Success", "data": { } }
```

---

## Tech stack

| Layer | Choice |
| --- | --- |
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5.6 (Web, Data JPA, Security, Validation, WebSocket, Actuator) |
| Database | PostgreSQL 16, schema owned by Flyway |
| Auth | Stateless JWT (HS256) via `jjwt` 0.12.6 |
| Docs | springdoc-openapi 2.8.9 → Swagger UI |
| Push | Firebase Admin SDK 9.4.3 (optional) |
| Boilerplate | Lombok (`@Getter` / `@Setter` on entities) |
| Tests | JUnit 5, Mockito, Testcontainers (real PostgreSQL) |
| Delivery | Docker multi-stage build → GHCR → EC2 over SSH |

---

## Quick start

**Prerequisites:** Java 21+ and Docker (for the database). The Maven wrapper `./mvnw` downloads
Maven itself — you do not need Maven installed.

### Option A — run the app locally, database in Docker (recommended for development)

```bash
# Start only PostgreSQL from Compose, then run the API locally.
docker compose up -d db
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The default local database credentials in `application.yaml` match `.env.example`. Set a valid
`JWT_SECRET` before starting the API.

### Option B — everything in Docker

```bash
cp .env.example .env
# Set JWT_SECRET and GH_REPO=<owner>/<repo>.
docker compose up -d
```

`docker-compose.yml` pulls the API image from GHCR. To use your own build, build it and set
`GH_REPO=local/lost-and-found-java` in `.env`:

```bash
docker build -t ghcr.io/local/lost-and-found-java:latest .
```

### Then

| What | Where |
| --- | --- |
| API base URL | <http://localhost:8080> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Health check | <http://localhost:8080/actuator/health> |

### Initial administrator

No administrator credentials are built in. To create the initial administrator, set both
`ADMIN_EMAIL` and `ADMIN_PASSWORD` before the first startup. The password must contain at least
12 characters. If the email already belongs to a regular user, startup fails instead of promoting
that account. An existing administrator that still uses the retired built-in password also causes
startup to fail until its password is explicitly reset with `ADMIN_RESET_PASSWORD=true`.

---

## Project layout

A request travels straight down these layers, and the answer comes back up:

```
HTTP request
   │
   ▼
 controller/   maps the URL, validates the body, wraps the result in ApiResponse
   │
   ▼
 service/      every rule lives here: who may do what, what is valid, what to notify
   │
   ▼
 repository/   Spring Data — mostly just method names, the SQL is generated
   │
   ▼
 entity/       the tables, as Java objects
```

```
src/main/java/com/group5/lostandfoundjava/
├── LostAndFoundJavaApplication.java   entry point
├── bootstrap/      optionally creates an explicitly configured initial admin
├── common/         ApiResponse envelope, PageResponse, GlobalExceptionHandler
├── config/         Spring configuration + typed @ConfigurationProperties records
├── controller/     REST endpoints, one class per resource, + the STOMP chat controller
├── dto/            request/response records, grouped by area (auth, item, chat, …)
├── entity/         JPA entities
│   └── enums/      Role, ItemType, ItemStatus, NotificationType
├── exception/      one class per HTTP error, e.g. NotFoundException → 404
├── repository/     Spring Data repositories
│   └── specification/   the dynamic WHERE clause behind the item search
├── security/       JWT creation/validation, HTTP filter, STOMP interceptor, login throttle
└── service/        interfaces, with the implementations in service/impl/
```

### Three conventions to know before reading the code

- **DTOs are `record`s.** Immutable, and they decide exactly which entity fields reach the client —
  `User.passwordHash` has no matching field in `UserResponse`, so it cannot leak. Each response
  record has a static `from(entity)` factory.
- **Entities use Lombok.** `@Getter`/`@Setter` generate accessors at compile time. If your IDE
  cannot find `getName()`, install the Lombok plugin.
- **Services come in pairs.** `ItemService` (interface) + `ItemServiceImpl` (code). Controllers
  depend on the interface, which is what lets tests swap in a mock.

### Where each kind of rule lives

| Rule | Where it is written |
| --- | --- |
| Field required / too long / not an email | Bean Validation annotations on the request record in `dto/` |
| Rule spanning several fields or the database | the matching class in `service/impl/` |
| Who may reach a URL at all | `config/SecurityConfig` |
| Who may call one specific method | `@PreAuthorize` on the controller method |
| Turning an exception into an HTTP status | `common/GlobalExceptionHandler` |

---

## API reference

All paths are prefixed with the base URL. Paged endpoints accept `?page=`, `?size=` and `?sort=`,
and return `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` inside `data`.

### Authentication

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | public | 201; returns tokens + user |
| `POST` | `/api/auth/login` | public | 429 when the email is locked out |
| `POST` | `/api/auth/refresh` | public | swaps a refresh token for a new pair |

### Users

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/api/users/me` | signed in |
| `PUT` | `/api/users/me` | signed in |
| `GET` | `/api/users/{id}` | public (no email/phone in the response) |

### Items

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/api/items` | public — paged search |
| `GET` | `/api/items/{id}` | public |
| `GET` | `/api/items/my` | signed in |
| `POST` | `/api/items` | signed in (201) |
| `PUT` | `/api/items/{id}` | the reporter only |
| `PATCH` | `/api/items/{id}/status` | the reporter only |
| `DELETE` | `/api/items/{id}` | the reporter only |

Search filters (all optional, combined with `AND`):

| Parameter | Type | Meaning |
| --- | --- | --- |
| `type` | `LOST` \| `FOUND` | which side of the board |
| `status` | `OPEN` \| `RETURNED` \| `CLOSED` | lifecycle state |
| `categoryId` | UUID | single category |
| `q` | text | case-insensitive match on **name or description** |
| `brand`, `color` | text | case-insensitive `LIKE` |
| `dateFrom`, `dateTo` | ISO-8601 instant | on the item's `dateTime`, inclusive |

Default paging: `size=20`, sorted by `createdAt` descending.

### Categories · Saved items · Chat · Ratings · Notifications · Admin

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/api/categories`, `/api/categories/{id}` | public |
| `POST` / `PUT` / `DELETE` | `/api/categories`, `/api/categories/{id}` | **admin** |
| `POST` / `DELETE` | `/api/saved-items/{itemId}` | signed in |
| `GET` | `/api/saved-items` | signed in |
| `POST` / `GET` | `/api/conversations` | signed in |
| `GET` | `/api/conversations/{id}` | the two participants |
| `POST` / `GET` | `/api/conversations/{id}/messages` | the two participants |
| `POST` | `/api/ratings` | signed in |
| `GET` | `/api/ratings/user/{userId}` | public |
| `GET` | `/api/notifications` | signed in |
| `PATCH` | `/api/notifications/{id}/read`, `/api/notifications/read-all` | the recipient |
| `GET` | `/api/admin/users`, `/api/admin/users/{id}` | **admin** |
| `PATCH` | `/api/admin/users/{id}/role` | **admin** |

### Error responses

| Status | When |
| --- | --- |
| `400` | validation failure (field → message map in `data`), or a broken business rule |
| `401` | missing/expired/invalid access token, or wrong credentials |
| `403` | signed in, but not the owner / not a participant / not an admin |
| `404` | the row does not exist |
| `405` | wrong HTTP method for that path |
| `409` | duplicate email, duplicate category name, duplicate rating, FK still referenced |
| `429` | login lockout in effect |
| `500` | anything unexpected — logged in full, never leaked to the client |

---

## Real-time chat

Chat runs over STOMP on a WebSocket at `/ws` (in-memory broker, prefixes `/topic` and `/queue`;
client sends go to `/app`).

```
1. CONNECT   ws://localhost:8080/ws
             header  Authorization: Bearer <accessToken>     ← required, connection is rejected without it
2. SUBSCRIBE /topic/conversations/{conversationId}
3. SEND      /app/conversations/{conversationId}/send   body { "text": "hello" }
```

A message sent over REST and one sent over the socket are stored and broadcast **identically** —
both go through `MessageService.send`.

---

## Authentication &amp; roles

1. `POST /api/auth/login` returns an **access token** (15 min) and a **refresh token** (7 days).
2. The client sends `Authorization: Bearer <accessToken>` on every request.
3. `JwtAuthenticationFilter` reads the token and records who the caller is. It **never rejects**
   anything — Spring Security's rules decide afterwards, which is how public endpoints stay public.
4. When the access token expires, `POST /api/auth/refresh` atomically consumes the refresh token
   and returns a new pair. Concurrent replays of the consumed token receive `401 Unauthorized`.

| | Access token | Refresh token |
| --- | --- | --- |
| Lifetime | 15 minutes | 7 days |
| Carries the role? | yes | no — it is re-read from the database on refresh |
| Accepted by | every endpoint | only `/api/auth/refresh` |

The database stores only SHA-256 token fingerprints and their JWT expiration times, never reusable
bearer-token text. Expired records are deleted hourly. Migration V5 deliberately revokes all tokens
created by older versions because those rows contain plaintext credentials without expiry metadata.

Two roles exist: `USER` and `ADMIN`. Changing a role immediately revokes all access and refresh
tokens belonging to that user. They must sign in again to receive tokens with the new role.

Full detail — including exactly what happens when someone spams the login endpoint — is in
[SYSTEM_FLOW.md](SYSTEM_FLOW.md).

---

## Database

Flyway owns the schema; migrations live in `src/main/resources/db/migration` and run when the
Spring Boot application starts.

| File | What it does |
| --- | --- |
| `V1__init_schema.sql` | all eight tables, their foreign keys and indexes |
| `V2__seed_categories.sql` | 13 standard categories with fixed UUIDs |
| `V3__add_user_role.sql` | adds `users.role` with a `CHECK` constraint |

```
users ──< items >── categories          conversations ── item + user_a + user_b
  │        │                                   │
  │        ├──< item_photo_urls                └──< messages >── users (sender)
  │        ├──< saved_items >── users
  │        └──< ratings >── users (from/to)
  └──< notifications
```

Hibernate runs with `ddl-auto: validate` — it never changes the schema, it only checks that the
entities and the tables still agree and refuses to start if they have drifted.

> **To change the schema, add a new `V4__….sql` file.** Never edit a migration that has already
> run: Flyway stores a checksum of each one and will refuse to start if an applied file changed.

---

## Configuration

Override application settings with environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` / `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost:5432/lostfound` | database location |
| `DB_USER` / `DB_PASSWORD` | `lostfound` / `lostfound` | shared database credentials used by PostgreSQL, Flyway, and the application |
| `JWT_SECRET` | required | HS256 signing key — startup fails unless it contains at least 32 bytes |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `15m` / `7d` | token lifetimes |
| `TOKEN_CLEANUP_CRON` | `0 0 * * * *` | Spring cron expression for deleting expired token fingerprints |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,http://localhost:4300` | browser origins allowed to call the API |
| `LOGIN_MAX_ATTEMPTS` | `5` | failures before an email is locked |
| `LOGIN_LOCKOUT` | `15m` | how long the lockout lasts |
| `LOGIN_ATTEMPT_WINDOW` | `15m` | failures further apart than this do not add up |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | empty / empty / `Administrator` | optional initial admin; email and a 12+ character password are both required |
| `ADMIN_RESET_PASSWORD` | `false` | explicitly reset the password of an existing admin on one boot |
| `FIREBASE_CREDENTIALS` | empty | path to a Firebase service-account JSON; empty = push disabled |
| `GH_REPO` / `IMAGE_TAG` | — | used by `docker-compose.yml` to pick the GHCR image |

Profiles: the default `application.yaml`, plus `application-docker.yaml`
(`SPRING_PROFILES_ACTIVE=docker`) which changes the datasource host and log pattern.

**Production checklist:** fresh `JWT_SECRET`, an explicitly chosen strong `ADMIN_PASSWORD` when
bootstrapping an admin, `CORS_ALLOWED_ORIGINS` narrowed to your real frontend, and
`ADMIN_RESET_PASSWORD=false`.

---

## Testing

```bash
./mvnw test        # unit tests + integration tests
./mvnw verify      # what CI runs
```

| Kind | Location | Needs Docker? |
| --- | --- | --- |
| Unit (Mockito) | `src/test/java/.../service/impl`, `.../security`, `.../bootstrap` | no |
| End-to-end | `src/test/java/.../integration` | yes — Testcontainers starts a real PostgreSQL |

The integration tests are annotated `@Testcontainers(disabledWithoutDocker = true)`, so they are
**skipped, not failed**, on a machine without a running Docker daemon.

---

## CI/CD

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `.github/workflows/ci.yaml` | every PR and push to `main` | `./mvnw -B verify`; uploads Surefire reports on failure |
| `.github/workflows/deploy.yml` | push to `main` | builds and pushes the API image, recreates the Compose application, then polls `/actuator/health` |

The `Dockerfile` is a two-stage build: Maven + JDK 21 to build, `eclipse-temurin:21-jre-alpine` to
run, as a non-root `app` user, with a `HEALTHCHECK` hitting `/actuator/health`.

---

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| App exits with a Hibernate *schema validation* error | Entities and tables drifted. Add a Flyway migration rather than editing an old one. |
| Flyway *checksum mismatch* on startup | An already-applied migration file was edited. Restore it and add a new `V…` file instead. |
| `401` on every request | Access token expired (15 min) — call `/api/auth/refresh`. |
| `403` although you are an admin | Role changes only reach the token on the next login/refresh. |
| `429` on login | Lockout is active for that email; wait it out or restart the app (counters are in memory). |
| WebSocket connects then immediately drops | The STOMP `CONNECT` frame had no valid `Authorization: Bearer …` header. |
| IDE cannot resolve `getX()` on an entity | Lombok plugin / annotation processing not enabled. |
| `docker compose up` pulls `ghcr.io/:latest` | `GH_REPO` is missing from `.env` — see [Quick start](#quick-start). |
| Push notifications never arrive | `FIREBASE_CREDENTIALS` is empty or points at a missing file; the app silently uses the no-op sender (check the startup log). |

---

## License &amp; credits

Built by **Group 5**. See [SYSTEM_FLOW.md](SYSTEM_FLOW.md) for the end-to-end behaviour of every
feature, and [HELP.md](HELP.md) for the generated Spring Boot reference links.
