package com.group5.lostandfoundjava.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before every request: if there is a valid {@code Authorization: Bearer ...} header, the
 * caller is recorded as authenticated for the rest of the request.
 *
 * <p>The filter never rejects anything. A missing or bad token simply leaves the request anonymous,
 * and Spring Security's own rules decide afterwards whether that is acceptable — which is what
 * makes endpoints like the public item search work.
 *
 * <p>The principal stored here is the user's {@link java.util.UUID}. That is why controllers can
 * write {@code @AuthenticationPrincipal UUID userId}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null
                && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            Claims claims = jwtProvider.parse(header.substring(BEARER_PREFIX.length()).trim());
            // A refresh token must not be usable as an access token.
            if (claims != null && jwtProvider.isAccessToken(claims)) {
                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                jwtProvider.userIdFrom(claims),
                                null,
                                List.of(new SimpleGrantedAuthority(jwtProvider.roleFrom(claims).authority())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
