package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.entity.User;
import java.util.UUID;

/**
 * The lifecycle of issued tokens: recording them, revoking them, and answering whether one is still
 * accepted.
 *
 * <p>This is the only place that touches
 * {@link com.group5.lostandfoundjava.repository.TokenRepository}, so the authentication filter, the
 * WebSocket interceptor and the sign-in service all decide "is this token still good?" by the same
 * rule instead of three similar-looking copies of it.
 */
public interface TokenService {

    /** Records a freshly issued token so it can be revoked later. */
    void save(User user, String token);

    /** Records an access/refresh pair issued together. */
    void savePair(User user, String accessToken, String refreshToken);

    /**
     * Marks every token the user currently holds as revoked.
     *
     * @return how many rows were affected
     */
    int revokeAll(UUID userId);

    /** Revokes a single token, leaving the user's other sessions alone. */
    void revoke(String token);

    /**
     * @return {@code true} when the token was issued by us and has not been revoked since. A token
     *     that was never recorded counts as inactive
     */
    boolean isActive(String token);
}
