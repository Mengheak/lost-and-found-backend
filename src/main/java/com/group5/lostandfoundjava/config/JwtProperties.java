package com.group5.lostandfoundjava.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Everything under {@code app.jwt} in {@code application.yml}, bound into a typed object so a typo
 * in the configuration fails at startup instead of at midnight.
 *
 * @param secret HS256 signing key; must be at least 32 characters and must be overridden outside
 *     local development
 * @param accessTokenTtl how long an access token stays valid, e.g. {@code 15m}
 * @param refreshTokenTtl how long a refresh token stays valid, e.g. {@code 7d}
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl) {}
