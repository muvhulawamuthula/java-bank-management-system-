package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByAccountIdAndIdempotencyKey(Long accountId, String idempotencyKey);
}
