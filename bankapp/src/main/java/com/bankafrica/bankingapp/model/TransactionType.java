package com.bankafrica.bankingapp.model;

/**
 * The kind of movement recorded on the ledger. Every balance change on an
 * account produces exactly one {@link Transaction} with one of these types.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT
}
