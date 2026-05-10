package com.cardservice.auth.service;

import com.cardservice.auth.dto.LoginRequest;
import com.cardservice.auth.dto.LoginResponse;
import com.cardservice.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse authenticate(LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .map(u -> {
                    String token = jwtTokenService.generate(u.getUserId(), u.getUsername());
                    log.info("User authenticated: username={}", u.getUsername());
                    return new LoginResponse(token, jwtTokenService.getExpirationSeconds());
                })
                .orElseThrow(() -> {
                    log.warn("Authentication failed for username={}", request.username());
                    return new BadCredentialsException("Invalid credentials");
                });
    }
}
