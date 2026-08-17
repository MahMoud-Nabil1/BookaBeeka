package com.system.booking.modules.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. بنقفل الـ CSRF عشان إحنا شغالين بـ APIs و JWT مش بـ HTML Forms
                .csrf(AbstractHttpConfigurer::disable)

                // 2. بنقوله إحنا شغالين Stateless (يعني مفيش Sessions في الميموري، الاعتماد كله ع التوكين)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. قواعد الدخول (Authorization Rules)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // افتح كل بوابات اللوجن
                        .requestMatchers("/customers/register").permitAll() // افتح بوابة إنشاء حساب للعملاء
                        .requestMatchers("/api/admin/super/**").permitAll()   // TEMP - remove once JWT filter is wired
                        .requestMatchers("/api/payments/**").permitAll()      // TEMP - remove once JWT filter is wired
                        .anyRequest().authenticated() // أي رابط تاني في السيستم لازم يكون معاه توكين
                );

        return http.build();
    }
}