package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.Token;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    boolean existsByTokenHashAndRevokedFalseAndExpiredFalseAndExpiresAtAfter(String tokenHash, Instant now);

    // Revokes a user's tokens in one statement instead of loading them and saving them back
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Token t set t.revoked = true, t.expired = true "
            + "where t.user.id = :userId and (t.revoked = false or t.expired = false)")
    int revokeAllTokensByUser(@Param("userId") UUID userId);

    // Atomic compare-and-set used when rotating a refresh token. Concurrent callers
    // contend on the same row; only the first active-token update can affect it.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Token t set t.revoked = true, t.expired = true "
            + "where t.tokenHash = :tokenHash and t.revoked = false and t.expired = false "
            + "and t.expiresAt > CURRENT_TIMESTAMP")
    int consumeActiveTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Token t where t.expiresAt <= :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
