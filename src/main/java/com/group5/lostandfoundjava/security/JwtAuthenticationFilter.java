package com.group5.lostandfoundjava.security;

import com.group5.lostandfoundjava.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before every request: if there is a valid {@code Authorization: Bearer ...} header, the
 * caller is recorded as authenticated for the rest of the request.
 *
 * <p>A token has to clear three checks to count. It must parse and verify against our signing key
 * and not be past its expiry; it must be an <em>access</em> token, so a refresh token cannot be used
 * to call the API; and it must still be active in the token store, which is what makes logging out
 * take effect immediately rather than whenever the token would have expired on its own.
 *
 * <p>The filter never rejects anything. A missing or bad token simply leaves the request anonymous,
 * and Spring Security's own rules decide afterwards whether that is acceptable — which is what makes
 * endpoints like the public item search work.
 *
 * <p>The principal stored here is the user's {@link java.util.UUID}. That is why controllers can
 * write {@code @AuthenticationPrincipal UUID userId}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, TokenService tokenService) {
        this.jwtProvider = jwtProvider;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null
                && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(BEARER_PREFIX.length()).trim();
            Claims claims = jwtProvider.parse(token);

            // The signature check is cheap and the store lookup is a database round trip, so the
            // token is only looked up once it has already proved to be one of ours.
            if (claims != null && jwtProvider.isAccessToken(claims) && tokenService.isActive(token)) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        jwtProvider.userIdFrom(claims),
                        null,
                        jwtProvider.roleFrom(claims).getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
