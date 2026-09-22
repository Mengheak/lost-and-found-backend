package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.config.LoginThrottleProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final LoginThrottleProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public Long lockoutSecondsRemaining(String email) {
        Instant now = clock.instant();
        AtomicReference<Long> remaining = new AtomicReference<>();
        attemptsByEmail.computeIfPresent(normalize(email), (key, attempts) -> {
            if (attempts.lockedUntil() == null) {
                return attempts;
            }
            if (!now.isBefore(attempts.lockedUntil())) {
                return null;
            }
            long millis = Duration.between(now, attempts.lockedUntil()).toMillis();
            remaining.set(Math.max(1L, (millis + 999L) / 1000L));
            return attempts;
        });
        return remaining.get();
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        attemptsByEmail.compute(normalize(email), (key, existing) -> {
            boolean stale = existing == null
                    || !now.isBefore(existing.firstFailureAt().plus(properties.attemptWindow()));
            int count = stale ? 1 : existing.count() + 1;
            Instant firstFailureAt = stale ? now : existing.firstFailureAt();
            Instant lockedUntil = count >= properties.maxAttempts()
                    ? now.plus(properties.lockoutDuration())
                    : null;
            return new Attempts(count, firstFailureAt, lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempts(int count, Instant firstFailureAt, Instant lockedUntil) {}
}
