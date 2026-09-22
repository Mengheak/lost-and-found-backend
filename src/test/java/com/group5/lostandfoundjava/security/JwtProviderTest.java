package com.group5.lostandfoundjava.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group5.lostandfoundjava.config.JwtProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    @Test
    void rejectsMissingOrWeakSigningSecrets() {
        assertThrows(IllegalStateException.class, () -> provider(null));
        assertThrows(IllegalStateException.class, () -> provider(""));
        assertThrows(IllegalStateException.class, () -> provider("too-short"));
    }

    private JwtProvider provider(String secret) {
        return new JwtProvider(new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7)));
    }
}
