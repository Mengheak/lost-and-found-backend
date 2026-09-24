# Deployment guide

This guide deploys the Lost & Found backend as a single Spring Boot container with PostgreSQL on a Linux server. It follows the existing [Dockerfile](Dockerfile), [Compose configuration](docker-compose.yml), and [GitHub Actions deployment workflow](.github/workflows/deploy.yml). Commands below use Bash and run from the deployment directory unless stated otherwise.

The application uses Java 21, Spring Boot 3.5.6, and PostgreSQL 16. The Docker build supplies Java and Maven; neither needs to be installed on the deployment host. This repository deploys the API only; deploy the frontend separately.

## 1. Prepare the server

Install Docker Engine with the Docker Compose v2 plugin, Git, curl, and OpenSSL. Confirm that the deployment user can run Docker without an interactive sudo prompt:

```bash
docker version
docker compose version
```

The included workflow expects an Ubuntu server, SSH user `ubuntu`, and the directory `~/lost-and-found-backend`. Use that directory if you plan to enable automatic deployment:

```bash
git clone https://github.com/<owner>/<repo>.git ~/lost-and-found-backend
cd ~/lost-and-found-backend
cp .env.example .env
chmod 600 .env
```

Replace `<owner>/<repo>` with your repository. Private repositories also require Git authentication when cloning. On an existing installation, preserve its `.env` instead of copying over it.

Compose limits the app to 600 MB and PostgreSQL to 256 MB. Allow additional host memory for the operating system, Docker, and reverse proxy; local image builds need additional capacity. The current image workflow builds on an Ubuntu runner without a multi-platform configuration, so use an x86-64 host for its images or explicitly add a build for your host's architecture.

## 2. Configure the environment

Edit `.env` on the server. Compose reads this file automatically. Add `GH_REPO` and `IMAGE_TAG`, which are not included in `.env.example`:

```dotenv
GH_REPO=your-owner/your-repository
IMAGE_TAG=latest
DB_NAME=lostfound
DB_USER=lostfound
DB_PASSWORD=replace-with-a-strong-unique-password
DB_PORT=5432
APP_PORT=8080
JWT_SECRET=replace-with-a-generated-secret
CORS_ALLOWED_ORIGINS=https://your-frontend.example.com
```

Use a lowercase `GH_REPO` without the `ghcr.io/` prefix. Generate a JWT secret with `openssl rand -hex 32` and paste the output into `.env`. The application requires at least 32 bytes. Keep it stable across restarts; changing it invalidates existing JWTs. Use a separate strong database password. Single-quote dotenv values containing literal `$` characters to prevent Compose interpolation. Do not commit `.env` or share rendered Compose output containing secrets.

| Setting | Default / behavior |
| --- | --- |
| `GH_REPO` | Required; image becomes `ghcr.io/<GH_REPO>:<IMAGE_TAG>`. |
| `IMAGE_TAG` | `latest`; use a published full commit SHA for a pinned manual release. |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Required by Compose; the same account is used by PostgreSQL, Flyway, and the app. |
| `DB_PORT` | Host loopback port, default `5432`. The app always reaches `db:5432` inside Compose. |
| `APP_PORT` | Host loopback port, default `8080`. The app listens on `8080` inside its container. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins including scheme and port, with no path. Defaults to local development origins. |
| `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL` | `15m`, `7d`. |
| `TOKEN_CLEANUP_CRON` | `"0 0 * * * *"`; hourly cleanup using a six-field Spring cron expression. |
| `LOGIN_MAX_ATTEMPTS` | `5`. |
| `LOGIN_LOCKOUT`, `LOGIN_ATTEMPT_WINDOW` | Both `15m`. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional initial administrator; set both to enable bootstrap. Password must be at least 12 characters. |
| `ADMIN_NAME` | `Administrator`. |
| `ADMIN_RESET_PASSWORD` | `false`; enable only for one startup to reset an existing admin password. |
| `FIREBASE_CREDENTIALS` | Empty disables push notifications; otherwise a readable path inside the container. |

An existing regular user is never promoted by administrator bootstrap. Use an unused email or an existing administrator's email. After initial setup, clear `ADMIN_PASSWORD` and `ADMIN_EMAIL`, keep `ADMIN_RESET_PASSWORD=false`, and recreate the app to remove the bootstrap credentials from its environment. Existing accounts remain in PostgreSQL.

