package com.bankafrica.bankingapp.service;


import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class BankingService {

    @Autowired
    private BankAccountRepository bankAccountRepository;


    public BankAccount getAccount(Long accountId) {
        Optional<BankAccount> account = bankAccountRepository.findById(accountId);
        return account.orElse(null);
    }


    public BankAccount deposit(Long accountId, BigDecimal amount) {
        // Find the account
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));


        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }


        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);


        return bankAccountRepository.save(account);
    }


    public BankAccount withdraw(Long accountId, BigDecimal amount) {

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));


        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be positive");
        }


        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds. Current balance: $" + account.getBalance());
        }


        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);


        return bankAccountRepository.save(account);
    }


    public BankAccount createAccount(String accountHolderName, BigDecimal initialBalance) {
        BankAccount account = new BankAccount();
        account.setAccountHolderName(accountHolderName);
        account.setBalance(initialBalance != null ? initialBalance : BigDecimal.ZERO);

        return bankAccountRepository.save(account);
    }
}