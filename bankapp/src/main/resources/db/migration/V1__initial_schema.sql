-- Baseline schema for Bank Africa.
-- Written in portable DDL that runs on MySQL (production) and on H2 in MySQL
-- compatibility mode (the migration test). No engine/charset clauses so the same
-- script applies to both. Column types mirror the JPA entity mappings.

CREATE TABLE bank_account (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    account_holder_name VARCHAR(255),
    balance             DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    account_number      VARCHAR(255),
    version             BIGINT,
    created_at          DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_bank_account_number UNIQUE (account_number)
);

CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    id_number    VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    password     VARCHAR(255) NOT NULL,
    account_id   BIGINT,
    created_at   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_id_number UNIQUE (id_number),
    CONSTRAINT uk_users_account UNIQUE (account_id),
    CONSTRAINT fk_users_account FOREIGN KEY (account_id) REFERENCES bank_account (id)
);

CREATE TABLE transactions (
    id                          BIGINT         NOT NULL AUTO_INCREMENT,
    account_id                  BIGINT         NOT NULL,
    type                        VARCHAR(20)    NOT NULL,
    amount                      DECIMAL(15, 2) NOT NULL,
    balance_after               DECIMAL(15, 2) NOT NULL,
    description                 VARCHAR(255),
    counterparty_account_number VARCHAR(255),
    created_at                  DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES bank_account (id)
);

CREATE INDEX idx_tx_account_created ON transactions (account_id, created_at);
