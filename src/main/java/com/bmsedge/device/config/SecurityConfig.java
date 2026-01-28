package com.bmsedge.device.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security Configuration for Device Management API
 * Works in conjunction with CorsConfig.java
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ============================================
                // CORS CONFIGURATION
                // ============================================
                // Enable CORS with configuration from CorsConfig
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ============================================
                // CSRF CONFIGURATION
                // ============================================
                // REST API → disable CSRF
                // Note: Enable CSRF for production if using cookie-based auth
                .csrf(csrf -> csrf.disable())

                // ============================================
                // SESSION MANAGEMENT
                // ============================================
                // Stateless session for REST API
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ============================================
                // AUTHORIZATION RULES
                // ============================================
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/public/**",
                                "/api/health",
                                "/api/status",
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Device Management endpoints (authentication required in production)
                        // For DEV: permitAll() - For PROD: authenticated()
                        .requestMatchers("/api/devices/**").permitAll()
                        .requestMatchers("/api/locations/**").permitAll()
                        .requestMatchers("/api/segments/**").permitAll()
                        .requestMatchers("/api/counters/**").permitAll()
                        .requestMatchers("/api/device-data/**").permitAll()
                        .requestMatchers("/api/mqtt-data/**").permitAll()

                        // All other requests require authentication (PROD)
                        // For DEV: permitAll() - For PROD: authenticated()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}