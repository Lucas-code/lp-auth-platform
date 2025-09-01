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
import java.util.Optional;

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
    public void authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletResponse response
    ) throws IOException {
        authService.authenticate(request, response);
    }

    @PostMapping("/verify")
    public void verify(
            @RequestParam Integer id,
            @RequestBody VerifyRequest request,
            HttpServletResponse response
    ) throws IOException {
        authService.verifyUser(id, request, response);
    }

    @PostMapping("/resendVerification")
    public ResponseEntity<?> resendVerification(
            @RequestParam Integer id
    ) {
        try {
            authService.resendVerificationCode(id);
            return ResponseEntity.ok("Verification code resent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping ("/refresh-token")
    public void refreshToken(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) throws IOException {
        authService.refreshToken(refreshToken, response);
    }

    @GetMapping("/userEnabled")
    public ResponseEntity<Boolean> isUserEnabled(
            @RequestParam Integer id
    ) {
        return ResponseEntity.ok(authService.isUserEnabled(id));
    }
}
