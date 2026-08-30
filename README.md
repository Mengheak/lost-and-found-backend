# Lost & Found — Java backend

A Java + Spring Boot rewrite of the Lost & Found API. Users report items they have lost or found,
search what others reported, save interesting ones, chat about them, and rate each other once
something is handed back.

Same database, same endpoints and same JSON as the Kotlin version in `../lost-and-found`, so the
existing Angular frontend works against either one without a change.

---

## Quick start

You need Java 21 (or newer) and a PostgreSQL database. The Maven wrapper (`./mvnw`) downloads Maven
itself, so you do not have to install it.

### With Docker (easiest — brings its own database)

```bash
cp .env.example .env && docker compose up --build
```

### Without Docker

Start a PostgreSQL and create the database:

```bash
docker run -d --name lostfound-db -p 5432:5432 -e POSTGRES_DB=lostfound -e POSTGRES_USER=lostfound -e POSTGRES_PASSWORD=lostfound postgres:16-alpine
```

Then run the app:

```bash
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Either way the API is then on <http://localhost:8080> and the interactive docs are at
<http://localhost:8080/swagger-ui.html>.

### First login

Startup ensures a default administrator exists (`app.admin.*` in `application.yaml`, or the
`ADMIN_*` variables in `.env`). Out of the box that is `mengheak088@gmail.com` / `12345678`.
**Change the password before deploying anywhere real.**

### Run the tests

```bash
./mvnw test
```

The unit tests need nothing but Java. The end-to-end tests in `src/test/java/.../integration` start
a real PostgreSQL with Testcontainers, and are skipped automatically when Docker is not running.

---

## How the code is organised

The project uses the classic layered structure. A request travels straight down these layers and
the answer comes back up:

```
HTTP request
    │
    ▼
controller/   receives the request, checks nothing itself, returns the ApiResponse envelope
    │
    ▼
service/      all the rules live here (who may do what, what is valid, what to notify)
    │
    ▼
repository/   talks to the database; mostly just method names, Spring Data writes the SQL
    │
    ▼
