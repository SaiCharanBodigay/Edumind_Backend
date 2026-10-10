package com.edumind.backend.config;

import com.edumind.backend.entity.User;
import com.edumind.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner createAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                User admin = new User();

                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRole("ADMIN");
                admin.setFullName("EduMind Administrator");
                admin.setEmail("admin@edumind.com");

                userRepository.save(admin);

                System.out.println("=================================");
                System.out.println("EduMind Admin account created");
                System.out.println("Username: admin");
                System.out.println("Password: Admin@123");
                System.out.println("=================================");
            }
        };
    }
}