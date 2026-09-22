package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.entity.Token;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.TokenType;
import com.group5.lostandfoundjava.repository.TokenRepository;
import com.group5.lostandfoundjava.security.JwtProvider;
import com.group5.lostandfoundjava.service.TokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final Clock clock;

    @Override
    @Transactional
    public void savePair(User user, String accessToken, String refreshToken) {
        save(user, accessToken);
        save(user, refreshToken);
    }

    private void save(User user, String token) {
        tokenRepository.save(new Token(
                user, fingerprint(token), jwtProvider.expiresAt(token), TokenType.BEARER));
    }

    @Override
    @Transactional
    public int revokeAll(UUID userId) {
        return tokenRepository.revokeAllTokensByUser(userId);
    }

    @Override
    @Transactional
    public boolean consume(String token) {
        return tokenRepository.consumeActiveTokenHash(fingerprint(token)) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String token) {
        return tokenRepository.existsByTokenHashAndRevokedFalseAndExpiredFalseAndExpiresAtAfter(
                fingerprint(token), clock.instant());
    }

    @Override
    @Transactional
    public int purgeExpired() {
        return tokenRepository.deleteExpiredBefore(clock.instant());
    }

    private String fingerprint(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
