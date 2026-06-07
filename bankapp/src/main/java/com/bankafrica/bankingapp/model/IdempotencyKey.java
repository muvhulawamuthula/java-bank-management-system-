package com.bankafrica.bankingapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A record of one idempotent money operation. Created in a "pending" state when a request
 * first reserves an {@code Idempotency-Key}, then "completed" with the response once the
 * operation succeeds. A later retry with the same key (and matching fingerprint) replays the
 * stored {@link #responseBody} instead of executing again.
 */
@Entity
@Table(name = "idempotency_key",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_account_key",
                columnNames = {"account_id", "idempotency_key"}))
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "operation", nullable = false, length = 40)
    private String operation;

    /** SHA-256 of the operation + request parameters; detects key reuse with a different body. */
    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected IdempotencyKey() {
        // for JPA
    }

    /** Creates a pending reservation for a key, before the operation has run. */
    public IdempotencyKey(String idempotencyKey, Long accountId, String operation,
                          String requestFingerprint) {
        this.idempotencyKey = idempotencyKey;
        this.accountId = accountId;
        this.operation = operation;
        this.requestFingerprint = requestFingerprint;
        this.createdAt = LocalDateTime.now();
    }

    /** Records the response and marks the operation complete. */
    public void complete(int responseStatus, String responseBody) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getOperation() {
        return operation;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
