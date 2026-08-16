package com.system.booking.modules.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cross-Origin Resource Sharing (CORS) configuration for the REST API.
 *
 * <p>This configuration is registered as the first layer in the Spring Security
 * filter chain (via {@code SecurityConfig}), ensuring that preflight OPTIONS
 * requests are handled <b>before</b> authentication kicks in.</p>
 *
 * <p><b>Current Profile: Development.</b> All origins, methods, and headers are
 * permitted. For production, restrict {@code allowedOrigins} to your actual
 * frontend domain(s).</p>
 */
@Configuration
public class CorsConfig {

    /**
     * Defines the CORS rules applied to all API endpoints.
     *
     * <p>Exposed as a Spring bean so that the {@code SecurityFilterChain} can
     * reference it via {@code http.cors(cors -> cors.configurationSource(...))}.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Development defaults — lock these down for production
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
