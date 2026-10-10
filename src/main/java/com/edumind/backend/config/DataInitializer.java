package com.edumind.backend.config;

import com.edumind.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Value("${edumind.seed-admin.enabled:false}")
    private boolean seedAdminEnabled;

    @Value("${edumind.seed-admin.username:admin}")
    private String adminUsername;

    @Value("${edumind.seed-admin.password:}")
    private String adminPassword;

    @Bean
    CommandLineRunner createAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!seedAdminEnabled) {
                return;
            }

            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException("EDUMIND_ADMIN_PASSWORD must be set when seed-admin is enabled.");
            }

            if (userRepository.findByUsername(adminUsername).isEmpty()) {
                var admin = new com.edumind.backend.entity.User();
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                admin.setFullName("EduMind Administrator");
                admin.setEmail("admin@edumind.com");
                userRepository.save(admin);
            }
        };
    }
}
