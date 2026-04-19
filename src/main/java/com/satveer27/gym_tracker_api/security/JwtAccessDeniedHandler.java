package com.satveer27.gym_tracker_api.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("action=access_denied message={}", accessDeniedException.getMessage());
        String requestId = MDC.get("requestId");
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        String jsonResponse = String.format("{\"status\": 403, \"error\": \"Forbidden\", \"message\": \"You don't have permission\", \"requestId\": \"%s\"}",
                requestId);
        response.getWriter().write(jsonResponse);
    }
}
