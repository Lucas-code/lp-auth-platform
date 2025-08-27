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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public boolean register(RegisterRequest request) {
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
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

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
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
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(user, jwt);
        return AuthenticationResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthenticationResponse verifyUser(VerifyRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
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
                var refreshToken = jwtService.generateRefreshToken(user);
                saveUserToken(savedUser, jwt);
                return AuthenticationResponse.builder()
                        .accessToken(jwt)
                        .refreshToken(refreshToken)
                        .build();
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
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.userRepository.findByEmail(userEmail).orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var jwt = jwtService.generateAccessToken(user);
                saveUserToken(user, jwt);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(jwt)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account already verified");
            }

            user.setVerificationCode(generateVerificationCode());
            user.setVerificationExpiration(LocalDateTime.now().plusHours(1));
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

    public void sendVerificationEmail(User user) throws MessagingException {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String message = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Email Verification</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            background-color: #f4f4f4;\n" +
                "        }\n" +
                "        .email-container {\n" +
                "            width: 100%;\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            background-color: #ffffff;\n" +
                "            padding: 20px;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);\n" +
                "        }\n" +
                "        .email-header {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .email-header h1 {\n" +
                "            color: #4CAF50;\n" +
                "            font-size: 24px;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "        .email-body {\n" +
                "            font-size: 16px;\n" +
                "            line-height: 1.5;\n" +
                "            color: #333;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .verification-code {\n" +
                "            font-size: 22px;\n" +
                "            font-weight: bold;\n" +
                "            color: #4CAF50;\n" +
                "            display: inline-block;\n" +
                "            background-color: #f0f8f0;\n" +
                "            padding: 10px;\n" +
                "            border-radius: 5px;\n" +
                "            margin-top: 10px;\n" +
                "        }\n" +
                "        .email-footer {\n" +
                "            font-size: 14px;\n" +
                "            text-align: center;\n" +
                "            color: #777;\n" +
                "            margin-top: 30px;\n" +
                "        }\n" +
                "        .email-footer a {\n" +
                "            color: #4CAF50;\n" +
                "            text-decoration: none;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"email-header\">\n" +
                "            <h1>Email Verification Code</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\">\n" +
                "            <p>Hi there,</p>\n" +
                "            <p>Thank you for registering with us! To complete your registration and verify your email address, please use the following verification code:</p>\n" +
                "            <div class=\"verification-code\">\n" +
                String.format("                %s\n",verificationCode) +
                "            </div>\n" +
                "            <p>This verification code is valid for the next 10 minutes. Please enter it on the verification page to complete your registration process.</p>\n" +
                "            <p>If you did not request this code, please ignore this email.</p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>Best regards, <br> The [Your Company Name] Team</p>\n" +
                "            <p>For any questions, visit <a href=\"http://www.yourwebsite.com/support\">our support page</a>.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>\n";
        emailService.sendVerificationEmail(user.getEmail(), subject, message);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens= tokenRepository.findValidTokensByUserId(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

    private void saveUserToken(User user, String jwt) {
        revokeAllUserTokens(user);
        var token = Token.builder()
                .user(user)
                .token(jwt)
                .tokenType(TokenType.ACCESS)
                .revoked(false)
                .expired(false)
                .build();
        tokenRepository.save(token);
    }
}
