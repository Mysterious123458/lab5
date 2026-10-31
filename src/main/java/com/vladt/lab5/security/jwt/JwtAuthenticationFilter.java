package com.vladt.lab5.security.jwt;

import com.vladt.lab5.model.AppUser;
import com.vladt.lab5.repository.UserRepository;
import com.vladt.lab5.security.AppUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Фильтр принимает только access-токены: refresh отклоняется проверкой claim type.
                Claims claims = jwtService.parseAccessToken(header.substring(7));
                userRepository.findByEmailIgnoreCase(claims.getSubject())
                        .filter(user -> user.getStatus().isActive())
                        .ifPresent(user -> {
                            AppUserPrincipal principal = new AppUserPrincipal(user);
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
                        });
            } catch (JwtException | IllegalArgumentException ignored) {
                // Невалидный access не аутентифицирует запрос: защищённый маршрут ответит 401.
            }
        }
        chain.doFilter(request, response);
    }
}
