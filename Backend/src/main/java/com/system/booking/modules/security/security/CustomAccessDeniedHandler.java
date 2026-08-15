package com.system.booking.modules.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"Forbidden\",\"message\":\"You do not have permission to access this resource\",\"path\":\"%s\"}",
                Instant.now(),
                HttpServletResponse.SC_FORBIDDEN,
                request.getRequestURI()
        );

        response.getWriter().write(jsonResponse);
    }
}