package com.group5.lostandfoundjava.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupJob {

    private final TokenService tokenService;

    public TokenCleanupJob(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Scheduled(cron = "${TOKEN_CLEANUP_CRON:0 0 * * * *}")
    public void removeExpiredTokens() {
        tokenService.purgeExpired();
    }
}
