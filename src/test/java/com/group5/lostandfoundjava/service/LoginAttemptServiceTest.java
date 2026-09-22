package com.group5.lostandfoundjava.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.group5.lostandfoundjava.config.LoginThrottleProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private final MutableClock clock = new MutableClock(START);
    private final LoginAttemptService service = new LoginAttemptService(
            new LoginThrottleProperties(3, Duration.ofMinutes(10), Duration.ofMinutes(5)), clock);

    @Test
    void locksAfterConfiguredFailuresAndNormalizesEmail() {
        service.recordFailure(" Jane@Example.COM ");
        service.recordFailure("jane@example.com");
        assertNull(service.lockoutSecondsRemaining("JANE@example.com"));

        service.recordFailure("jane@example.com");

        assertEquals(600L, service.lockoutSecondsRemaining(" jane@example.com "));
    }

    @Test
    void expiredLockClearsTheFailureStreak() {
        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");
        clock.advance(Duration.ofMinutes(10));

        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
        service.recordFailure("jane@example.com");
        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    void failuresOutsideTheAttemptWindowStartANewStreak() {
        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");
        clock.advance(Duration.ofMinutes(5));

        service.recordFailure("jane@example.com");

        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    @Test
    void successfulLoginClearsFailures() {
        service.recordFailure("jane@example.com");
        service.recordFailure("jane@example.com");
        service.recordSuccess("jane@example.com");
        service.recordFailure("jane@example.com");

        assertNull(service.lockoutSecondsRemaining("jane@example.com"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
