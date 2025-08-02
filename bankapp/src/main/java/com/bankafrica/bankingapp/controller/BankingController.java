package com.bankafrica.bankingapp.controller;



import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.service.BankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to connect
public class BankingController {

    @Autowired
    private BankingService bankingService;

    
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable Long accountId) {
        try {
            BankAccount account = bankingService.getAccount(accountId);
            if (account == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error retrieving account: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        try {
            // Extract data from request
            Long accountId = Long.valueOf(request.get("accountId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Amount must be greater than 0");
                return ResponseEntity.badRequest().body(error);
            }

            BankAccount account = bankingService.deposit(accountId, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Deposit successful");
            response.put("newBalance", account.getBalance());
            response.put("accountId", account.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Deposit failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> request) {
        try {
            // Extract data from request
            Long accountId = Long.valueOf(request.get("accountId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Amount must be greater than 0");
                return ResponseEntity.badRequest().body(error);
            }

            BankAccount account = bankingService.withdraw(accountId, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Withdrawal successful");
            response.put("newBalance", account.getBalance());
            response.put("accountId", account.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Withdrawal failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    
    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> request) {
        try {
            String name = request.get("name").toString();
            BigDecimal initialBalance = new BigDecimal(request.get("initialBalance").toString());

            BankAccount account = bankingService.createAccount(name, initialBalance);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Account creation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
