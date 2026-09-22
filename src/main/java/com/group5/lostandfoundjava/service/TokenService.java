package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.entity.User;
import java.util.UUID;

// The lifecycle of issued tokens: recording them, revoking them
public interface TokenService {

    // Records a freshly issued token so it can be revoked later
    void save(User user, String token);

    // Records an access/refresh pair issued together
    void savePair(User user, String accessToken, String refreshToken);

    // Marks every token the user currently holds as revoked
    int revokeAll(UUID userId);

    // Atomically consumes one active token. Exactly one concurrent caller can succeed.
    boolean consume(String token);

    boolean isActive(String token);
}
