package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.exception.ConflictException;
import com.bankafrica.bankingapp.model.IdempotencyKey;
import com.bankafrica.bankingapp.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wraps a money operation so it executes <b>at most once</b> per {@code Idempotency-Key}.
 *
 * <p>The reservation row, the wrapped operation and the stored response all commit in the same
 * transaction, so a successful key is always paired with exactly one balance change. The
 * {@code UNIQUE (account_id, idempotency_key)} constraint serialises concurrent retries: the
 * first request reserves the key and runs; a simultaneous duplicate fails to reserve and is
 * rejected, never executing the operation twice.
 *
 * <ul>
 *   <li><b>Same key, same params, already completed</b> → the stored response is replayed.</li>
 *   <li><b>Same key, different params</b> → {@code 409 Conflict} (a key must identify one request).</li>
 *   <li><b>Same key, original still in flight</b> → {@code 409 Conflict}; the client may retry.</li>
 *   <li><b>No key</b> → the operation runs normally with no idempotency guarantee.</li>
 * </ul>
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> T execute(String key, Long accountId, String operation, Object request,
                         Supplier<T> action, Class<T> responseType) {
        if (key == null || key.isBlank()) {
            return action.get(); // idempotency is opt-in via the header
        }

        String fingerprint = fingerprint(operation, request);

        Optional<IdempotencyKey> existing =
                repository.findByAccountIdAndIdempotencyKey(accountId, key);
        if (existing.isPresent()) {
            return replay(existing.get(), fingerprint, responseType);
        }

        // Reserve the key first and flush so the UNIQUE constraint fires immediately; this
        // blocks a concurrent duplicate from also executing the operation.
        IdempotencyKey record = new IdempotencyKey(key, accountId, operation, fingerprint);
        try {
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException raceLost) {
            throw new ConflictException(
                    "A request with this Idempotency-Key is already being processed");
        }

        T result = action.get();
        record.complete(200, serialize(result));
        repository.save(record);
        return result;
    }

    private <T> T replay(IdempotencyKey record, String fingerprint, Class<T> responseType) {
        if (!record.getRequestFingerprint().equals(fingerprint)) {
            throw new ConflictException(
                    "Idempotency-Key was already used with different request parameters");
        }
        if (!record.isCompleted()) {
            throw new ConflictException(
                    "A request with this Idempotency-Key is already being processed");
        }
        return deserialize(record.getResponseBody(), responseType);
    }

    private String fingerprint(String operation, Object request) {
        try {
            String canonical = operation + ":" + objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to fingerprint idempotent request", e);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize idempotent response", e);
        }
    }

    private <T> T deserialize(String body, Class<T> responseType) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize stored idempotent response", e);
        }
    }
}
