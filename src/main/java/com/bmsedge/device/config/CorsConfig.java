package com.bmsedge.device.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for Device Management API
 * Allows frontend applications to make cross-origin requests
 */
@Configuration
public class CorsConfig {

    /**
     * Configure CORS settings for the entire application
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ============================================
        // ALLOWED ORIGINS
        // ============================================
        // DEV: Allow localhost with different ports
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",           // React dev server
                "http://localhost:3001",           // Alternative React port
                "http://localhost:5173",           // Vite dev server
                "http://localhost:5174",           // Alternative Vite port
                "http://localhost:4200",           // Angular dev server
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        ));

        // PROD: Add your production domains here
        // configuration.addAllowedOrigin("https://your-production-domain.com");
        // configuration.addAllowedOrigin("https://app.your-domain.com");

        // Alternative: Allow all origins (NOT recommended for production)
        // configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // ============================================
        // ALLOWED HTTP METHODS
        // ============================================
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS",
                "HEAD"
        ));

        // ============================================
        // ALLOWED HEADERS
        // ============================================
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-API-Key",
                "X-Device-Id",
                "X-User-Id"
        ));

        // ============================================
        // EXPOSED HEADERS
        // ============================================
        // Headers that the frontend can access in the response
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count",
                "X-Page",
                "X-Per-Page",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"
        ));

        // ============================================
        // CREDENTIALS
        // ============================================
        // Allow cookies and authentication headers
        configuration.setAllowCredentials(true);

        // ============================================
        // MAX AGE
        // ============================================
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        // ============================================
        // APPLY TO ALL ENDPOINTS
        // ============================================
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Alternative: CORS Filter Bean
     * Use this if you need more control over the filter chain
     */
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}