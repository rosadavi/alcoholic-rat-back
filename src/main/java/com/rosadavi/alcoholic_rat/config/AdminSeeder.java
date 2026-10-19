package com.rosadavi.alcoholic_rat.config;

import com.rosadavi.alcoholic_rat.domain.user.User;
import com.rosadavi.alcoholic_rat.domain.user.UserRole;
import com.rosadavi.alcoholic_rat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if(adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Admin email or password is blank. Skipping admin seeding.");
            return;
        }

        if(repository.existsByLogin(adminEmail)) {
            return;
        }

        repository.save(new User(adminEmail.trim().toLowerCase(), passwordEncoder.encode(adminPassword), UserRole.ADMIN));
        log.info("Admin user created with email: {}", adminEmail);
    }
}
