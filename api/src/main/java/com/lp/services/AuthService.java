package com.lp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lp.dto.AuthenticationRequest;
import com.lp.dto.AuthenticationResponse;
import com.lp.dto.RegisterRequest;
import com.lp.dto.VerifyRequest;
import com.lp.entities.Token;
import com.lp.repository.TokenRepository;
import com.lp.enums.TokenType;
import com.lp.enums.Role;
import com.lp.entities.User;
import com.lp.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final UrlService urlService;

    public boolean register(RegisterRequest request) {
        var user = User.builder()
//                .firstName(request.getFirstName())
//                .lastName(request.getLastName())
                .email(request.getEmail())
                .verificationCode(generateVerificationCode())
                .verificationExpiration(LocalDateTime.now().plusMinutes(15))
                .enabled(false)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        try {
            sendVerificationEmail(user);
            return true;
        } catch (MessagingException e) {
            return false;
//            throw new RuntimeException("Failed to send verification email");
        }
    }

    public void authenticate(AuthenticationRequest request, HttpServletResponse response) throws IOException {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified, please verify your account");
        }

        var jwt = jwtService.generateAccessToken(user);
//        saveUserToken(user, jwt);
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserRefreshToken(user, refreshToken);
        setResponseAuthData(response, jwt, refreshToken, user);
    }

    private void setResponseAuthData(HttpServletResponse response, String jwt, String refreshToken, User user) throws IOException {
//        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
//        refreshCookie.setHttpOnly(true);
//        refreshCookie.setAttribute("");
//        refreshCookie.setPath("/api/v1/auth");
//        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
//        response.addCookie(refreshCookie);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
//                .sameSite("None")
//                .secure(false)
                .path("/api/v1/auth")
                .maxAge(jwtService.getRefreshTokenExpiration())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        var authResponse = AuthenticationResponse.builder()
                .accessToken(jwt)
                .userEmail(user.getEmail())
                .role(user.getRole())
                .build();
        new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
    }

    public void verifyUser(Integer id,VerifyRequest request, HttpServletResponse response) throws IOException {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationExpiration().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");
            }

            if (user.getVerificationCode().equals(request.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationExpiration(null);
                var savedUser = userRepository.save(user);
                var jwt = jwtService.generateAccessToken(user);
//                saveUserToken(savedUser, jwt);
                var refreshToken = jwtService.generateRefreshToken(user);
                saveUserRefreshToken(user, refreshToken);
                setResponseAuthData(response, jwt, refreshToken, user);
            }
            else {
                throw new RuntimeException("Invalid verification code");
            }
        }
        else {
            throw new RuntimeException("User not found");
        }
    }

    public void refreshToken(
            String refreshToken,
            HttpServletResponse response
    ) throws IOException {
        final String userEmail;

        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.userRepository.findByEmail(userEmail).orElseThrow();
            var isTokenValid = tokenRepository.findByToken(refreshToken).map(t -> !t.isRevoked()).orElse(false);
            if (jwtService.isTokenValid(refreshToken, user) && isTokenValid) {
                var jwt = jwtService.generateAccessToken(user);
//                saveUserToken(user, jwt);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(jwt)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
                return;
            }
        }
        throw new RuntimeException("Invalid refresh token");
    }

    public void resendVerificationCode(Integer id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account already verified");
            }

            user.setVerificationCode(generateVerificationCode());
            user.setVerificationExpiration(LocalDateTime.now().plusMinutes(15));
            try {
                sendVerificationEmail(user);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to resend verification code");
            }
            userRepository.save(user);
        }
        else {
            throw new RuntimeException("User not found");
        }
    }

    public boolean isUserEnabled(Integer id) {
        var user = userRepository.findById(id);
        return user.map(User::isEnabled).orElse(false);
    }

    public void sendVerificationEmail(User user) throws MessagingException {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String verificationLink = urlService.getClientUrl() + "/verify/" + user.getId();
        Context context = new Context();
        context.setVariable("verificationCode", verificationCode);
        context.setVariable("verificationLink", verificationLink);
        String message = templateEngine.process("verificationTemplate", context);
        emailService.sendVerificationEmail(user.getEmail(), subject, message);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens= tokenRepository.findValidAccessTokensByUserId(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(t -> {
            t.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

    private void revokeAllUserRefreshTokens(User user) {
        var validUserRefreshTokens= tokenRepository.findValidRefreshTokensByUserId(user.getId());
        if (validUserRefreshTokens.isEmpty()) {
            return;
        }

        validUserRefreshTokens.forEach(t -> {
            t.setRevoked(true);
        });

        tokenRepository.saveAll(validUserRefreshTokens);
    }

    private void saveUserToken(User user, String jwt) {
        revokeAllUserTokens(user);
        var token = Token.builder()
                .user(user)
                .token(jwt)
                .tokenType(TokenType.ACCESS)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void saveUserRefreshToken(User user, String refreshToken) {
        revokeAllUserRefreshTokens(user);
        var token = Token.builder()
                .user(user)
                .token(refreshToken)
                .tokenType(TokenType.REFRESH)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }
}