The Compose file explicitly passes selected variables into the app. Adding an arbitrary Spring environment variable or `DB_URL` to `.env` alone does not pass it through; add it to the app's `environment` configuration as well. A direct Java process also does not automatically read `.env`.

## 3. Obtain an image and start the services

### Use a published GHCR image

Ensure the selected image tag exists. For a private image, authenticate on the server as the same user that will run deployments:

```bash
read -rsp 'GHCR read token: ' GHCR_TOKEN
printf '\n'
printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u <github-user> --password-stdin
unset GHCR_TOKEN
```

Use a token with permission to read the package (`read:packages` for a classic GitHub personal access token). Public images do not require this login.

```bash
docker compose config --quiet
docker compose pull
docker compose up -d
```

### Build the image yourself

Compose has no `build:` section. To deploy a local build, set `GH_REPO=local/lost-and-found-java` and `IMAGE_TAG=latest` in `.env`, then run:

```bash
docker build -t ghcr.io/local/lost-and-found-java:latest .
docker compose pull db
docker compose up -d --pull never
```

The Dockerfile skips tests. Before publishing or deploying a source change, run `./mvnw -B verify` on a build machine with Java 21 and Docker available for Testcontainers (`.\mvnw.cmd -B verify` in Windows PowerShell).

### Verify startup

```bash
docker compose ps
docker compose logs --tail=100 app db
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
curl --fail --silent --show-error http://127.0.0.1:8080/api/categories
```

Adjust the host port if `APP_PORT` differs. Allow time for PostgreSQL initialization, migrations, and JVM startup. Health should return `"status":"UP"`; categories should return a successful API response. Swagger UI is at `/swagger-ui.html`, and OpenAPI JSON is at `/v3/api-docs`.

Flyway automatically applies `src/main/resources/db/migration` on startup. Hibernate then validates the schema. Do not manually apply these SQL files or edit migrations that have already run. Migration V5 revokes existing sessions when upgrading from the older token schema, so those users must sign in again.

## 4. Expose the API over HTTPS

Both published ports bind to `127.0.0.1`. Place a reverse proxy on the same host, terminate HTTPS there, and forward requests to `http://127.0.0.1:8080` (or your configured `APP_PORT`). Configure DNS and a valid TLS certificate for your API hostname before directing clients to it.

Configure the proxy to:

- Preserve request paths, including `/api`, `/ws`, and the documentation routes if exposed.
- Forward the `Host`, `X-Forwarded-For`, and `X-Forwarded-Proto` headers.
- Support HTTP/1.1 WebSocket upgrades for `/ws` and an idle timeout suitable for chat sessions.
- Redirect HTTP to HTTPS and restrict public access to management routes as appropriate.

For EC2, allow inbound HTTPS (443), HTTP (80) if needed for redirects or certificate issuance, and SSH (22) from authorized deployment/admin sources. Ports 8080 and 5432 do not need public access. The Actions runner must be able to reach SSH for automated deployment.

Verify `https://<api-host>/api/categories` and test frontend login and chat. Browser chat uses `wss://<api-host>/ws` with `Authorization: Bearer <access-token>` in the STOMP CONNECT headers. REST CORS uses `CORS_ALLOWED_ORIGINS`; the current WebSocket configuration separately allows all origin patterns.

This deployment runs one app instance. Chat uses an in-memory broker, so multiple replicas would require changes to coordinate message delivery.

## 5. Enable GitHub Actions deployment

The existing workflow runs on pushes to `main`. It builds and publishes two GHCR tags: `latest` and the full commit SHA. It then connects to the server and recreates the app.

Set these repository Actions secrets:

| Secret | Value |
| --- | --- |
| `EC2_HOST` | Server hostname or IP reachable from the Actions runner. |
| `EC2_SSH_KEY` | Private SSH key whose public key authorizes login as `ubuntu`. |

Complete the initial server setup and database startup first. The server needs its own GHCR authentication for private packages; the workflow's `GITHUB_TOKEN` login only authenticates the build runner.

Current workflow behavior to account for:

