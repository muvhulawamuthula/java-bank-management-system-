package com.bankafrica.bankingapp.dto;

import com.bankafrica.bankingapp.model.BankAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long accountId,
        String accountNumber,
        String accountHolderName,
        BigDecimal balance,
        LocalDateTime createdAt
) {
    public static AccountResponse from(BankAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
