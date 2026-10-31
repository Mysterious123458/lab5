package com.vladt.lab5.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\": \"" + messageFor(accessDeniedException) + "\"}");
    }

    private String messageFor(AccessDeniedException exception) {
        Throwable cause = exception.getCause();
        if (exception instanceof InvalidCsrfTokenException || exception instanceof MissingCsrfTokenException
                || cause instanceof InvalidCsrfTokenException || cause instanceof MissingCsrfTokenException) {
            return "Запрос изменяет данные и требует действительный CSRF-токен";
        }
        return "Недостаточно прав для этой операции";
    }
}
