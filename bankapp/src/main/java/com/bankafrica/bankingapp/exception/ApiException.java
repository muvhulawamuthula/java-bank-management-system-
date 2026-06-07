package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for application errors that carry the HTTP status they should map to.
 * The {@link GlobalExceptionHandler} reads {@link #getStatus()} to build the response,
 * so adding a new failure mode is just a new subclass — no handler changes required.
 */
public abstract class ApiException extends RuntimeException {

    protected ApiException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();
}
