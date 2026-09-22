package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.entity.Token;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.TokenRepository;
import com.group5.lostandfoundjava.security.JwtProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TokenServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final TokenServiceImpl service = new TokenServiceImpl(
            tokenRepository, jwtProvider, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void savesOnlyFingerprintsAndJwtExpirations() {
        User user = new User("Jane", "jane@example.com", null, "hash", Role.USER);
        Instant accessExpiry = NOW.plusSeconds(900);
        Instant refreshExpiry = NOW.plusSeconds(604_800);
        when(jwtProvider.expiresAt("access-token")).thenReturn(accessExpiry);
        when(jwtProvider.expiresAt("refresh-token")).thenReturn(refreshExpiry);
        ArgumentCaptor<Token> saved = ArgumentCaptor.forClass(Token.class);
        when(tokenRepository.save(saved.capture())).thenAnswer(call -> call.getArgument(0));

        service.savePair(user, "access-token", "refresh-token");

        assertEquals(2, saved.getAllValues().size());
        assertEquals(fingerprint("access-token"), saved.getAllValues().get(0).getTokenHash());
        assertEquals(accessExpiry, saved.getAllValues().get(0).getExpiresAt());
        assertEquals(fingerprint("refresh-token"), saved.getAllValues().get(1).getTokenHash());
        assertEquals(refreshExpiry, saved.getAllValues().get(1).getExpiresAt());
        assertNotEquals("access-token", saved.getAllValues().get(0).getTokenHash());
    }

    @Test
    void consumeHashesThePresentedTokenAndSucceedsOnlyOnce() {
        String winnerHash = fingerprint("winner");
        String replayHash = fingerprint("replay");
        when(tokenRepository.consumeActiveTokenHash(winnerHash)).thenReturn(1);
        when(tokenRepository.consumeActiveTokenHash(replayHash)).thenReturn(0);

        assertTrue(service.consume("winner"));
        assertFalse(service.consume("replay"));
        verify(tokenRepository).consumeActiveTokenHash(winnerHash);
        verify(tokenRepository).consumeActiveTokenHash(replayHash);
    }

    @Test
    void activeLookupUsesFingerprintAndCurrentTime() {
        String tokenHash = fingerprint("access-token");
        when(tokenRepository.existsByTokenHashAndRevokedFalseAndExpiredFalseAndExpiresAtAfter(tokenHash, NOW))
                .thenReturn(true);

        assertTrue(service.isActive("access-token"));
    }

    @Test
    void cleanupDeletesRowsPastTheirJwtExpiry() {
        when(tokenRepository.deleteExpiredBefore(NOW)).thenReturn(3);

        assertEquals(3, service.purgeExpired());
        verify(tokenRepository).deleteExpiredBefore(NOW);
    }

    private String fingerprint(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
