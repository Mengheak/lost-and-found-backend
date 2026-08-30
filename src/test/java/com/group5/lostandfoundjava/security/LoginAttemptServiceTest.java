package com.group5.lostandfoundjava.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.group5.lostandfoundjava.config.LoginThrottleProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lockout behaviour, driven by a fake clock.
 *
 * <p>This test lives in the same package as the class it tests so it can use the package-private
 * constructor that takes a {@link Clock}. Waiting 15 real minutes for a lockout to expire is not an
 * option, so time is moved forward by hand instead.
 */
class LoginAttemptServiceTest {

    /** A clock that only moves when the test tells it to. */
    private static class TestClock extends Clock {
        private Instant now;

        TestClock(Instant now) {
            this.now = now;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        void advance(Duration amount) {
            now = now.plus(amount);
        }
    }

    private final TestClock clock = new TestClock(Instant.parse("2026-01-01T00:00:00Z"));

    private LoginAttemptService service(int maxAttempts, Duration lockout, Duration window) {
        return new LoginAttemptService(new LoginThrottleProperties(maxAttempts, lockout, window), clock);
    }

    private LoginAttemptService service(int maxAttempts) {
        return service(maxAttempts, Duration.ofMinutes(15), Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("a fresh email is never locked out")
    void freshEmailIsNotLockedOut() {
        assertNull(service(3).lockoutSecondsRemaining("nobody@example.com"));
    }

    @Test
    @DisplayName("failures below the limit do not lock the account")
    void failuresBelowLimitDoNotLock() {
        LoginAttemptService service = service(3);

        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");

        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    @DisplayName("reaching the limit locks the account for the configured duration")
    void reachingLimitLocksAccount() {
        LoginAttemptService service = service(3, Duration.ofMinutes(15), Duration.ofMinutes(15));

        for (int i = 0; i < 3; i++) {
            service.recordFailure("jane@example.com");
        }

        assertEquals(15 * 60L, service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    @DisplayName("a successful login clears the streak")
    void successClearsStreak() {
        LoginAttemptService service = service(3);
        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");

        service.recordSuccess("jane@example.com");
        service.recordFailure("jane@example.com");

        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    @DisplayName("the lockout lifts once its duration has passed")
    void lockoutExpires() {
        LoginAttemptService service = service(2, Duration.ofMinutes(15), Duration.ofMinutes(15));
        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");
        assertNotNull(service.lockoutSecondsRemaining("jane@example.com"));

        clock.advance(Duration.ofMinutes(15));

        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    @DisplayName("failures spread beyond the window never accumulate into a lockout")
    void failuresOutsideWindowDoNotAccumulate() {
        LoginAttemptService service = service(3, Duration.ofMinutes(15), Duration.ofMinutes(15));

        for (int i = 0; i < 5; i++) {
            service.recordFailure("jane@example.com");
            clock.advance(Duration.ofMinutes(20));
        }

        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    @DisplayName("failures inside the window do accumulate")
    void failuresInsideWindowAccumulate() {
        LoginAttemptService service = service(3, Duration.ofMinutes(15), Duration.ofMinutes(15));

        for (int i = 0; i < 3; i++) {
            service.recordFailure("jane@example.com");
            clock.advance(Duration.ofMinutes(1));
        }

        assertNotNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    @DisplayName("the email key is normalised")
    void emailKeyIsNormalised() {
        LoginAttemptService service = service(2);

        service.recordFailure("  Jane@Example.COM ");
        service.recordFailure("jane@example.com");

        assertNotNull(service.lockoutSecondsRemaining("JANE@example.com"));
    }

    @Test
    @DisplayName("locking one account leaves others alone")
    void lockingOneAccountLeavesOthersAlone() {
        LoginAttemptService service = service(2);

        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");

        assertNotNull(service.lockoutSecondsRemaining("jane@example.com"));
        assertNull(service.lockoutSecondsRemaining("john@example.com"));
    }
}
