package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

/** Raised for invalid business input (bad amount, malformed field, failed rule). */
public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
