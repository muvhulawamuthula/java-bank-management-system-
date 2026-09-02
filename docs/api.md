# API reference

Base URL (local): `http://localhost:8080`

Interactive docs: **[/swagger-ui.html](http://localhost:8080/swagger-ui.html)** · OpenAPI JSON: `/v3/api-docs`

All authenticated routes require:

```http
Authorization: Bearer <token>
```

## Error envelope

Every failure (validation, domain, auth, rate limit, unexpected) returns JSON shaped like:

```json
{
  "timestamp": "2026-09-02T09:00:00.000+00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Insufficient funds. Current balance: R750.00",
  "path": "/api/account/withdraw",
  "fieldErrors": { "amount": "Amount must be greater than 0" }
}
```

`fieldErrors` appears on bean-validation failures (`400`). Domain rules such as insufficient funds typically use `422`.

| Status | When |
|---|---|
| `400` | Malformed JSON or validation failure |
| `401` | Missing/invalid token or bad credentials |
| `403` | Authenticated but not allowed |
| `404` | Account/transaction not found (scoped to caller) |
| `409` | Conflict (e.g. duplicate registration, idempotency key reused with different body) |
| `429` | Auth rate limit exceeded |
| `422` | Business rule (e.g. insufficient funds) |
| `500` | Unexpected (message sanitized; no stack traces to clients) |

---

## Auth — `/api/auth`

### `POST /api/auth/register` (public)

Creates a user and linked bank account; returns a JWT.

**Body (`RegisterRequest`):**

| Field | Rules |
|---|---|
| `firstName`, `lastName` | required, non-blank |
| `email` | required, valid email |
| `idNumber` | exactly 13 digits |
| `phoneNumber` | exactly 10 digits |
| `password` | min 6 characters |
| `initialDeposit` | ≥ `100.00` |

**`201` response (`AuthResponse`):** `token`, `userId`, `firstName`, `lastName`, `email`, `accountId`, `accountNumber`, `balance`.

### `POST /api/auth/login` (public)

**Body:** `{ "email", "password" }` → **`200`** `AuthResponse` (same shape as register).

Both credential endpoints are rate-limited per IP (`app.ratelimit.auth.*`).

### `GET /api/auth/me` (JWT)

Returns `ProfileResponse` for the authenticated user (replaces any unauthenticated profile-by-id endpoint).

---

## Account — `/api/account`

Account identity is **always** taken from the JWT. Clients never pass an account id for deposit/withdraw/transfer.

### `GET /api/account`

**`200` `AccountResponse`:** account id, account number, balance, and related snapshot fields.

### `POST /api/account/deposit` · `POST /api/account/withdraw`

**Body (`AmountRequest`):**

| Field | Rules |
|---|---|
| `amount` | required, ≥ `0.01`, max 2 decimal places |

**Headers (optional):** `Idempotency-Key: <opaque-string>`

**`200`:** updated `AccountResponse`. Withdraw with insufficient funds → `422`.

### `POST /api/account/transfer`

**Body (`TransferRequest`):**

| Field | Rules |
|---|---|
| `toAccountNumber` | 10 digits |
| `amount` | ≥ `0.01`, max 2 decimal places |
| `description` | optional, ≤ 140 chars |

Optional `Idempotency-Key` as above. Returns the **source** account snapshot after the transfer.

### Idempotency behaviour

| Scenario | Result |
|---|---|
| No key | Operation runs every time |
| Same key + same body | Original response replayed; money not moved again |
| Same key + different body | `409 Conflict` |

### `GET /api/account/transactions?page=&size=`

Paged ledger, newest first.

| Query | Default | Notes |
|---|---|---|
| `page` | `0` | clamped ≥ 0 |
| `size` | `20` | clamped to `[1, 100]` |

**`200` `PagedResponse<TransactionResponse>`:** `content`, `page`, `size`, `totalElements`, `totalPages`, etc. Each transaction includes type, amount, resulting balance, timestamps, and transfer counterparty when applicable.

### `GET /api/account/transactions/{id}/swift`

Generates a SWIFT **MT103** (ISO 15022 Single Customer Credit Transfer) for one of the caller's **transfer** transactions. Sender BIC / bank name from `SWIFT_BIC` / `SWIFT_BANK_NAME`.

**`200` `SwiftMessageResponse`:** structured MT103 fields / message text as produced by `SwiftMessageService`.

---

## Curl walkthrough

```bash
# Register
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName":"Mac","lastName":"M","email":"mac@example.com",
    "idNumber":"9001015000000","phoneNumber":"0712345678",
    "password":"securepass","initialDeposit":500.00
  }' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Deposit (idempotent retry-safe)
curl -s -X POST localhost:8080/api/account/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: dep-001' \
  -d '{"amount":250.00}'

# Ledger
curl -s "localhost:8080/api/account/transactions?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Public non-API surfaces

| Path | Purpose |
|---|---|
| `/`, `/index.html` | Static SPA |
| `/actuator/health`, `/actuator/info` | Liveness / info |
| `/swagger-ui.html`, `/v3/api-docs/**` | OpenAPI UI + spec |
