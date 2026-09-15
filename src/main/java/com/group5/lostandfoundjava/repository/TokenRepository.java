package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.Token;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByToken(String token);

    // Every token of one user that has not been revoked or expired yet
    @Query("select t from Token t where t.user.id = :userId and t.revoked = false and t.expired = false")
    List<Token> findAllActiveTokensByUser(@Param("userId") UUID userId);

    // Revokes a user's tokens in one statement instead of loading them and saving them back
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Token t set t.revoked = true, t.expired = true "
            + "where t.user.id = :userId and (t.revoked = false or t.expired = false)")
    int revokeAllTokensByUser(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
