package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.model.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    boolean existsByAccountNumber(String accountNumber);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    /**
     * Loads an account while holding a row-level write lock (SELECT ... FOR UPDATE)
     * for the duration of the surrounding transaction. This serialises concurrent
     * balance mutations so two withdrawals can never both read a stale balance and
     * overdraw the account.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
