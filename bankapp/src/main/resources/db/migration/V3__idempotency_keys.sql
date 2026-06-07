-- Idempotency for money movement. When a client sends an `Idempotency-Key` header with a
-- deposit/withdraw/transfer, the first request is recorded here together with a fingerprint
-- of its parameters and the response it produced. A retry with the same key replays that
-- stored response instead of moving money twice — so a network timeout-and-retry can never
-- double-charge an account.
--
-- The UNIQUE (account_id, idempotency_key) constraint is the concurrency backstop: two
-- simultaneous requests with the same key can never both reserve a row, so at most one
-- executes. Keys are scoped per account so one customer's key can't collide with another's.
--
-- Portable DDL: runs on MySQL (production) and H2 in MySQL mode (FlywayMigrationTest).
CREATE TABLE idempotency_key (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    idempotency_key     VARCHAR(100) NOT NULL,
    account_id          BIGINT       NOT NULL,
    operation           VARCHAR(40)  NOT NULL,
    request_fingerprint VARCHAR(64)  NOT NULL,
    response_status     INT,
    response_body       TEXT,
    created_at          DATETIME     NOT NULL,
    completed_at        DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_idempotency_account_key UNIQUE (account_id, idempotency_key),
    CONSTRAINT fk_idempotency_account FOREIGN KEY (account_id) REFERENCES bank_account (id)
);
