package com.cardservice.config;

import com.cardservice.user.domain.User;
import com.cardservice.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedUsername;
    private final String seedPassword;

    public SeedRunner(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${seed.username}") String seedUsername,
                      @Value("${seed.password}") String seedPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedUsername = seedUsername;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(seedUsername)) {
            log.info("Seed user '{}' already exists — skipping", seedUsername);
            return;
        }
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(seedUsername)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        log.info("Seed user '{}' created", seedUsername);
    }
}
