package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

/** Raised on a failed login. The message is deliberately generic to avoid leaking
 *  whether an email exists. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
