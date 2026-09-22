package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.entity.Token;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.TokenType;
import com.group5.lostandfoundjava.repository.TokenRepository;
import com.group5.lostandfoundjava.service.TokenService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenRepository tokenRepository;

    @Override
    @Transactional
    public void save(User user, String token) {
        tokenRepository.save(new Token(user, token, TokenType.BEARER));
    }

    @Override
    @Transactional
    public void savePair(User user, String accessToken, String refreshToken) {
        save(user, accessToken);
        save(user, refreshToken);
    }

    @Override
    @Transactional
    public int revokeAll(UUID userId) {
        return tokenRepository.revokeAllTokensByUser(userId);
    }

    @Override
    @Transactional
    public boolean consume(String token) {
        return tokenRepository.consumeActiveToken(token) == 1;
    }

    // Runs on every authenticated request
    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String token) {
        return tokenRepository.findByToken(token).map(Token::isActive).orElse(false);
    }
}
