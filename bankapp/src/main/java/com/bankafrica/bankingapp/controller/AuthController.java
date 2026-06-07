package com.bankafrica.bankingapp.controller;

import com.bankafrica.bankingapp.dto.AuthResponse;
import com.bankafrica.bankingapp.dto.LoginRequest;
import com.bankafrica.bankingapp.dto.ProfileResponse;
import com.bankafrica.bankingapp.dto.RegisterRequest;
import com.bankafrica.bankingapp.exception.InvalidCredentialsException;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.security.JwtService;
import com.bankafrica.bankingapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.registerUser(
                request.firstName(), request.lastName(), request.email(), request.idNumber(),
                request.phoneNumber(), request.password(), request.initialDeposit());
        String token = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(user, token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.loginUser(request.email(), request.password());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthResponse.from(user, token));
    }

    /** Returns the profile of the currently authenticated user (replaces the old,
     *  unauthenticated /profile/{userId} endpoint that allowed reading anyone's data). */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal UserDetails principal) {
        User user = authService.getUserByEmail(principal.getUsername());
        if (user == null) {
            throw new InvalidCredentialsException();
        }
        return ResponseEntity.ok(ProfileResponse.from(user));
    }
}
