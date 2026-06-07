package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public static AccountNotFoundException withId(Long accountId) {
        return new AccountNotFoundException("Account not found with ID: " + accountId);
    }

    public static AccountNotFoundException withNumber(String accountNumber) {
        return new AccountNotFoundException("Account not found: " + accountNumber);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
