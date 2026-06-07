package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

/** Raised when a unique field (email, ID number, account number) already exists. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
