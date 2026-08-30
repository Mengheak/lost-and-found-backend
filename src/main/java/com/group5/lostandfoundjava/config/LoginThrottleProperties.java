package com.group5.lostandfoundjava.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Everything under {@code app.login-throttle}: how brute-force protection behaves.
 *
 * @param maxAttempts failed logins allowed for one email before it is locked
 * @param lockoutDuration how long the lockout lasts once it triggers
 * @param attemptWindow failures further apart than this do not count towards the same streak
 */
@ConfigurationProperties(prefix = "app.login-throttle")
public record LoginThrottleProperties(
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("15m") Duration lockoutDuration,
        @DefaultValue("15m") Duration attemptWindow) {}
