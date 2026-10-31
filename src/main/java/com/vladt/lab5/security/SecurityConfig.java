package com.vladt.lab5.security;

import com.vladt.lab5.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler restAccessDeniedHandler,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // Токен загружается на каждый запрос, чтобы cookie XSRF-TOKEN всегда был актуальным.
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        http
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/login", "/auth/register", "/auth/refresh").permitAll()
                    .requestMatchers("/api/auth/register", "/api/security/csrf", "/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/*").hasAnyRole("DISPATCHER", "ADMIN")
                    .requestMatchers("/api/users/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/city-objects/**", "/api/work-crews/**", "/api/work-orders/**")
                        .hasAnyRole("DISPATCHER", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/city-objects/**", "/api/work-crews/**", "/api/work-orders/**")
                        .hasAnyRole("DISPATCHER", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/city-objects/**", "/api/work-crews/**", "/api/work-orders/**")
                        .hasAnyRole("DISPATCHER", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/city-reports/*/assign", "/api/city-reports/*/reject",
                            "/api/work-orders/*/complete", "/api/work-orders/*/reassign", "/api/city-objects/*/mark-emergency")
                        .hasAnyRole("DISPATCHER", "ADMIN")
                    .anyRequest().authenticated())
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfRequestHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
