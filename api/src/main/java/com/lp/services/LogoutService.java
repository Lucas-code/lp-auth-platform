package com.lp.services;

import com.lp.repository.TokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CookieValue;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private final TokenRepository tokenRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
//        final String authHeader = request.getHeader("Authorization");
//        final String jwt;
//
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            return;
//        }
//
//        jwt = authHeader.substring(7);
//        var storedToken = tokenRepository.findByToken(jwt)
//                .orElse(null);
//        if (storedToken != null) {
//            storedToken.setRevoked(true);
//            tokenRepository.save(storedToken);
//        }



        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            System.out.println(Arrays.toString(cookies));
            Arrays.stream(cookies)
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .findFirst()
                    .ifPresent(refreshCookie -> {
                        String refreshToken = refreshCookie.getValue();
                        System.out.println(refreshToken);

                        var storedRefreshToken = tokenRepository.findByToken(refreshToken).orElse(null);
                        if (storedRefreshToken != null && !storedRefreshToken.isRevoked()) {
                            storedRefreshToken.setRevoked(true);
                            tokenRepository.save(storedRefreshToken);
                        }

                        ResponseCookie clearedCookie = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
//                                .sameSite("None")
//                                .secure(false)
                                .path("/api/v1/auth")
                                .maxAge(0)
                                .build();
                        response.addHeader(HttpHeaders.SET_COOKIE, clearedCookie.toString());

                    });
        }
    }
}
