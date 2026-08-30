package com.group5.lostandfoundjava.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A chat thread between exactly two users about one item.
 *
 * <p>There is no "owner" side: whoever starts the thread becomes {@code userA} and the other person
 * {@code userB}, which is why lookups have to check both orders.
 */
@Getter
@Setter
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    /** Required by JPA. */
    protected Conversation() {}

    public Conversation(Item item, User userA, User userB) {
        this.item = item;
        this.userA = userA;
        this.userB = userB;
    }

    /** True when the given user is one of the two people in this thread. */
    public boolean isParticipant(UUID userId) {
        return userA.getId().equals(userId) || userB.getId().equals(userId);
    }

    /** Given one participant, returns the other one — the person to notify about a new message. */
    public User otherParticipant(UUID userId) {
        return userA.getId().equals(userId) ? userB : userA;
    }
}
