# System Flow — Lost &amp; Found Backend

This document walks through **what actually happens**, class by class, for every flow in the
system: from the moment the JVM starts, through a request being authenticated, to an item being
reported, a message being delivered and a rating being recorded.

For setup, endpoint tables and configuration, see [README.md](README.md).

---

## Contents

1. [Cast of characters](#1-cast-of-characters)
2. [Application startup flow](#2-application-startup-flow)
3. [The universal request flow](#3-the-universal-request-flow)
4. [Registration flow](#4-registration-flow)
5. [Login flow — and what happens when it is spammed](#5-login-flow--and-what-happens-when-it-is-spammed)
6. [Token refresh flow](#6-token-refresh-flow)
7. [Authorization flow (RBAC)](#7-authorization-flow-rbac)
8. [Profile flows](#8-profile-flows)
9. [Item flows](#9-item-flows)
10. [Item search flow](#10-item-search-flow)
11. [Saved item flow](#11-saved-item-flow)
12. [Conversation flow](#12-conversation-flow)
13. [Message flow — REST and WebSocket](#13-message-flow--rest-and-websocket)
14. [WebSocket connection flow](#14-websocket-connection-flow)
15. [Notification and push flow](#15-notification-and-push-flow)
16. [Rating flow](#16-rating-flow)
17. [Category flow](#17-category-flow)
18. [Admin user management flow](#18-admin-user-management-flow)
19. [Error flow](#19-error-flow)
20. [Deployment flow](#20-deployment-flow)
21. [Known limits of the current design](#21-known-limits-of-the-current-design)

---

## 1. Cast of characters

Every flow below is some combination of these pieces.

| Component | File | Responsibility |
| --- | --- | --- |
| `JwtProvider` | `security/JwtProvider.java` | The **only** class that knows the token format. Creates and parses JWTs. |
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter.java` | Reads the `Authorization` header on every HTTP request and records who the caller is. Never rejects. |
| `AuthChannelInterceptor` | `security/AuthChannelInterceptor.java` | The WebSocket equivalent. Checks the token on `CONNECT` and **does** reject. |
| `LoginAttemptService` | `security/LoginAttemptService.java` | In-memory brute-force counter, keyed by email. |
| `SecurityConfig` | `config/SecurityConfig.java` | The URL → who-may-call-it table. Stateless, CSRF off, CORS on. |
| `GlobalExceptionHandler` | `common/GlobalExceptionHandler.java` | Turns every exception into the `ApiResponse` envelope with the right status. |
| `ApiResponse` / `PageResponse` | `common/` | The envelope and the paging shape returned to clients. |
| `AdminBootstrap` | `bootstrap/AdminBootstrap.java` | Runs once per boot to guarantee an admin exists. |
| `NotificationService` | `service/impl/NotificationServiceImpl.java` | Writes the in-app feed row **and** fires the push. |
| `PushSender` | `service/impl/FcmPushSender.java` / `NoopPushSender.java` | Firebase push, or a silent stand-in when Firebase is not configured. |

---

## 2. Application startup flow

```
java -jar app.jar
   │
   ▼
LostAndFoundJavaApplication.main()
   │
   ├─▶ Spring reads application.yaml (+ application-docker.yaml when SPRING_PROFILES_ACTIVE=docker)
   │      and binds the typed records:
   │      JwtProperties · CorsProperties · LoginThrottleProperties · AdminProperties
   │      → a typo in configuration fails HERE, at startup, not at midnight
   │
   ├─▶ DataSource connects to PostgreSQL (DB_URL / DB_HOST / DB_PORT / DB_NAME)
   │
   ├─▶ FLYWAY runs classpath:db/migration in order
   │      V1__init_schema.sql      8 tables + indexes
   │      V2__seed_categories.sql  13 categories with fixed UUIDs
   │      V3__add_user_role.sql    users.role + CHECK constraint
   │      → each file's checksum is stored; editing an applied file aborts the boot
   │
   ├─▶ HIBERNATE starts with ddl-auto: validate
   │      compares @Entity classes against the real tables
   │      → any drift aborts the boot; it never alters the schema itself
   │
   ├─▶ PushConfig.pushSender(...) decides which PushSender bean exists:
   │      app.fcm.credentials-path empty or file missing → NoopPushSender  (logs "push disabled")
   │      file present                                   → FcmPushSender   (logs "FCM initialized")
   │
   ├─▶ SecurityConfig builds the filter chain; WebSocketConfig registers /ws and the STOMP broker
   │
   ├─▶ Tomcat starts on server.port (8080)
   │
   └─▶ AdminBootstrap.run()   ← an ApplicationRunner, so it fires after the context is ready
```

### AdminBootstrap decision tree

```
app.admin.email
   │
   ├── empty ───────────────────────────────▶ do nothing (feature switched off)
   │
   └── set → look the account up by email
             │
             ├── NOT found
             │      ├── app.admin.password empty → log a warning, skip
             │      └── password set            → CREATE the account with role = ADMIN
             │
             └── FOUND
                    ├── role != ADMIN                    → promote to ADMIN
                    ├── app.admin.reset-password = true  → overwrite the password hash (logged as a warning)
                    └── nothing changed                  → no database write at all
```

> **Why it never overwrites an existing password by default:** pointing `ADMIN_EMAIL` at a real
> user's address must not hand their account to whoever knows `ADMIN_PASSWORD`. The
> `ADMIN_RESET_PASSWORD` flag is the deliberate, logged escape hatch for when everyone is locked out.

Once startup finishes, `/actuator/health` reports `UP` — which is what Docker's `HEALTHCHECK` and
the deploy workflow poll.

---

## 3. The universal request flow

Every HTTP request, public or protected, follows this path:

```
Browser / mobile client
   │  HTTP request
   ▼
① CORS filter  (WebConfig → CorsConfigurationSource)
   │  origin in app.cors.allowed-origins?     no → browser blocks the response
   │  methods GET POST PUT PATCH DELETE OPTIONS, headers Authorization + Content-Type
   │  allowCredentials = false (auth is a header, not a cookie); preflight cached 1 hour
   ▼
② JwtAuthenticationFilter
   │  header starts with "Bearer "?  → JwtProvider.parse(token)
   │      signature bad / expired / malformed → parse() returns null → stays anonymous
   │      token type != "access"              → ignored (a refresh token is NOT an access token)
   │      valid                                → SecurityContext = (UUID userId, ROLE_USER|ROLE_ADMIN)
   │  NOTHING is rejected here — that is deliberate, it is what keeps public endpoints public
   ▼
③ SecurityConfig.authorizeHttpRequests  — first matching rule wins
   │  permitAll : /api/auth/**, /swagger-ui**, /v3/api-docs/**, /actuator/health**, /ws/**
   │  authenticated (GET) : /api/items/my, /api/users/me
   │  permitAll (GET) : /api/items, /api/items/*, /api/categories, /api/categories/*,
   │                    /api/users/*, /api/ratings/user/*
   │  hasRole(ADMIN) : /api/admin/**
   │  anyRequest     : authenticated
   │
   │  not signed in → 401 {"success":false,"message":"Authentication required"}
   │  signed in but not allowed → 403 {"success":false,"message":"Access denied"}
   │  (both written by hand in SecurityConfig, because no controller has run yet)
   ▼
④ @PreAuthorize  (method-level, enabled by @EnableMethodSecurity)
   │  e.g. AdminUserController (class-level hasRole('ADMIN')),
   │       CategoryController write methods
   ▼
⑤ Controller
   │  @Valid @RequestBody → Bean Validation on the request record
   │       failure → MethodArgumentNotValidException → 400 with a field→message map in `data`
   │  @AuthenticationPrincipal UUID userId  ← the principal filter ② stored
   ▼
⑥ Service (@Transactional)
   │  ALL business rules: ownership, participation, uniqueness, cross-field validity
   │  throws BadRequest / Unauthorized / Forbidden / NotFound / Conflict / TooManyRequests
   ▼
⑦ Repository → Hibernate → PostgreSQL
   ▼
⑧ Response mapping
   │  entity → record via ResponseRecord.from(entity)   ← this is where passwordHash is dropped
   │  Page<T> → PageResponse.from(page)
   │  wrapped in ApiResponse.ok(data)
   ▼
   { "success": true, "message": "Success", "data": … }
```

If anything in ⑤–⑧ throws, `GlobalExceptionHandler` catches it and produces the **same envelope**
with `"success": false` — see [§19](#19-error-flow).

> `spring.jpa.open-in-view: false`: the Hibernate session closes when the service method returns,
> so every field a response needs must be read inside the transaction. That is exactly why the
> `from(entity)` mappers are called from services/controllers that still hold loaded entities.

---

## 4. Registration flow

```
POST /api/auth/register
{ "name": "Dara", "email": "Dara@Example.COM", "phone": "0123", "password": "secret12" }
   │
   ▼ AuthController.register        @Valid RegisterRequest
   │    name    @NotBlank @Size(max=255)
   │    email   @NotBlank @Email @Size(max=255)
   │    phone   @Size(max=50)          (optional)
   │    password @NotBlank @Size(min=8, max=72)   ← 72 is bcrypt's input limit
   │
   ▼ AuthServiceImpl.register    @Transactional
   │    ① email = email.trim().toLowerCase()      → "dara@example.com"
   │    ② userRepository.existsByEmail(email)?
   │           yes → ConflictException → 409 "Email is already registered"
   │    ③ passwordEncoder.encode(password)        BCrypt, strength 12
   │    ④ save User(name, email, phone, hash, role = USER)
   │    ⑤ issue tokens immediately — registering logs you in
   │
   ▼ 201 Created
{ "success": true, "message": "Success",
  "data": { "accessToken": "…", "refreshToken": "…", "tokenType": "Bearer",
            "expiresInSeconds": 900, "user": { … no password field exists … } } }
```

Emails are normalised on the way in, so `Dara@Example.COM` and `dara@example.com` are the same
account — and the `uq_users_email` unique constraint is the final backstop if two registrations race.

---

## 5. Login flow — and what happens when it is spammed

This is the flow with the most moving parts, so it gets the most detail.

### 5.1 The happy path

```
POST /api/auth/login   { "email": "dara@example.com", "password": "secret12" }
   │
   ▼ AuthServiceImpl.login   @Transactional(readOnly = true)
   │
   ① email = trim().toLowerCase()
   │
   ② loginAttemptService.lockoutSecondsRemaining(email)   ← THE THROTTLE CHECK, BEFORE ANYTHING ELSE
   │      returns null → not locked → continue
   │
   ③ userRepository.findByEmail(email)
   │
   ④ passwordEncoder.matches(rawPassword, user.passwordHash)   ← bcrypt compare
   │
   ⑤ loginAttemptService.recordSuccess(email)   ← wipes the whole failure streak
   │
   ⑥ JwtProvider issues:
   │      accessToken  = { sub: userId, type: "access",  role: "USER"|"ADMIN", iat, exp = +15m }
   │      refreshToken = { sub: userId, type: "refresh", iat, exp = +7d }        ← NO role claim
   │
   ▼ 200 OK  → AuthResponse (same shape as registration)
```

### 5.2 The failure path — one wrong password

```
③/④ user not found  OR  password does not match
      │
      ├─▶ loginAttemptService.recordFailure(email)
      │
      └─▶ throw UnauthorizedException("Invalid email or password")  → 401
```

> **The same message for both cases is deliberate.** Answering "no such user" for an unknown email
> would turn the login endpoint into a tool for checking which addresses are registered here.

### 5.3 What `LoginAttemptService` does when someone spams login

State lives in a `ConcurrentHashMap<String, Attempts>` where
`Attempts = (count, firstFailureAt, lockedUntil)` and the key is the **normalised email**
(`trim().toLowerCase()`, so `Jane@Example.COM` and `jane@example.com` share one counter).

Defaults (`app.login-throttle`, overridable by `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCKOUT` /
`LOGIN_ATTEMPT_WINDOW`):

| Setting | Default | Meaning |
| --- | --- | --- |
| `max-attempts` | `5` | failures allowed before the email is locked |
| `lockout-duration` | `15m` | how long the lock lasts |
| `attempt-window` | `15m` | failures further apart than this do **not** belong to the same streak |

**`recordFailure(email)` — called on every wrong password:**

```
now = clock.instant()
attempts.compute(email, existing -> {

    stale = (existing == null)                                  ← first ever failure
         || (now - existing.firstFailureAt) >= attemptWindow    ← the old streak has aged out

    count          = stale ? 1   : existing.count + 1
    firstFailureAt = stale ? now : existing.firstFailureAt      ← the window is anchored to the FIRST failure
    lockedUntil    = (count >= maxAttempts) ? now + lockoutDuration : null

    return new Attempts(count, firstFailureAt, lockedUntil)
})
```

**`lockoutSecondsRemaining(email)` — called at the top of every login:**

```
record missing, or lockedUntil == null      → null   (not locked)
lockedUntil already in the past             → remove the entry entirely, return null
                                              ↑ the lockout expiring also RESETS the counter to zero
otherwise                                   → seconds remaining (never less than 1)
```

**The spam timeline, step by step:**

```
attempt 1  wrong  → count=1, streak starts               → 401 Invalid email or password
attempt 2  wrong  → count=2                              → 401
attempt 3  wrong  → count=3                              → 401
attempt 4  wrong  → count=4                              → 401
attempt 5  wrong  → count=5 ≥ 5 → lockedUntil = now+15m  → 401   ← still 401: the failure is
                                                                   recorded, THEN the exception is
                                                                   thrown, so the lock only takes
                                                                   effect from the next request
attempt 6  ANY password, even the CORRECT one
           → lockoutSecondsRemaining() returns ~900
           → TooManyRequestsException
           → 429 { "success": false,
                   "message": "Too many failed attempts. Try again in 15 minute(s)." }
              ↑ seconds are rounded UP to whole minutes: (seconds + 59) / 60

… 15 minutes pass …

attempt 7  → lockoutSecondsRemaining() sees an expired lock, deletes the entry, returns null
           → the login proceeds normally, with a clean counter
```

**Two properties worth noticing:**

- **Locked means locked.** Once `lockedUntil` is set, the correct password is refused too. The
  throttle check happens *before* the database is even queried.
- **A success clears everything.** `recordSuccess` removes the map entry, so four typos followed by
  a correct password leave no residue — occasional mistakes never accumulate into a lockout.

**And two limits (see also [§21](#21-known-limits-of-the-current-design)):**

- The counter is keyed by **email, not IP**. An attacker spraying one password across thousands of
  different emails is not slowed down by this, and conversely a hostile third party can lock a
  known user out of their own account for 15 minutes at a time.
- The map is **in-memory and per-instance**. Restarting the app clears every lockout, and two
  instances behind a load balancer each count separately. A shared store (Redis) would be needed to
  make this survive restarts or scale out.

---

## 6. Token refresh flow

```
POST /api/auth/refresh   { "refreshToken": "…" }
   │
   ▼ AuthServiceImpl.refresh
   │  ① jwtProvider.parse(token)
   │        null (bad signature / expired / garbage) → 401 "Invalid or expired refresh token"
   │  ② jwtProvider.isRefreshToken(claims)?
   │        no → 401 "Provided token is not a refresh token"
   │        ↑ an ACCESS token cannot be traded for a new pair
   │  ③ userRepository.findById(sub)
   │        gone → 401 "User no longer exists"
   │  ④ issue a brand-new access + refresh pair
   │
   ▼ 200 OK → AuthResponse
```

> **Why the refresh token carries no `role` claim:** the role is re-read from the database in
> step ③. That is what makes a promotion or demotion take effect at the next *refresh* (≤15 minutes
> in practice) instead of only at the next full login.

The mirror image, in `JwtAuthenticationFilter`, is just as important: a refresh token presented as
`Authorization: Bearer …` fails the `isAccessToken` check and is ignored, so a long-lived token can
never be used to call ordinary endpoints.

---

## 7. Authorization flow (RBAC)

Authorization is decided at three different heights, in this order:

```
① URL level      SecurityConfig.authorizeHttpRequests
                 "/api/admin/**" → hasRole("ADMIN")      → 403 before any controller code runs

② Method level   @PreAuthorize("hasRole('ADMIN')")
                 whole class:  AdminUserController
                 single method: CategoryController.create / update / delete
                 (reads on /api/categories stay public, so the annotation sits per-method)

③ Row level      inside the service — the only place that can know it
                 ItemServiceImpl.findOwnedItem      → item.user.id == caller?      else 403
                 MessageServiceImpl / ConversationServiceImpl.isParticipant        else 403
                 NotificationServiceImpl.markRead   → notification.user.id == caller? else 403
```

The role itself comes from the **access token's `role` claim** — no database lookup per request.
`Role.authority()` maps `ADMIN` → `ROLE_ADMIN`, which is the string `hasRole('ADMIN')` expects.
`Role.fromNameOrDefault` falls back to `USER` for anything unrecognised, so a token with a garbage
role claim degrades to the least privilege rather than blowing up.

**Consequence to remember:** because the role is inside a token that lives 15 minutes, a demoted
admin keeps admin powers until that token expires or is refreshed.

---

## 8. Profile flows

```
GET /api/users/me           → UserResponse       id, name, email, phone, photo, ratingAvg, role, createdAt
GET /api/users/{id}         → PublicUserResponse id, name, photo, ratingAvg, memberSince
                                                 ↑ NO email, NO phone, NO role — a different record,
                                                   so the private fields cannot leak by accident
PUT /api/users/me           → partial update
```

`UserServiceImpl.updateProfile` treats **`null` as "not sent"**: only non-null fields are applied.
A field that is present but blank is a different thing — `name: ""` is rejected with
400 "Name must not be blank", while `phone` and `profilePhotoUrl` are simply trimmed and stored.

---

## 9. Item flows

### 9.1 Create — `POST /api/items`

```
CreateItemRequest → ItemServiceImpl.create(userId, request)
   │
   ① CROSS-FIELD RULES FIRST — checked before any query, so a bad request costs zero DB round trips
   │     type == FOUND && rewardAmount != null     → 400 "rewardAmount is only allowed for LOST items"
   │     type == LOST  && storageLocation != null  → 400 "storageLocation is only allowed for FOUND items"
   │
   ② load User(userId)          → 404 "User not found"
   ③ load Category(categoryId)  → 404 "Category not found"
   │
   ④ new Item(user, category, type, name.trim())
   │     status defaults to OPEN
   │     photoUrls (≤10) go into the item_photo_urls collection table
   │     dateTime = when it was lost/found, NOT when the report was filed (that is createdAt)
   │
   ▼ 201 Created → ItemResponse (with nested owner summary + category)
```

### 9.2 Update — `PUT /api/items/{id}`

```
findOwnedItem(userId, itemId)
   │  item missing            → 404 "Item not found"
   │  item.user.id != caller  → 403 "You are not the owner of this item"
   ▼
every null field is skipped (partial update), and:
   name        blank → 400
   categoryId  re-resolved, missing → 404
   photoUrls   REPLACED, not appended — the client always sends the complete list
   rewardAmount   on a FOUND item → 400
   storageLocation on a LOST item → 400
```

### 9.3 Status change — `PATCH /api/items/{id}/status`

```
findOwnedItem(...) → item.status = OPEN | RETURNED | CLOSED → save
```

Any status may follow any other; there is no state machine. `RETURNED` is what makes a rating
socially meaningful, but nothing in the code enforces that ordering.

### 9.4 Delete — `DELETE /api/items/{id}`

```
findOwnedItem(...) → itemRepository.delete(item)
   │
   └─ the database cascades: item_photo_urls, saved_items, conversations
      (and through them messages), and ratings referencing this item are all removed
      via ON DELETE CASCADE in V1__init_schema.sql
```

### 9.5 Own items — `GET /api/items/my`

`itemRepository.findByUserId(userId, pageable)` — every status included, newest first.

---

## 10. Item search flow

`GET /api/items` is public and paged.

```
8 query parameters
type · status · categoryId · q · brand · color · dateFrom · dateTo
   │
   ▼ ItemController.search bundles them into ONE ItemSearchFilter record
   │
   ▼ ItemSpecifications.matching(filter) builds the WHERE clause dynamically:
   │
   │     type       → type = :type
   │     status     → status = :status
   │     categoryId → category.id = :categoryId
   │     brand      → lower(brand) LIKE %…%
   │     color      → lower(color) LIKE %…%
   │     q          → lower(name) LIKE %…%  OR  lower(description) LIKE %…%
   │     dateFrom   → dateTime >= :dateFrom
   │     dateTo     → dateTime <= :dateTo
   │
   │     only supplied filters contribute a predicate; the rest are skipped
   │     everything is joined with AND; an empty list means "match everything"
   │
   ▼ itemRepository.findAll(spec, pageable)      ← JpaSpecificationExecutor
   ▼ Page<Item> → map(ItemResponse::from) → PageResponse.from(...)
```

Default paging is `size=20`, `sort=createdAt,DESC`. A `?sort=` naming a field that does not exist
raises `PropertyReferenceException`, which the exception handler reports as
400 "Unknown sort property '…'".

> **Why a Specification and not a repository method:** eight optional filters would need 256 method
> names. One `Specification` covers every combination.

Indexes backing this search: `idx_items_status`, `idx_items_type`, `idx_items_category`,
`idx_items_date_time`, `idx_items_created_at`. The `q`/`brand`/`color` `LIKE %…%` predicates are
**not** index-backed — they are sequential scans, which is fine at this data size but is the first
thing to revisit if the items table grows large.

---

## 11. Saved item flow

```
POST /api/saved-items/{itemId}
   │
   ▼ SavedItemServiceImpl.save(userId, itemId)
   │
   ① already saved?  → return the existing row, 201, no error
   │      ↑ IDEMPOTENT on purpose: a double tap on the bookmark button must not fail
   │        and must not create a second row
   │
   ② load Item  → 404 "Item not found"
   ③ load User  → 404 "User not found"
   ④ save SavedItem(user, item)          uq_saved_items_user_item is the race-condition backstop
   │
   ⑤ if the saver is NOT the item's owner:
   │      notificationService.notify(item.owner, ITEM_SAVED,
   │                                "<saver name> saved your item \"<item name>\"")
   │      ↑ you are never notified about saving your own item
   │
   ▼ 201 → SavedItemResponse

DELETE /api/saved-items/{itemId}
   │  not in the list → 404 "Item is not in your saved list"
   │  otherwise       → deleted, 200. No notification: un-saving is nobody else's business.
```

---

## 12. Conversation flow

A conversation is a thread about **one item between exactly two users**.

```
POST /api/conversations   { "itemId": "…", "otherUserId": null }
   │
   ▼ ConversationServiceImpl.startOrGet(currentUserId, request)
   │
   ① load Item → 404
   │
   ② otherUserId == null ? item.user.id : otherUserId
   │      ↑ the common case — "talk about this item" means "talk to whoever reported it"
   │
   ③ otherUserId == currentUserId → 400 "You cannot start a conversation with yourself"
   │
   ④ conversationRepository.findByItemAndParticipants(itemId, me, them)
   │      JPQL that checks BOTH orderings, because (userA, userB) may be stored either way round:
   │        (a = me AND b = them) OR (a = them AND b = me)
   │      found → return it, no new row      ← "startOrGet", not "start"
   │
   ⑤ otherwise create Conversation(item, currentUser, otherUser)
   │
   ▼ ConversationResponse { id, item summary, userA summary, userB summary, createdAt }

GET /api/conversations       → every thread the caller is in, on either side (findAllForUser)
GET /api/conversations/{id}  → conversation.isParticipant(caller)?  no → 403
```

---

## 13. Message flow — REST and WebSocket

Both entry points converge on **one method**, which is what guarantees they behave identically.

```
REST                                       WEBSOCKET
POST /api/conversations/{id}/messages      SEND /app/conversations/{id}/send
   │                                          │
   ▼ MessageController.send                   ▼ ChatWebSocketController.send
   │  @AuthenticationPrincipal UUID            │  Principal ← set by AuthChannelInterceptor
   │  (from JwtAuthenticationFilter)           │  at CONNECT time, so the sender cannot be
   │                                           │  spoofed by the payload
   └──────────────┬────────────────────────────┘
                  ▼
      MessageServiceImpl.send(senderId, conversationId, request)   @Transactional
                  │
   ① text blank AND imageUrl blank → 400 "A message must contain text or an image"
   │      (either one alone is fine — an image-only message is valid)
   │
   ② findConversationForParticipant(conversationId, senderId)
   │      missing        → 404 "Conversation not found"
   │      not a member   → 403 "You are not a participant of this conversation"
   │
   ③ sender = whichever of userA / userB matches senderId
   │
   ④ save Message(conversation, sender, text.trim(), imageUrl.trim())
   │
   ⑤ BROADCAST  messagingTemplate.convertAndSend("/topic/conversations/{id}", response)
   │      → every subscriber of that topic receives it live, INCLUDING the sender's other devices,
   │        and including the REST case: posting over HTTP still pushes to the socket
   │
   ⑥ NOTIFY     notificationService.notify(conversation.otherParticipant(senderId),
   │                                       NEW_MESSAGE, "New message from <sender>")
   │      → a notifications row + an FCM push for the recipient only
   │
   ▼ REST: 201 with MessageResponse      WebSocket: nothing returned — the client already got ⑤
```

Reading history: `GET /api/conversations/{id}/messages` runs the same participant check, then pages
`messageRepository.findByConversationId`, backed by
`idx_messages_conversation_created (conversation_id, created_at)`.

---

## 14. WebSocket connection flow

```
Client                                    Server
  │  ws:// … /ws  (handshake)                │  SecurityConfig permits /ws/** at the HTTP level
  │─────────────────────────────────────────▶│  ↑ the handshake itself carries no token
  │                                          │
  │  STOMP CONNECT                           │
  │  Authorization: Bearer <accessToken>     │
  │─────────────────────────────────────────▶│ AuthChannelInterceptor.preSend
  │                                          │   header missing?           → REJECT
  │                                          │   parse() returns null?     → REJECT
  │                                          │   type != "access"?         → REJECT
  │                                          │     MessageDeliveryException
  │                                          │     "Missing or invalid JWT in STOMP CONNECT"
  │                                          │   valid → accessor.setUser(userId + authority)
  │  ◀───── CONNECTED ───────────────────────│
  │                                          │
  │  SUBSCRIBE /topic/conversations/{id}     │
  │─────────────────────────────────────────▶│  in-memory SimpleBroker (/topic, /queue)
  │                                          │
  │  SEND /app/conversations/{id}/send       │  /app = application destination prefix
  │─────────────────────────────────────────▶│  → ChatWebSocketController → §13
  │  ◀───── MESSAGE on /topic/… ─────────────│
```

The difference from HTTP is the important part: **`JwtAuthenticationFilter` never rejects, but
`AuthChannelInterceptor` always does.** There is no such thing as an anonymous chat session, and a
WebSocket has no headers after the handshake, so the token is checked exactly once, at CONNECT.

> The authentication is pinned at connect time, so a session stays authenticated with the identity
> it connected with even after that access token's 15-minute expiry passes. The client is expected
> to reconnect with a fresh token.

Note that the **subscribe** destination is not authorization-checked — the participant check lives
on the send path and on the REST history endpoint.

---

## 15. Notification and push flow

Every notification in the system goes through one method:

```
NotificationServiceImpl.notify(user, type, message)   @Transactional
   │
   ├─▶ ① notificationRepository.save(new Notification(user, type, message, read = false))
   │        the in-app feed row
   │
   └─▶ ② pushSender.sendToUser(user.id, title(type), message)
            │
            ├── FcmPushSender   → Firebase message to TOPIC "user-<uuid>"
            │      the client subscribes to its own topic after login, so the backend
            │      never stores device tokens
            │      any failure is caught and logged as a warning — a dead push must NEVER
            │      break the action that triggered it
            │
            └── NoopPushSender  → debug log only (Firebase not configured)
```

**Who triggers what:**

| Trigger | Type | Recipient | Push title |
| --- | --- | --- | --- |
| Someone saves your item | `ITEM_SAVED` | the item's owner (never yourself) | "Your item was saved" |
| Someone messages you | `NEW_MESSAGE` | the other participant | "New message" |
| Someone rates you | `NEW_RATING` | the rated user | "New rating" |
| — (unused so far) | `GENERAL` | — | "Lost &amp; Found" |

**Reading the feed:**

```
GET   /api/notifications             paged, newest first (size=20, sort=createdAt DESC by default)
PATCH /api/notifications/{id}/read   ownership check → 403 if it is not yours
PATCH /api/notifications/read-all    ONE bulk UPDATE (@Modifying JPQL), not a load-then-save loop
                                     → data: { "updated": <row count> }
```

---

## 16. Rating flow

```
POST /api/ratings   { "toUserId": "…", "itemId": "…", "score": 5, "comment": "…" }
   │                    score is @Min(1) @Max(5); the DB also has CHECK (score BETWEEN 1 AND 5)
   │
   ▼ RatingServiceImpl.submit(fromUserId, request)   @Transactional
   │
   ① fromUserId == toUserId          → 400 "You cannot rate yourself"
   ② load fromUser / toUser / item   → 404 each
   ③ existsByFromUserIdAndToUserIdAndItemId?
   │      yes → 409 "You have already rated this user for this item"
   │      ↑ ONE rating per (rater, rated, item) triple — also enforced by
   │        uq_ratings_from_to_item, so a race cannot slip a duplicate through
   │
   ④ save Rating(fromUser, toUser, item, score, comment)
   │
   ⑤ toUser.ratingAvg = ratingRepository.averageScoreFor(toUser.id)
   │      "select coalesce(avg(r.score), 0) …"  — coalesce turns SQL's NULL-for-empty-set into 0
   │      the average is CACHED on the user row so profile pages never aggregate on read
   │
   ⑥ notificationService.notify(toUser, NEW_RATING,
   │       "<rater> rated you 5/5 for \"<item>\"")
   │
   ▼ 201 → RatingResponse

GET /api/ratings/user/{userId}   public, paged
   │  user does not exist → 404
   │  user exists with no ratings → 200 with an empty page
   │  ↑ the explicit existsById check is what keeps those two cases distinguishable
```

Nothing forces the item to be `RETURNED` first, and nothing checks that the two users ever talked.
The rule enforced is only "one rating per rater, per rated user, per item".

---

## 17. Category flow

```
GET  /api/categories        public — every category, sorted by name
GET  /api/categories/{id}   public — 404 if missing

POST   /api/categories      @PreAuthorize ADMIN
   │  existsByNameIgnoreCase → 409 "Category with this name already exists"
   │  → 201

PUT    /api/categories/{id} @PreAuthorize ADMIN
   │  renaming to a different CASE of its own name is allowed
   │  clashing with a DIFFERENT category's name → 409

DELETE /api/categories/{id} @PreAuthorize ADMIN
   │  category missing → 404
   │  items still reference it → the FK rejects the delete →
   │      DataIntegrityViolationException → 409 "Request conflicts with existing data"
```

The 13 seeded categories from `V2__seed_categories.sql` have **fixed UUIDs**, so a client can
hard-code a category id and have it work in every environment.

---

## 18. Admin user management flow

Everything below is behind two independent gates: `hasRole("ADMIN")` on `/api/admin/**` in
`SecurityConfig`, *and* `@PreAuthorize("hasRole('ADMIN')")` on `AdminUserController` itself.

```
GET /api/admin/users?keyword=dara
   │  keyword empty → findAll(pageable)
   │  otherwise     → findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable)
   │  → UserResponse rows (email and role included — this is the admin view)

GET /api/admin/users/{id}   → UserResponse, 404 if missing

PATCH /api/admin/users/{id}/role   { "role": "ADMIN" }
   │
   ▼ AdminUserServiceImpl.updateRole(actingAdminId, userId, role)
   │
   ① user already has that role → return unchanged, no guard needed, no write
   ② userId == actingAdminId    → 400 "You cannot change your own role"
   ③ demoting an ADMIN when countByRole(ADMIN) <= 1 → 400 "Cannot demote the last remaining admin"
   ④ save
   │
   └─ the affected user KEEPS their old permissions until their next login or token refresh,
      because the role is baked into the access token when it is issued
```

Guards ② and ③ exist for one reason: to make it impossible to lock every administrator out of the
admin area. `AdminBootstrap` is the recovery path if it ever happens anyway.

---

## 19. Error flow

```
anything throws inside a controller or service
   │
   ▼ GlobalExceptionHandler  (@RestControllerAdvice — applies to every controller)
   │
   ▼ ResponseEntity<ApiResponse<…>>  with success = false
```

| Exception | Status | Response `message` |
| --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | `"Validation failed"`, `data` = `{ field: reason, … }` for **every** bad field |
| `ConstraintViolationException` | 400 | same shape, for validated method parameters |
| `HttpMessageNotReadableException` | 400 | "Malformed request body or missing required fields" |
| `MethodArgumentTypeMismatchException` | 400 | "Invalid value 'x' for parameter 'y'" |
| `MissingServletRequestParameterException` | 400 | "Missing required parameter 'x'" |
| `PropertyReferenceException` | 400 | "Unknown sort property 'x'" |
| `BadRequestException` | 400 | the thrown message |
| `UnauthorizedException` / `BadCredentialsException` | 401 | the thrown message / "Invalid credentials" |
| `ForbiddenException` / `AccessDeniedException` | 403 | the thrown message / "Access denied" |
| `NotFoundException` / `NoResourceFoundException` | 404 | the thrown message / "Resource not found" |
| `HttpRequestMethodNotSupportedException` | 405 | "Method not allowed" |
| `ConflictException` | 409 | the thrown message |
| `DataIntegrityViolationException` | 409 | "Request conflicts with existing data" (real cause logged) |
| `TooManyRequestsException` | 429 | "Too many failed attempts. Try again in N minute(s)." |
| anything else | 500 | "An unexpected error occurred" — **stack trace logged, never sent** |

**Two errors bypass this handler entirely**, because they happen before any controller runs:
Spring Security's 401 and 403, which `SecurityConfig` writes by hand into the same envelope so the
client sees one consistent shape either way.

---

## 20. Deployment flow

```
git push origin main
   │
   ├─▶ ci.yaml            ./mvnw -B verify on Temurin 21
   │                      failure → uploads target/surefire-reports as an artifact
   │
   └─▶ deploy.yml
         │
         ① docker/build-push-action builds the two-stage Dockerfile
         │     stage 1  maven:3.9-eclipse-temurin-21 → dependency:go-offline → package -DskipTests
         │     stage 2  eclipse-temurin:21-jre-alpine, non-root user "app", only the jar copied
         │     tags: ghcr.io/<owner>/<repo>:latest  and  :<git sha>
         │     GitHub Actions layer cache (type=gha) for both read and write
         │
         ② SSH to EC2 (appleboy/ssh-action)
         │     cd ~/lost-and-found-backend
         │     docker compose pull app
         │     docker compose up -d --force-recreate app
         │
         ③ poll http://localhost:8080/actuator/health, 20 tries × 5 s = 100 s
         │     UP    → docker image prune -f, exit 0
         │     never → docker logs --tail 100 lostfound-java-app, exit 1
         ▼
On the box: db (postgres:16-alpine, 256 MB, pgdata volume) + app (600 MB,
published on 127.0.0.1:8080 only — a reverse proxy is expected in front),
SPRING_PROFILES_ACTIVE=docker, MaxRAMPercentage=65, SerialGC.
```

On every container start the flow from [§2](#2-application-startup-flow) repeats: Flyway applies
any new migration, Hibernate validates, `AdminBootstrap` re-checks the admin account.

---

## 21. Known limits of the current design

Documented so they are deliberate choices rather than surprises.

| Area | Limit | Consequence / what would fix it |
| --- | --- | --- |
| Login throttle | In-memory `ConcurrentHashMap`, keyed by **email only** | Lockouts vanish on restart and are per-instance; a third party can lock a known email out; password-spraying across many emails is unaffected. → Redis, plus an IP-based counter. |
| WebSocket broker | Spring's in-memory `SimpleBroker` | Subscribers on instance A never see messages published on instance B. → RabbitMQ/ActiveMQ relay. |
| Message broadcast | `convertAndSend` runs **inside** the `@Transactional` method, before commit | If the transaction rolled back after the broadcast, subscribers would have seen a message that was never stored. → publish after commit. |
| Refresh tokens | Stateless, not stored, not revocable | A stolen refresh token stays valid for its full 7 days; logout is client-side only. → a token store or a denylist. |
| Role propagation | Role is a claim inside a 15-minute access token | A demoted admin keeps admin rights until the token expires. Accepted trade-off for not hitting the DB on every request. |
| Item search | `LIKE %…%` on name/description/brand/colour | Sequential scans, no index. → PostgreSQL full-text search or a trigram index. |
| Push delivery | FCM **topic** `user-<uuid>` per user | No device tokens are stored (simple), but a client that subscribes to another user's topic would receive their pushes. → per-device tokens. |
| Rating eligibility | Any user may rate any other about any item | Only the "one per (rater, rated, item)" rule is enforced; there is no check that the item was actually returned or that the two ever talked. |
| `MockController` | `GET /api/auth/mockapi` returns a plain string | A CI/CD smoke-test endpoint that sits under the `permitAll` `/api/auth/**` prefix and does not use the response envelope. Remove it once deployment is settled. |
