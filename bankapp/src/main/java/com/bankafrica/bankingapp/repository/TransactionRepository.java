package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Returns the account's full ledger, most recent first. */
    List<Transaction> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

    /** Returns one page of the account's ledger, most recent first. */
    Page<Transaction> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId, Pageable pageable);

    /** Fetches a single transaction only if it belongs to the given account (ownership check). */
    Optional<Transaction> findByIdAndAccountId(Long id, Long accountId);
}
