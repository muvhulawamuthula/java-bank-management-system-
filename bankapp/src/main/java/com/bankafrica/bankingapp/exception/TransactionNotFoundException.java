package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

/** Raised when a transaction can't be found on the authenticated user's own account. */
public class TransactionNotFoundException extends ApiException {

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found with ID: " + transactionId);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
