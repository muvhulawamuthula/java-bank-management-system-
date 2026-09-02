# Bank Africa Management System

Stateless JWT REST API for deposits, withdrawals, transfers and an immutable transaction ledger — Spring Boot 3.5, Java 21, Flyway, OpenAPI.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Java](https://img.shields.io/badge/Java-21-orange)
![Security](https://img.shields.io/badge/Auth-JWT%20%2B%20BCrypt-blue)
![Migrations](https://img.shields.io/badge/Schema-Flyway-red)
![API Docs](https://img.shields.io/badge/API-OpenAPI%203-85ea2d)
![SWIFT](https://img.shields.io/badge/SWIFT-MT103-yellow)
![Tests](https://img.shields.io/badge/tests-146%20passing-success)

## What it does

Users register and log in to receive a bearer token. Every money operation runs against the **authenticated user's own account** (resolved from the JWT, never from a client-supplied id). Balance changes are locked, ledgered in the same DB transaction, and optionally protected by an `Idempotency-Key` so retries cannot double-charge.

| Concern | Approach |
|---|---|
| Passwords | BCrypt via Spring Security |
| AuthN | Stateless JWT (HS256); no sessions |
| AuthZ / IDOR | Account id taken from the token only |
| Concurrency | Pessimistic `SELECT … FOR UPDATE` + `@Version` |
| Transfers | Deterministic lock order; atomic debit + both ledger legs |
| Schema | Flyway owns it (`DDL_AUTO=none`); never Hibernate in prod |
| Idempotency | `UNIQUE (account_id, key)` + request fingerprint → at-most-once |
| Brute force | Per-IP token bucket on `/login` and `/register` → `429` |

## Docs

- **[Architecture](docs/architecture.md)** — layers, security filter chain, locking, idempotency, migrations, package map
- **[API reference](docs/api.md)** — endpoints, DTOs, error envelope, curl examples
- **Live OpenAPI** — Swagger UI at `/swagger-ui.html` (Authorize with JWT); spec at `/v3/api-docs`

## Stack

- Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation, Security, Actuator)
- JJWT · Flyway · springdoc-openapi · MySQL 8 (prod) / H2 (dev + tests)
- JUnit 5 · Mockito · MockMvc · Testcontainers (MySQL)
- Vanilla HTML/CSS/JS SPA served by the app

## Quick start

**Dev (in-memory H2, no DB setup):**

```bash
cd bankapp
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Open <http://localhost:8080> or <http://localhost:8080/swagger-ui.html>.

**MySQL via Docker Compose** (host port **3307**):

```bash
cd bankapp
docker compose up -d
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DDB_URL=jdbc:mysql://localhost:3307/bank_africa_db"
```

Flyway applies `V1`–`V3` on boot. Tear down with `docker compose down` (add `-v` to wipe data).

### Environment

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local MySQL | datasource |
| `JWT_SECRET` | dev-only fallback | **override in production** (≥ 32 bytes) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | token lifetime |
| `CORS_ALLOWED_ORIGINS` | localhost :8080/:5173/:3000 | comma-separated |
| `RATELIMIT_AUTH_CAPACITY` | `10` | per-IP bucket size on auth endpoints |
| `RATELIMIT_AUTH_REFILL_SECONDS` | `60` | bucket refill period |
| `SWIFT_BIC` / `SWIFT_BANK_NAME` | `BANKZAJJXXX` / `BANK AFRICA` | MT103 sender fields |

## API at a glance

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/auth/register` | public | creates user + account; returns JWT |
| `POST` | `/api/auth/login` | public | returns JWT |
| `GET` | `/api/auth/me` | JWT | current profile |
| `GET` | `/api/account` | JWT | account snapshot |
| `POST` | `/api/account/deposit` | JWT | optional `Idempotency-Key` |
| `POST` | `/api/account/withdraw` | JWT | optional `Idempotency-Key` |
| `POST` | `/api/account/transfer` | JWT | by 10-digit account number; optional key |
| `GET` | `/api/account/transactions` | JWT | paged ledger (`page`, `size` ≤ 100) |
| `GET` | `/api/account/transactions/{id}/swift` | JWT | SWIFT MT103 for a transfer |

Full request/response shapes, status codes and examples: **[docs/api.md](docs/api.md)**.

## Testing

```bash
cd bankapp
./mvnw test
```

**146** tests (counting parameterized expansions): models, repositories, services (including concurrency / overdraw), controllers, JWT auth, idempotency, SWIFT MT103, auth rate limiting, Flyway migrations on H2 (MySQL-compat), and a Testcontainers MySQL integration test that **skips** when Docker is unavailable so CI stays green without a daemon.

## Project layout

```
bankapp/
  src/main/java/com/bankafrica/bankingapp/
    controller/   REST endpoints
    service/      domain logic, idempotency, SWIFT
    security/     JWT filter, rate limit, SecurityConfig
    repository/   Spring Data JPA
    model/        entities
    dto/          request/response records
    exception/    ApiError + GlobalExceptionHandler
  src/main/resources/db/migration/   Flyway V1–V3
  docker-compose.yml                 MySQL 8 for local prod-like runs
docs/
  architecture.md
  api.md
```

## Contributors

- muthula muvhulawa
