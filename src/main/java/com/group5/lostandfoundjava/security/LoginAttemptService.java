package com.group5.lostandfoundjava.security;

import com.group5.lostandfoundjava.config.LoginThrottleProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection for the login endpoint: too many failures on one email lock that email out
 * for a while, even if the correct password is then supplied.
 *
 * <p>The counters live in memory, in a {@link ConcurrentHashMap} because several requests can be
 * handled at the same time. That means the counters reset when the app restarts and are not shared
 * between instances — fine for a single deployment, but a shared store such as Redis would be needed
 * to scale out.
 */
@Service
public class LoginAttemptService {

    /** One email's failure streak. A record is immutable, so it is replaced rather than mutated. */
    private record Attempts(int count, Instant firstFailureAt, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    private final LoginThrottleProperties properties;
    private final Clock clock;

    public LoginAttemptService(LoginThrottleProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** Package-private so tests can hand in a clock they control instead of waiting 15 minutes. */
    LoginAttemptService(LoginThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @return seconds left on the lockout, or {@code null} when the email is not locked out. An
     *     expired lockout is cleaned up here and reported as "not locked"
     */
    public Long lockoutSecondsRemaining(String email) {
        Attempts record = attempts.get(key(email));
        if (record == null || record.lockedUntil() == null) {
            return null;
        }
        Duration remaining = Duration.between(clock.instant(), record.lockedUntil());
        if (remaining.isNegative() || remaining.isZero()) {
            attempts.remove(key(email));
            return null;
        }
        return Math.max(remaining.getSeconds(), 1);
    }

    /** Counts one failed login, locking the email once the configured limit is reached. */
    public void recordFailure(String email) {
        Instant now = clock.instant();
        attempts.compute(
                key(email),
                (key, existing) -> {
                    // A streak that started longer ago than the window has expired; start a new one.
                    boolean stale = existing == null
                            || Duration.between(existing.firstFailureAt(), now)
                                            .compareTo(properties.attemptWindow())
                                    >= 0;

                    int count = stale ? 1 : existing.count() + 1;
                    Instant firstFailureAt = stale ? now : existing.firstFailureAt();
                    Instant lockedUntil =
                            count >= properties.maxAttempts() ? now.plus(properties.lockoutDuration()) : null;

                    return new Attempts(count, firstFailureAt, lockedUntil);
                });
    }

    /** A successful login wipes the streak, so occasional typos never add up to a lockout. */
    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    /** Normalises the key so "Jane@Example.COM" and "jane@example.com" share one counter. */
    private String key(String email) {
        return email.trim().toLowerCase();
    }
}
