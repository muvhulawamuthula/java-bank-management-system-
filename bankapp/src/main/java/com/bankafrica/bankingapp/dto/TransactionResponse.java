package com.bankafrica.bankingapp.dto;

import com.bankafrica.bankingapp.model.Transaction;
import com.bankafrica.bankingapp.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        String counterpartyAccountNumber,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getDescription(),
                tx.getCounterpartyAccountNumber(),
                tx.getCreatedAt()
        );
    }
}
