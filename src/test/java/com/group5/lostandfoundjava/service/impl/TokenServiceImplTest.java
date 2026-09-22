package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.repository.TokenRepository;
import org.junit.jupiter.api.Test;

class TokenServiceImplTest {

    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final TokenServiceImpl service = new TokenServiceImpl(tokenRepository);

    @Test
    void consumeSucceedsOnlyWhenTheConditionalUpdateChangesOneRow() {
        when(tokenRepository.consumeActiveToken("winner")).thenReturn(1);
        when(tokenRepository.consumeActiveToken("replay")).thenReturn(0);

        assertTrue(service.consume("winner"));
        assertFalse(service.consume("replay"));
        verify(tokenRepository).consumeActiveToken("winner");
        verify(tokenRepository).consumeActiveToken("replay");
    }
}
