package com.bankafrica.bankingapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An immutable ledger entry. One row is written for every deposit, withdrawal
 * and transfer leg, capturing the amount, the resulting balance and (for
 * transfers) the counterparty account. Rows are never updated or deleted, which
 * gives the account a full, auditable history.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_account_created", columnList = "account_id, created_at")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description")
    private String description;

    @Column(name = "counterparty_account_number")
    private String counterpartyAccountNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
        // for JPA
    }

    public Transaction(BankAccount account, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, String description, String counterpartyAccountNumber) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.counterpartyAccountNumber = counterpartyAccountNumber;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public BankAccount getAccount() {
        return account;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public String getCounterpartyAccountNumber() {
        return counterpartyAccountNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
