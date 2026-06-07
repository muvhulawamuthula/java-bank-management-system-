package com.bankafrica.bankingapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Moves money from the authenticated user's account to another account by number. */
public record TransferRequest(
        @Pattern(regexp = "\\d{10}", message = "Destination account number must be 10 digits")
        String toAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Amount may have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 140, message = "Description must be 140 characters or fewer")
        String description
) {}
