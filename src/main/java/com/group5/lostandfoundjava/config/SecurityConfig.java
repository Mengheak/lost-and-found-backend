package com.group5.lostandfoundjava.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Who may call what.
 *
 * <p>{@code @EnableMethodSecurity} additionally turns on {@code @PreAuthorize}, which the category
 * and admin controllers use for rules that are easier to read next to the method they protect.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // No server-side sessions — every request proves who it is with its own token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/ws/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items/my", "/api/users/me")
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/items",
                                "/api/items/*",
                                "/api/categories",
                                "/api/categories/*",
                                "/api/users/*",
                                "/api/ratings/user/*")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> {
                    // Not signed in -> 401. Signed in but not allowed -> 403. Both in our envelope.
                    ex.authenticationEntryPoint((request, response, authException) ->
                            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"));
                    ex.accessDeniedHandler((request, response, deniedException) ->
                            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied"));
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Security errors happen before any controller runs, so the envelope is written by hand here. */
    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message));
    }
}