- It always uses `ubuntu`, `~/lost-and-found-backend`, and `http://localhost:8080/actuator/health`. Keep `APP_PORT=8080` or update the workflow's health URL.
- It deploys whatever `IMAGE_TAG` the server `.env` selects. Use `latest` for the existing automatic flow; pinning a SHA on the server keeps deploying that SHA, even after later pushes.
- It does not fetch repository changes, copy Compose files, update `.env`, or configure the proxy. Synchronize deployment configuration separately before releases that depend on it.
- It polls health up to 20 times with five-second waits. Failure prints app logs and fails the job; it does not automatically roll back.
- The separate CI workflow runs Maven verification, but deployment does not depend on that workflow succeeding. Require CI before merging, or add an explicit test dependency if deployment must wait for tests.

For reproducible automated releases, update the workflow to pass the built commit SHA into Compose rather than relying on the mutable `latest` tag.

## 6. Updates and rollback

Back up the database before a release with schema changes. Record the currently deployed image before replacing it:

```bash
docker inspect lostfound-java-app --format '{{.Config.Image}} {{.Image}}'
```

For a manual release, set `IMAGE_TAG` in `.env` to the published full commit SHA, then run:

```bash
docker compose pull app
docker compose up -d --no-deps --force-recreate app
docker compose ps
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

App replacement causes a brief interruption and disconnects WebSocket clients. Recreate the app after environment changes; `docker compose restart` does not load a changed environment.

To roll back, select the previous known-good SHA and repeat the release commands. Confirm that the old application supports the migrated database first. Flyway does not undo migrations when an older image starts. If a database restore is necessary, stop application writes and follow a tested recovery procedure; restoring a backup loses writes made after that backup. Pause automatic deployments during recovery so a new push does not replace the chosen version.

## 7. Database persistence and backups

PostgreSQL data lives in the Compose `pgdata` named volume. Keep the deployment directory/project name stable so Compose continues to select the same volume. `docker compose down` preserves it; **`docker compose down -v` deletes it and its data**.

Create a backup on the Linux server:

```bash
mkdir -p backups
chmod 700 backups
backup_file="backups/lostfound-$(date -u +%Y%m%dT%H%M%SZ).dump"
(umask 077; docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup_file")
```

Check the command's exit status before treating the file as a successful backup. Store encrypted copies off the server and test restoration into a separate PostgreSQL 16 database. The archive can be restored with `pg_restore`; do not run a destructive restore against production without a recovery plan. Preserve server configuration and secrets separately from database backups.

Changing `DB_PASSWORD` in `.env` does not change the password inside an initialized database. Coordinate credential changes in PostgreSQL and the app configuration. Do not delete the volume to resolve an authentication error.

## 8. Optional Firebase push notifications

Keep the service-account JSON outside the repository, for example `/home/ubuntu/.config/lostfound/firebase-service-account.json`. Create a server-local `compose.override.yaml` beside `docker-compose.yml`:

```yaml
services:
  app:
    volumes:
      - /home/ubuntu/.config/lostfound/firebase-service-account.json:/run/secrets/firebase.json:ro
```

Set `FIREBASE_CREDENTIALS=/run/secrets/firebase.json` in `.env`, ensure the container's non-root `app` user can read the mounted file, and recreate the app. Check startup logs and send a test notification to confirm configuration. Keep the override server-local. Leaving credentials empty runs without push notifications.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Compose reports a required variable is missing | Run from the directory containing `.env`; supply `GH_REPO`, database values, and `JWT_SECRET`. |
| GHCR reports denied or manifest unknown | Check lowercase image name, published tag, package permissions, and server-side Docker login. |
| App is unhealthy or exits | Inspect `docker compose logs --tail=200 app db` for JWT validation, database, migration, bootstrap, or memory errors. |
| Database password authentication fails after editing `.env` | Existing database credentials remain unchanged; update them in PostgreSQL as well. |
| Flyway checksum or schema validation fails | Compare the deployed image and migration history; restore the correct migration files instead of deleting schema/history. |
| Local health works but public requests fail | Check DNS, TLS, firewall/security group rules, proxy upstream port, and proxy logs. |
| Browser requests fail while curl works | Check the exact frontend origin in `CORS_ALLOWED_ORIGINS`, then recreate the app. |
| Chat fails through the proxy | Check WebSocket upgrade forwarding, `/ws`, STOMP authentication, and timeouts. |
| A successful push deploys an older release | Check the server's `IMAGE_TAG`; the workflow does not override a pinned tag. |
| Admin bootstrap prevents startup | Check email conflicts, password length, and whether an existing legacy admin requires a one-time explicit password reset. |
