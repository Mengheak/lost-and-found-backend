package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Database access for {@link Conversation}.
 *
 * <p>These two queries are written by hand with {@code @Query} because a method name would be
 * unreadable: the participants can be stored in either order, so both combinations must be checked.
 * The language is JPQL — it names entities and fields, not tables and columns.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Finds the existing thread for an item between two users, whichever way round they are stored. */
    @Query(
            """
            select c from Conversation c
            where c.item.id = :itemId
              and ((c.userA.id = :userId1 and c.userB.id = :userId2)
                or (c.userA.id = :userId2 and c.userB.id = :userId1))
            """)
    Optional<Conversation> findByItemAndParticipants(
            @Param("itemId") UUID itemId, @Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    /** Every thread the given user takes part in, on either side. */
    @Query("select c from Conversation c where c.userA.id = :userId or c.userB.id = :userId")
    Page<Conversation> findAllForUser(@Param("userId") UUID userId, Pageable pageable);
}
