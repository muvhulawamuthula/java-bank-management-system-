package com.bankafrica.bankingapp.dto;

/**
 * The SWIFT representation of a transfer: the generated ISO 15022 MT103 (Single Customer
 * Credit Transfer) message plus the key fields lifted out for convenience.
 */
public record SwiftMessageResponse(
        Long transactionId,
        String messageType,
        String reference,
        String senderBic,
        String receiverBic,
        String message
) {}
