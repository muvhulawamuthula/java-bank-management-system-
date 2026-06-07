-- Database-level backstop for the overdraw guard in BankingService.
-- The application already refuses any withdrawal/transfer that would push an
-- account negative, but a CHECK constraint makes that invariant true of the
-- data itself: no bug, manual SQL, or future code path can ever persist a
-- negative balance. Defence in depth for the one number that must never lie.
--
-- Supported by MySQL 8.0.16+ (enforced) and by H2 in MySQL-compatibility mode,
-- so the same script applies in production and in FlywayMigrationTest.
ALTER TABLE bank_account
    ADD CONSTRAINT chk_bank_account_balance_non_negative CHECK (balance >= 0);
