package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.dto.SwiftMessageResponse;
import com.bankafrica.bankingapp.exception.InvalidRequestException;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.Transaction;
import com.bankafrica.bankingapp.model.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Renders a transfer {@link Transaction} as a SWIFT <b>MT103</b> (Single Customer Credit
 * Transfer) message in ISO 15022 format — the message a bank would put on the SWIFT network
 * to instruct the beneficiary bank to credit the payee.
 *
 * <p>This produces a standards-shaped message (correct blocks, field tags and formatting:
 * {@code :32A:} value-date/currency/amount with the SWIFT decimal comma, {@code :50K:}/{@code :59:}
 * ordering and beneficiary customers, {@code :71A:} charge bearer). It does not transmit
 * anything onto the live SWIFT network. Real cross-border rails are migrating to ISO 20022
 * {@code pacs.008}; MT103 remains the canonical, recognisable customer-transfer message.
 */
@Service
public class SwiftMessageService {

    private static final DateTimeFormatter VALUE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CURRENCY = "ZAR";

    private final String senderBic;
    private final String bankName;

    public SwiftMessageService(
            @Value("${app.swift.bic:BANKZAJJXXX}") String senderBic,
            @Value("${app.swift.bank-name:BANK AFRICA}") String bankName) {
        this.senderBic = senderBic;
        this.bankName = bankName;
    }

    /**
     * Builds the MT103 for a transfer leg. Only the debit leg ({@code TRANSFER_OUT}) and the
     * credit leg ({@code TRANSFER_IN}) carry a counterparty and can be expressed as a credit
     * transfer; deposits and withdrawals are not SWIFT payments.
     */
    public SwiftMessageResponse toMt103(Transaction tx, BankAccount ownerAccount) {
        if (tx.getType() != TransactionType.TRANSFER_OUT && tx.getType() != TransactionType.TRANSFER_IN) {
            throw new InvalidRequestException(
                    "A SWIFT MT103 can only be generated for a transfer transaction");
        }

        boolean debit = tx.getType() == TransactionType.TRANSFER_OUT;
        String ownerNumber = ownerAccount.getAccountNumber();
        String ownerName = upper(ownerAccount.getAccountHolderName());
        String counterpartyNumber = tx.getCounterpartyAccountNumber();

        // For the debit leg the owner is the ordering customer; for the credit leg they are the
        // beneficiary. The counterparty fills the opposite role.
        String orderingNumber = debit ? ownerNumber : counterpartyNumber;
        String orderingName = debit ? ownerName : counterpartyName(counterpartyNumber);
        String beneficiaryNumber = debit ? counterpartyNumber : ownerNumber;
        String beneficiaryName = debit ? counterpartyName(counterpartyNumber) : ownerName;

        String reference = reference(tx);
        String remittance = (tx.getDescription() == null || tx.getDescription().isBlank())
                ? "PAYMENT" : upper(tx.getDescription());

        String message = """
                {1:F01%s0000000000}{2:I103%sN}{4:
                :20:%s
                :23B:CRED
                :32A:%s%s%s
                :50K:/%s
                %s
                :52A:%s
                :57A:%s
                :59:/%s
                %s
                :70:%s
                :71A:SHA
                -}""".formatted(
                pad11(senderBic),
                pad11(senderBic),
                reference,
                tx.getCreatedAt().format(VALUE_DATE), CURRENCY, formatAmount(tx.getAmount()),
                orderingNumber,
                orderingName,
                bic8(senderBic),
                bic8(senderBic),
                beneficiaryNumber,
                beneficiaryName,
                truncate(remittance, 35));

        return new SwiftMessageResponse(
                tx.getId(), "MT103", reference, senderBic, senderBic, message);
    }

    /** SWIFT field 20 (transaction reference): max 16 chars, no slashes. */
    private String reference(Transaction tx) {
        String ref = "FT" + tx.getCreatedAt().format(VALUE_DATE) + tx.getId();
        return truncate(ref, 16);
    }

    /** SWIFT amounts use a decimal comma and no thousands separators, e.g. 1234,50. */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private String counterpartyName(String accountNumber) {
        return upper(bankName) + " ACCOUNT " + (accountNumber == null ? "" : accountNumber);
    }

    /** Pads/truncates a BIC to the 11-char form used in the application header. */
    private String pad11(String bic) {
        String b = bic == null ? "" : bic.toUpperCase();
        if (b.length() >= 11) {
            return b.substring(0, 11);
        }
        return b + "X".repeat(11 - b.length());
    }

    /** The 8-char BIC used in :52A:/:57A: institution fields. */
    private String bic8(String bic) {
        String b = pad11(bic);
        return b.substring(0, 8);
    }

    private String upper(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
