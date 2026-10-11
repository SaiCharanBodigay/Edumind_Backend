package com.edumind.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // REST API using JWT does not use CSRF tokens
                .csrf(csrf -> csrf.disable())
		.cors(cors -> {})
                // Disable browser-based authentication
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // JWT authentication is stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Login doesn't require authentication
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()

                        // Everything under admin requires ADMIN
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Faculty portal data is scoped to the authenticated faculty account
                        .requestMatchers("/api/faculty/**")
                        .hasRole("FACULTY")

                        // Student portal endpoints are restricted to STUDENT accounts
                        .requestMatchers("/api/student/**")
                        .hasRole("STUDENT")

                        // Other API endpoints require authentication
                        .anyRequest()
                        .authenticated()
                )

                // Read JWT before Spring's username/password filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        org.springframework.security.web.authentication
                                .UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}