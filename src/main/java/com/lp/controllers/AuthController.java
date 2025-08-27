package com.lp.controllers;

import com.lp.dto.AuthenticationRequest;
import com.lp.dto.AuthenticationResponse;
import com.lp.dto.RegisterRequest;
import com.lp.dto.VerifyRequest;
import com.lp.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request
    ) {
        try {
            boolean result = authService.register(request);
            return ResponseEntity.ok("Verification email has been sent!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Failed to send verification email.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody VerifyRequest request
    ) {
        try {
            return ResponseEntity.ok(authService.verifyUser(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resendVerification")
    public ResponseEntity<?> resendVerification(
            @RequestParam String email
    ) {
        try {
            authService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code resent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        authService.refreshToken(request, response);
    }
}
