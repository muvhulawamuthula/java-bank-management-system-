# Bank Africa Management System

A full-stack banking application: users register, log in, and manage a bank account —
deposits, withdrawals, transfers and a full transaction history — through a modern web UI
backed by a secure, stateless REST API.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Java](https://img.shields.io/badge/Java-21-orange)
![Security](https://img.shields.io/badge/Auth-JWT%20%2B%20BCrypt-blue)
![Migrations](https://img.shields.io/badge/Schema-Flyway-red)
![API Docs](https://img.shields.io/badge/API-OpenAPI%203-85ea2d)
![SWIFT](https://img.shields.io/badge/SWIFT-MT103-yellow)
![Tests](https://img.shields.io/badge/tests-146%20passing-success)

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Security & Correctness](#security--correctness)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [Running Locally](#running-locally)
- [Testing](#testing)

## 🔍 Overview

Bank Africa is a banking backend and single-page UI. Authentication is token-based (JWT):
clients register or log in to receive a bearer token and present it on every subsequent
request. All money operations act on the **authenticated user's own account** — the account
is resolved from the token, never from a client-supplied id — and every balance change is
written to an immutable transaction ledger inside the same database transaction.

## ✨ Features

- **Authentication** — register and log in; passwords are hashed with BCrypt and a signed
  JWT is issued. A protected `/api/auth/me` returns the current user's profile.
- **Account operations** — view balance, deposit, withdraw, and transfer to another account
  by account number. Each operation returns the updated account.
- **Transaction history** — every deposit, withdrawal and transfer leg is recorded with the
  amount, the resulting balance and (for transfers) the counterparty account.
- **Validation & errors** — request bodies are validated; all failures return a single,
  consistent JSON error envelope (never a stack trace or HTML page).
- **Idempotent money movement** — deposit/withdraw/transfer accept an optional
  `Idempotency-Key` header; a retried request replays the original result instead of moving
  money twice, so a network timeout-and-retry can never double-charge an account.
- **SWIFT MT103** — any transfer can be rendered as a standards-shaped ISO 15022 MT103
  (Single Customer Credit Transfer) message.
- **Interactive API docs** — OpenAPI 3 with a live Swagger UI at `/swagger-ui.html`
  (with an Authorize button for JWT), so every endpoint is browseable and executable.
- **Brute-force defense** — per-IP rate limiting on the login/register endpoints (`429` past
  a threshold).

## 🔒 Security & Correctness

This is what separates the app from a typical demo:

| Concern | How it's handled |
|---|---|
| Password storage | **BCrypt** hashing via Spring Security; raw passwords are never persisted. |
| Authentication | **Stateless JWT** bearer tokens (HS256); no server sessions. |
| Authorization | Operations target the caller's own account, resolved from the JWT — closes the IDOR hole where any caller could deposit to / drain any account by id. |
| Concurrency | Balance changes load the row with a **pessimistic write lock** (`SELECT … FOR UPDATE`) plus an optimistic `@Version`, so concurrent withdrawals can't overdraw or lose updates. |
| Auditability | An **immutable ledger** row is written for every movement, in the same transaction as the balance change. |
| Transfers | Both accounts are locked in a deterministic id order to avoid deadlocks; the debit and both ledger legs are atomic. |
| Configuration | DB credentials and the JWT secret are read from **environment variables**; CORS is restricted to configured origins. |
| Schema | Managed by **Flyway** versioned migrations — never by `hibernate.ddl-auto` in production. Hibernate is set to `validate`/`none` so the database, not the app, owns the schema, and every change is reviewable, repeatable and auditable. |
| Idempotency | Money operations honour an **`Idempotency-Key`**; the key + a request fingerprint + the response are stored, and a `UNIQUE (account_id, key)` constraint serialises concurrent retries so an operation executes **at most once**. Reuse with different parameters → `409`. |
| Brute force | Per-IP **token-bucket rate limiting** on `/login` and `/register`, returning `429` (in the same error envelope) before authentication runs. |

## 🛠️ Technology Stack

- **Java 21**, **Spring Boot 3.5** (Web, Data JPA, Validation, Actuator)
- **Spring Security** + **JJWT** for stateless JWT auth
- **Hibernate / JPA** with pessimistic + optimistic locking
- **Flyway** for versioned, repeatable schema migrations (production schema source of truth)
- **springdoc-openapi** for OpenAPI 3 + Swagger UI
- **MySQL** in production, **H2** for the dev profile and tests
- **JUnit 5 + Mockito + MockMvc + Testcontainers** (146 tests: unit, repository, service-concurrency,
  migration, idempotency, SWIFT, rate-limiting, real-MySQL integration, and end-to-end)
- Vanilla **HTML/CSS/JS** single-page frontend served by Spring Boot

## 🏗️ Architecture

```
Browser (static SPA)
   │  Authorization: Bearer <JWT>
   ▼
Controllers ── DTO validation ──► Services ──► Repositories ──► Database
   │                                 │
   └── GlobalExceptionHandler        ├── BCrypt password hashing
       (uniform ApiError JSON)       ├── pessimistic row locks
                                     └── transaction ledger
```

## 📡 API Documentation

Interactive docs (try every endpoint from the browser, with an **Authorize** button for the
JWT) are served at **`/swagger-ui.html`**; the raw OpenAPI 3 spec is at `/v3/api-docs`.

All errors return:

```json
{ "timestamp": "...", "status": 422, "error": "Unprocessable Entity",
  "message": "Insufficient funds. Current balance: R750.00", "path": "/api/account/withdraw" }
```

Validation failures additionally include a `fieldErrors` map.

### Auth (public)

| Method | Path | Body | Result |
|---|---|---|---|
| POST | `/api/auth/register` | `firstName, lastName, email, idNumber(13), phoneNumber(10), password(≥6), initialDeposit(≥100)` | `201` + `{ token, userId, accountNumber, balance, … }` |
| POST | `/api/auth/login` | `email, password` | `200` + `{ token, … }` |

### Account (require `Authorization: Bearer <token>`)

| Method | Path | Body | Result |
|---|---|---|---|
| GET  | `/api/auth/me` | — | current user's profile |
| GET  | `/api/account` | — | account snapshot |
| POST | `/api/account/deposit` | `{ amount }` | updated account |
| POST | `/api/account/withdraw` | `{ amount }` | updated account (422 if insufficient) |
| POST | `/api/account/transfer` | `{ toAccountNumber, amount, description? }` | updated source account |
| GET  | `/api/account/transactions?page=&size=` | — | paged ledger envelope (`content`, `totalElements`, …), newest first |
| GET  | `/api/account/transactions/{id}/swift` | — | SWIFT **MT103** for that transfer transaction |

The three money endpoints accept an optional **`Idempotency-Key`** header: a retry with the
same key replays the original response (reuse with different parameters → `409`). The ledger
is **paginated** (`page` default 0, `size` default 20, max 100).

### Example

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Mac","lastName":"M","email":"mac@example.com","idNumber":"9001015000000",
       "phoneNumber":"0712345678","password":"securepass","initialDeposit":500.00}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -s -X POST localhost:8080/api/account/deposit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"amount":250.00}'
```

## 🚀 Running Locally

### Quick start (no database setup)

The `dev` profile runs against an in-memory H2 database:

```bash
cd bankapp
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Then open <http://localhost:8080>.

### Production (MySQL)

Create the database, then provide configuration via environment variables:

```sql
CREATE DATABASE bank_africa_db;
CREATE USER 'bank_user'@'localhost' IDENTIFIED BY 'bank_password';
GRANT ALL PRIVILEGES ON bank_africa_db.* TO 'bank_user'@'localhost';
FLUSH PRIVILEGES;
```

```bash
export DB_URL="jdbc:mysql://localhost:3306/bank_africa_db"
export DB_USERNAME=bank_user
export DB_PASSWORD=bank_password
export JWT_SECRET="$(openssl rand -base64 48)"   # must be ≥ 32 bytes
./mvnw spring-boot:run
```

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local MySQL | datasource |
| `JWT_SECRET` | dev-only fallback | **set this in production** (≥ 32 bytes) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | token lifetime |
| `CORS_ALLOWED_ORIGINS` | localhost dev ports | comma-separated allowed origins |

### MySQL via Docker Compose (no local MySQL needed)

`bankapp/docker-compose.yml` provisions a MySQL 8 instance with the database and user
already created. It maps host port **3307** so it coexists with any MySQL you already run
on 3306, and waits until the server is healthy before reporting ready.

```bash
cd bankapp
docker compose up -d                       # start MySQL (waits until healthy)

export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DDB_URL=jdbc:mysql://localhost:3307/bank_africa_db"
```

On boot, Flyway applies `V1` then `V2` against the fresh schema (watch the log for
`Successfully applied 2 migrations … now at version v2`). Tear down with:

```bash
docker compose down       # stop and remove the container
docker compose down -v    # …and also wipe the data volume for a clean slate
```

The default `DB_USERNAME`/`DB_PASSWORD` (`bank_user` / `bank_password`) already match the
compose credentials, so only `DB_URL` (for the 3307 port) and `JWT_SECRET` need overriding.

### Database migrations (Flyway)

On a production (MySQL) boot, **Flyway runs automatically before the app accepts traffic**,
applying any new versioned scripts in `src/main/resources/db/migration/` and recording them in
the `flyway_schema_history` table. Hibernate never alters the schema (`DDL_AUTO=none`), so the
database is governed entirely by reviewed migrations.

Current migrations:

| Version | Script | What it does |
|---|---|---|
| `V1` | `V1__initial_schema.sql` | Baseline schema: `bank_account`, `users`, `transactions`, their FKs, unique constraints and the ledger index. |
| `V2` | `V2__balance_non_negative_check.sql` | Adds a `CHECK (balance >= 0)` constraint — a database-level backstop ensuring no account can ever be stored negative. |
| `V3` | `V3__idempotency_keys.sql` | Adds the `idempotency_key` table (with `UNIQUE (account_id, idempotency_key)`) backing idempotent money operations. |

- **Naming**: `V<n>__<description>.sql` (e.g. `V1__initial_schema.sql`). Versions apply in order.
- **Immutability**: never edit an applied migration — Flyway checksums them and will refuse to
  start if one changed. Add a new `V<n+1>__…` script for every schema change.
- **Adopting an existing database**: `spring.flyway.baseline-on-migrate=true` lets Flyway take
  over a pre-existing schema by baselining it.
- **Verification**: `FlywayMigrationTest` applies the real migrations to H2 in MySQL-compatibility
  mode, registers a user end-to-end, and asserts the `V2` constraint rejects a negative balance —
  so a broken, drifted, or unenforced migration fails the build.

The `dev` profile skips Flyway and uses Hibernate's generated H2 schema for a zero-setup start.

## 🧪 Testing

```bash
cd bankapp
./mvnw test
```

146 tests cover model logic, repositories, service rules, **concurrent deposits/withdrawals
and overdraw protection**, JWT auth, transfers, the global error envelope, **idempotent money
movement**, **SWIFT MT103 generation**, **auth rate limiting**, and the **Flyway migrations**
(the real production schema, applied and exercised end-to-end) — all against H2.

One additional **Testcontainers** test (`MySqlFlywayIntegrationTest`) boots a real MySQL 8 in
Docker and runs the migrations + an idempotent deposit against the genuine engine. It skips
automatically when no compatible Docker daemon is reachable, so `./mvnw test` is always green;
where Docker is available it runs as part of the suite.

## 👥 Contributors

- muthula muvhulawa
