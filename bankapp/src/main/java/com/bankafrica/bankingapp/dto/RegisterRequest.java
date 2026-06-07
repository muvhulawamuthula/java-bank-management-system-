package com.bankafrica.bankingapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "ID number is required")
        @Pattern(regexp = "\\d{13}", message = "ID number must be 13 digits")
        String idNumber,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotNull(message = "Initial deposit is required")
        @DecimalMin(value = "100.00", message = "Minimum initial deposit is R100.00")
        BigDecimal initialDeposit
) {}
