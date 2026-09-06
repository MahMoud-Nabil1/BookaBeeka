package com.system.booking.modules.security.config;

import com.system.booking.modules.security.security.CustomAccessDeniedHandler;
import com.system.booking.modules.security.security.CustomAuthenticationEntryPoint;
import com.system.booking.modules.security.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsConfig corsConfig;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Auth & Onboarding endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/customers/register").permitAll()
                        .requestMatchers("/api/admin/super/login").permitAll()
                        .requestMatchers("/api/tenants/subdomain/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/availability/search").permitAll()

                        // Platform SuperAdmin only
                        .requestMatchers("/api/admin/super/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tenants/register").hasRole("SUPER_ADMIN")

                        // Hotel Owner endpoints
                        .requestMatchers("/api/v1/owner/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/tenants/me").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/tenants/me").hasRole("OWNER")

                        // Hotel Management (Owner & Hotel Admin)
                        .requestMatchers("/api/inventory/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/availability/room-blocks/**").hasAnyRole("OWNER", "ADMIN")

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}