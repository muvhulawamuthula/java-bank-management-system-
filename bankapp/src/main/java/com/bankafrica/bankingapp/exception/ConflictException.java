package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a request conflicts with the current state — notably idempotency conflicts:
 * an {@code Idempotency-Key} reused with different parameters, or replayed while the original
 * request is still in flight.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
