package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.exception.DuplicateResourceException;
import com.bankafrica.bankingapp.exception.InvalidCredentialsException;
import com.bankafrica.bankingapp.exception.InvalidRequestException;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.Transaction;
import com.bankafrica.bankingapp.model.TransactionType;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.repository.TransactionRepository;
import com.bankafrica.bankingapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Registration and credential verification. Passwords are stored only as BCrypt
 * hashes; the raw password never touches the database. Registration is atomic:
 * the user, their account and the opening ledger entry are persisted together.
 */
@Service
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern ID_NUMBER = Pattern.compile("^\\d{13}$");
    private static final Pattern PHONE = Pattern.compile("^\\d{10}$");
    private static final BigDecimal MIN_INITIAL_DEPOSIT = new BigDecimal("100.00");

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       BankAccountRepository bankAccountRepository,
                       TransactionRepository transactionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String firstName, String lastName, String email,
                             String idNumber, String phoneNumber, String password,
                             BigDecimal initialDeposit) {

        validateRegistration(firstName, lastName, email, idNumber, phoneNumber, password);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByIdNumber(idNumber)) {
            throw new DuplicateResourceException("ID number already registered");
        }
        validateInitialDeposit(initialDeposit);

        User user = new User(firstName, lastName, email, idNumber, phoneNumber,
                passwordEncoder.encode(password));

        BankAccount account = new BankAccount(firstName + " " + lastName, initialDeposit);
        ensureUniqueAccountNumber(account);
        user.setBankAccount(account);

        User saved = userRepository.save(user);

        // Seed the ledger with the opening balance so history is complete from day one.
        BankAccount savedAccount = saved.getBankAccount();
        transactionRepository.save(new Transaction(savedAccount, TransactionType.DEPOSIT,
                initialDeposit, savedAccount.getBalance(), "Account opening deposit", null));

        return saved;
    }

    /**
     * Verifies credentials. Returns the user on success; throws
     * {@link InvalidCredentialsException} otherwise. The failure message is identical
     * for "no such email" and "wrong password" so attackers can't enumerate accounts.
     */
    @Transactional(readOnly = true)
    public User loginUser(String email, String password) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new InvalidRequestException("Invalid email format");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidRequestException("Password cannot be empty");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        return userRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private void validateRegistration(String firstName, String lastName, String email,
                                      String idNumber, String phoneNumber, String password) {
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidRequestException("First name cannot be empty");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new InvalidRequestException("Last name cannot be empty");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new InvalidRequestException("Invalid email format");
        }
        if (idNumber == null || !ID_NUMBER.matcher(idNumber.trim()).matches()) {
            throw new InvalidRequestException("Invalid ID number format");
        }
        if (phoneNumber == null || !PHONE.matcher(phoneNumber.trim()).matches()) {
            throw new InvalidRequestException("Invalid phone number format");
        }
        if (password == null || password.trim().length() < 6) {
            throw new InvalidRequestException("Password must be at least 6 characters");
        }
    }

    private void validateInitialDeposit(BigDecimal initialDeposit) {
        if (initialDeposit == null) {
            throw new InvalidRequestException("Initial deposit cannot be null");
        }
        if (initialDeposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Initial deposit must be positive");
        }
        if (initialDeposit.compareTo(MIN_INITIAL_DEPOSIT) < 0) {
            throw new InvalidRequestException("Minimum initial deposit is R100.00");
        }
    }

    /** Retries account-number generation until a globally unique value is found. */
    private void ensureUniqueAccountNumber(BankAccount account) {
        int attempts = 0;
        while (bankAccountRepository.existsByAccountNumber(account.getAccountNumber())) {
            account.regenerateAccountNumber();
            if (++attempts > 10) {
                throw new IllegalStateException("Unable to allocate a unique account number");
            }
        }
    }
}
