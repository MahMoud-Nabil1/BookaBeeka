package com.system.booking.modules.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"%s\"}",
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                request.getRequestURI()
        );

        response.getWriter().write(jsonResponse);
    }
}