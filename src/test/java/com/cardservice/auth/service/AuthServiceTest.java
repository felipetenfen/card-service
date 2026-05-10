package com.cardservice.auth.service;

import com.cardservice.auth.dto.LoginRequest;
import com.cardservice.auth.dto.LoginResponse;
import com.cardservice.user.domain.User;
import com.cardservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void shouldReturnTokenOnValidCredentials() {
        String userId = UUID.randomUUID().toString();
        User user = User.builder()
                .id(userId)
                .username("admin")
                .passwordHash("$2a$12$hash")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$12$hash")).thenReturn(true);
        when(jwtTokenService.generate(UUID.fromString(userId), "admin")).thenReturn("jwt.token.here");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.authenticate(new LoginRequest("admin", "secret"));

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void shouldThrowOnWrongPassword() {
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username("admin")
                .passwordHash("$2a$12$hash")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$12$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldThrowOnUnknownUsername() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(new LoginRequest("unknown", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
