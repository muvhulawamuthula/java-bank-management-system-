package com.bankafrica.bankingapp.dto;

import com.bankafrica.bankingapp.model.User;

import java.math.BigDecimal;

/** Returned by register and login: the bearer token plus the user's account snapshot. */
public record AuthResponse(
        String token,
        Long userId,
        String firstName,
        String lastName,
        String email,
        Long accountId,
        String accountNumber,
        BigDecimal balance
) {
    public static AuthResponse from(User user, String token) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBankAccount().getId(),
                user.getBankAccount().getAccountNumber(),
                user.getBankAccount().getBalance()
        );
    }
}