entity/       the tables, as Java objects
```

```
src/main/java/com/group5/lostandfoundjava/
├── LostAndFoundJavaApplication.java   entry point
├── bootstrap/     runs once at startup (ensures the default admin exists)
├── common/        the ApiResponse envelope, PageResponse, and the global exception handler
│   └── exception/ one exception class per HTTP error, e.g. NotFoundException -> 404
├── config/        Spring configuration + the typed @ConfigurationProperties records
├── controller/    the REST endpoints, one class per resource
├── dto/           what goes in and out over HTTP, grouped by area (auth, item, chat, ...)
├── entity/        JPA entities
│   └── enums/     Role, ItemType, ItemStatus, NotificationType
├── repository/    Spring Data repositories
│   └── specification/  the dynamic WHERE clause behind the item search
├── security/      JWT creation and checking, plus login throttling
└── service/       interfaces, with the implementations in service/impl
```

Three conventions worth knowing before reading the code:

- **DTOs are `record`s.** They are short and immutable, and they decide exactly which entity fields
  reach the client — `User.passwordHash` has no matching field in `UserResponse`, so it cannot leak.
  Each response record has a `from(entity)` factory that does the mapping.
- **Entities use Lombok.** `@Getter` and `@Setter` generate the accessors at compile time. If your
  IDE cannot find `getName()`, install the Lombok plugin.
- **Services come in pairs.** `ItemService` (the interface) and `ItemServiceImpl` (the code).
  Controllers depend on the interface, which is what lets the tests replace it with a mock.

### Where the rules live

| Rule | Where |
| --- | --- |
| Field is required / too long / not an email | annotations on the request record in `dto/` |
| Rule that involves more than one field or the database | the matching class in `service/impl/` |
| Who may reach a URL at all | `config/SecurityConfig` |
| Who may call one specific method | `@PreAuthorize` on the controller method |
| Turning an exception into an HTTP status | `common/GlobalExceptionHandler` |

---

## The API

Every response uses the same envelope:

```json
{ "success": true, "message": "Success", "data": { } }
```

Paged endpoints put `content`, `page`, `size`, `totalElements`, `totalPages`, `first` and `last`
inside `data`, and accept `?page=`, `?size=` and `?sort=` query parameters.

| Method | Path | Who |
| --- | --- | --- |
| POST | `/api/auth/register` | anyone |
| POST | `/api/auth/login` | anyone |
| POST | `/api/auth/refresh` | anyone with a refresh token |
| GET | `/api/users/me` | signed in |
| PUT | `/api/users/me` | signed in |
| GET | `/api/users/{id}` | anyone |
| GET | `/api/items` | anyone |
| GET | `/api/items/{id}` | anyone |
| GET | `/api/items/my` | signed in |
| POST | `/api/items` | signed in |
| PUT | `/api/items/{id}` | the reporter |
| PATCH | `/api/items/{id}/status` | the reporter |
| DELETE | `/api/items/{id}` | the reporter |
| GET | `/api/categories`, `/api/categories/{id}` | anyone |
| POST / PUT / DELETE | `/api/categories`… | admin |
| POST / DELETE | `/api/saved-items/{itemId}` | signed in |
| GET | `/api/saved-items` | signed in |
| POST / GET | `/api/conversations` | signed in |
| GET | `/api/conversations/{id}` | the two participants |
| POST / GET | `/api/conversations/{id}/messages` | the two participants |
| POST | `/api/ratings` | signed in |
| GET | `/api/ratings/user/{userId}` | anyone |
| GET | `/api/notifications` | signed in |
| PATCH | `/api/notifications/{id}/read`, `/read-all` | the recipient |
| GET | `/api/admin/users`, `/api/admin/users/{id}` | admin |
| PATCH | `/api/admin/users/{id}/role` | admin |

The search endpoint accepts `type`, `status`, `categoryId`, `q`, `brand`, `color`, `dateFrom` and
`dateTo`. They are all optional and combine with AND.

### Real-time chat

Chat also works over STOMP on a WebSocket at `/ws`:

1. connect, sending `Authorization: Bearer <accessToken>` in the CONNECT frame
2. subscribe to `/topic/conversations/{id}`
3. publish to `/app/conversations/{id}/send` with `{ "text": "..." }`

A message sent over REST and one sent over the socket are stored and broadcast identically — both
go through `MessageService.send`.

---

## How authentication works

1. `POST /api/auth/login` returns an **access token** (15 minutes) and a **refresh token** (7 days).
2. The client sends `Authorization: Bearer <accessToken>` on every request.
3. `JwtAuthenticationFilter` checks the token and records who the caller is. It never rejects
   anything — Spring Security's rules decide afterwards, which is how the public endpoints stay
   public.
4. When the access token expires, `POST /api/auth/refresh` swaps the refresh token for a new pair.

Passwords are hashed with bcrypt (strength 12) and five failed logins on one email lock it for
15 minutes. The access token carries the user's role, so a promotion or demotion only takes effect
at the user's next login or refresh.

---

## Database

Flyway owns the schema. The migrations are in `src/main/resources/db/migration` and run
automatically at startup:

| File | What it does |
| --- | --- |
| `V1__init_schema.sql` | all eight tables and their indexes |
| `V2__seed_categories.sql` | the 13 standard categories, with fixed ids |
| `V3__add_user_role.sql` | adds `users.role` |

Hibernate is set to `ddl-auto: validate`, so it never changes the schema — it only checks that the
entities and the tables still agree, and refuses to start if they have drifted apart.

**To change the schema, add a new `V4__…sql` file.** Never edit a migration that has already run:
Flyway records a checksum of each one and will refuse to start if a file it has applied changed.

---

## Configuration

Everything has a working default for local development. Override with environment variables:

| Variable | Default | What it is |
| --- | --- | --- |
| `DB_URL` / `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost:5432/lostfound` | database location |
| `DB_USER` / `DB_PASSWORD` | `lostfound` | database credentials |
| `JWT_SECRET` | dev-only value | HS256 signing key, **must be 32+ characters** |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `15m` / `7d` | token lifetimes |
| `CORS_ALLOWED_ORIGINS` | `localhost:4200,4300` | which sites may call the API |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCKOUT` | `5` / `15m` | brute-force protection |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | see `.env.example` | default admin; empty email disables it |
| `ADMIN_RESET_PASSWORD` | `false` | one-boot escape hatch if you are locked out |
| `FIREBASE_CREDENTIALS` | empty | path to a Firebase JSON file; empty disables push |

Before deploying anywhere real, at minimum: set a fresh `JWT_SECRET`, change `ADMIN_PASSWORD`, and
narrow `CORS_ALLOWED_ORIGINS` to your actual frontend.
#   l o s t - a n d - f o u n d - b a c k e n d  
 