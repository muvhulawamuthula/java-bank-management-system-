package com.bankafrica.bankingapp.dto;

import com.bankafrica.bankingapp.model.User;

import java.math.BigDecimal;

public record ProfileResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Long accountId,
        String accountNumber,
        BigDecimal balance
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getBankAccount().getId(),
                user.getBankAccount().getAccountNumber(),
                user.getBankAccount().getBalance()
        );
    }
}
