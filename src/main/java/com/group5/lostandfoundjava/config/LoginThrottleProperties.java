package com.group5.lostandfoundjava.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.login-throttle")
public record LoginThrottleProperties(
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("15m") Duration lockoutDuration,
        @DefaultValue("15m") Duration attemptWindow) {

    public LoginThrottleProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("app.login-throttle.max-attempts must be at least 1");
        }
        if (lockoutDuration.isZero() || lockoutDuration.isNegative()) {
            throw new IllegalArgumentException("app.login-throttle.lockout-duration must be positive");
        }
        if (attemptWindow.isZero() || attemptWindow.isNegative()) {
            throw new IllegalArgumentException("app.login-throttle.attempt-window must be positive");
        }
    }
}
