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

// Creates and reads JSON Web Tokens
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
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET is required");
        }
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = properties.accessTokenTtl();
        this.refreshTtl = properties.refreshTokenTtl();
    }

    // Reported to the client so it knows when to refresh
    public long getAccessTokenTtlSeconds() {
        return accessTtl.getSeconds();
    }

    public String generateAccessToken(UUID userId, Role role) {
        return generate(userId, accessTtl, TYPE_ACCESS, role);
    }

    public String generateRefreshToken(UUID userId) {
        return generate(userId, refreshTtl, TYPE_REFRESH, null);
    }

    // Verifies the signature and expiry and returns the token's contents
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    // The sub claim: which user the token belongs
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
                        // A JWT ID makes every token unique
                        .id(UUID.randomUUID().toString())
                        .claim(CLAIM_TYPE, type)
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(now.plus(ttl)));
        if (role != null) {
            builder.claim(CLAIM_ROLE, role.name());
        }
        return builder.signWith(key).compact();
    }
}
