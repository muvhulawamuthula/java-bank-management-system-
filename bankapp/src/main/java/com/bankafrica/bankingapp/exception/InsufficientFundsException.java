package com.bankafrica.bankingapp.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(BigDecimal currentBalance) {
        super("Insufficient funds. Current balance: R" + currentBalance);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
