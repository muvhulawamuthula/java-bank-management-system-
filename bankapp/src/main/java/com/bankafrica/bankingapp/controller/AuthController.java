package com.bankafrica.bankingapp.controller;



import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        try {
            String firstName = request.get("firstName").toString();
            String lastName = request.get("lastName").toString();
            String email = request.get("email").toString();
            String idNumber = request.get("idNumber").toString();
            String phoneNumber = request.get("phoneNumber").toString();
            String password = request.get("password").toString();
            BigDecimal initialDeposit = new BigDecimal(request.get("initialDeposit").toString());

            User user = authService.registerUser(firstName, lastName, email,
                    idNumber, phoneNumber, password, initialDeposit);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful");
            response.put("userId", user.getId());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            User user = authService.loginUser(email, password);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("accountId", user.getBankAccount().getId());
            response.put("accountNumber", user.getBankAccount().getAccountNumber());
            response.put("balance", user.getBankAccount().getBalance());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        try {
            User user = authService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("accountNumber", user.getBankAccount().getAccountNumber());
            response.put("balance", user.getBankAccount().getBalance());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error retrieving profile: " + e.getMessage());
        }
    }
}
