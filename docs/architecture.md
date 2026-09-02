# Architecture

Bank Africa is a single Spring Boot 3 process: a stateless JWT API plus a static SPA. There are no server sessions; the database is the system of record for balances, the ledger and idempotency keys.

## High-level flow

```
Browser / client
   │  Authorization: Bearer <JWT>
   ▼
RateLimitingFilter ──► JwtAuthenticationFilter ──► Controllers
                                                      │
                                              DTO bean validation
                                                      ▼
                                                   Services
                                      ┌───────────────┼───────────────┐
                                      ▼               ▼               ▼
                              BankingService   IdempotencyService  AuthService
                                      │               │               │
                                      ▼               ▼               ▼
                                 Repositories ──► MySQL (Flyway) / H2 (dev,test)
                                      │
                                      └── GlobalExceptionHandler → uniform ApiError JSON
```

Public paths: `POST /api/auth/register`, `POST /api/auth/login`, static UI, Actuator `health`/`info`, Swagger UI and `/v3/api-docs`. Everything else requires a valid bearer token.

## Layers

| Layer | Responsibility |
|---|---|
| **Controllers** | HTTP mapping, auth principal → account id, idempotency header wiring. Never accept a client-supplied account id for money ops. |
| **DTOs** | Jakarta Validation records (`RegisterRequest`, `AmountRequest`, …). Controllers never bind entities. |
| **Services** | Domain rules: register/login, deposit/withdraw/transfer, ledger paging, MT103 rendering, idempotent execute-or-replay. |
| **Repositories** | Spring Data JPA; balance updates use pessimistic write locks where money moves. |
| **Security** | Stateless filter chain: rate limit → JWT → `UsernamePasswordAuthenticationFilter` slot; BCrypt `PasswordEncoder`; CORS from config. |
| **Exceptions** | Domain `ApiException` subtypes + `GlobalExceptionHandler` → single `ApiError` envelope (optional `fieldErrors`). |

## Security design

1. **Registration / login** are public and rate-limited per client IP (`app.ratelimit.auth.*`). Exhausted bucket → `429` in the same error envelope, before authentication runs.
2. **JWT** (JJWT, HS256) is issued on register/login. Secret and TTL come from `JWT_SECRET` / `JWT_EXPIRATION_MS`.
3. **`JwtAuthenticationFilter`** loads the user via `AppUserDetailsService` and sets the `SecurityContext` for the request only.
4. **Account scoping**: `BankingController.currentAccountId(principal)` resolves the caller's account from email → user → bank account. That closes the previous IDOR where any account id in the path/body could be mutated.
5. **CSRF off**, **sessions `STATELESS`** — appropriate for bearer-token APIs (no cookie auth).

## Money movement & concurrency

- Deposit / withdraw / transfer load the account row with a **pessimistic write lock** and an optimistic **`@Version`** so concurrent withdrawals cannot overdraw or lose updates.
- Transfers lock both accounts in a **deterministic id order** to avoid deadlocks; debit and both ledger legs commit atomically.
- Every balance change writes an **immutable ledger** row (`transactions`) in the same database transaction as the balance update.
- MySQL also enforces `CHECK (balance >= 0)` (Flyway `V2`) as a hard backstop.

## Idempotency

Money endpoints accept an optional `Idempotency-Key` header, handled by `IdempotencyService`:

1. If the key is absent, the operation runs normally.
2. If present, the service stores `(account_id, key, operation, request fingerprint, response)`.
3. A retry with the **same** key and fingerprint **replays** the stored response — no second movement.
4. Reuse with a **different** body → `409 Conflict`.
5. `UNIQUE (account_id, idempotency_key)` (Flyway `V3`) serialises concurrent first attempts so the operation executes **at most once**.

## Schema ownership (Flyway)

Hibernate `ddl-auto` is `none` in the default profile. Flyway applies versioned scripts before the app accepts traffic:

| Version | Script | Purpose |
|---|---|---|
| `V1` | `V1__initial_schema.sql` | `bank_account`, `users`, `transactions`, FKs, uniques, ledger index |
| `V2` | `V2__balance_non_negative_check.sql` | `CHECK (balance >= 0)` |
| `V3` | `V3__idempotency_keys.sql` | `idempotency_key` table + unique `(account_id, idempotency_key)` |

Never edit an applied migration; add `V<n+1>__…`. `baseline-on-migrate=true` lets Flyway adopt a pre-existing database. The `dev` profile skips Flyway and uses Hibernate-generated H2 for zero-setup local runs.

## Package map (`com.bankafrica.bankingapp`)

```
controller/     AuthController, BankingController
service/        AuthService, BankingService, IdempotencyService, SwiftMessageService
security/       SecurityConfig, JwtService, JwtAuthenticationFilter,
                RateLimitingFilter, AppUserDetailsService, RestAuthenticationEntryPoint
repository/     User, BankAccount, Transaction, IdempotencyKey
model/          User, BankAccount, Transaction, TransactionType, IdempotencyKey
dto/            request + response records
exception/      ApiError, ApiException hierarchy, GlobalExceptionHandler
config/         OpenApiConfig
```

## Observability & docs surface

- Actuator: only `health` and `info` exposed.
- springdoc: Swagger UI at `/swagger-ui.html`, OpenAPI 3 at `/v3/api-docs`.
- Static SPA at `/` (`src/main/resources/static/index.html`).

## Runtime profiles

| Profile | Database | Flyway | Typical use |
|---|---|---|---|
| default | MySQL (`DB_*` env) | on | local MySQL / Compose / production-like |
| `dev` | in-memory H2 | off | fastest local UI/API loop |
| `test` | H2 (+ optional Testcontainers MySQL) | as needed by tests | `./mvnw test` |
