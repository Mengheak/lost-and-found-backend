package com.group5.lostandfoundjava.security;

import com.group5.lostandfoundjava.config.JwtProperties;
import com.group5.lostandfoundjava.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Creates and reads JSON Web Tokens. This is the only class in the app that knows the token format.
 *
 * <p>Two kinds of token are issued, told apart by a {@code type} claim:
 *
 * <ul>
 *   <li><b>access</b> — short-lived, sent with every request, and carries the user's role so that
 *       authorising a request needs no database lookup
 *   <li><b>refresh</b> — long-lived, only accepted by {@code /api/auth/refresh}, and deliberately
 *       carries no role, so a role change takes effect on the next refresh
 * </ul>
 *
 * <p>The token is signed, not encrypted: anyone can read its contents, but nobody can change them
 * without the secret.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = properties.accessTokenTtl();
        this.refreshTtl = properties.refreshTokenTtl();
    }

    /** Reported to the client so it knows when to refresh. */
    public long getAccessTokenTtlSeconds() {
        return accessTtl.getSeconds();
    }

    public String generateAccessToken(UUID userId, Role role) {
        return generate(userId, accessTtl, TYPE_ACCESS, role);
    }

    public String generateRefreshToken(UUID userId) {
        return generate(userId, refreshTtl, TYPE_REFRESH, null);
    }

    /**
     * Verifies the signature and expiry and returns the token's contents.
     *
     * @return the claims, or {@code null} when the token is missing, malformed, tampered with or
     *     expired. Callers treat all of those the same way, so there is no reason to distinguish them
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /** The {@code sub} claim: which user the token belongs to. */
    public UUID userIdFrom(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public Role roleFrom(Claims claims) {
        return Role.fromNameOrDefault(claims.get(CLAIM_ROLE, String.class));
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE));
    }

    private String generate(UUID userId, Duration ttl, String type, Role role) {
        Instant now = Instant.now();
        JwtBuilder builder =
                Jwts.builder()
                        .subject(userId.toString())
                        .claim(CLAIM_TYPE, type)
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(now.plus(ttl)));
        if (role != null) {
            builder.claim(CLAIM_ROLE, role.name());
        }
        return builder.signWith(key).compact();
    }
}
