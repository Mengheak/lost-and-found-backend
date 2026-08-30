package com.group5.lostandfoundjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Reputation left after an item changed hands. The unique constraint allows one rating per
 * (rater, rated user, item) triple, so a user cannot inflate someone's score by rating twice.
 */
@Getter
@Setter
@Entity
@Table(
        name = "ratings",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_ratings_from_to_item",
                        columnNames = {"from_user_id", "to_user_id", "item_id"}))
public class Rating extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "text")
    private String comment;

    /** Required by JPA. */
    protected Rating() {}

    public Rating(User fromUser, User toUser, Item item, int score, String comment) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.item = item;
        this.score = score;
        this.comment = comment;
    }
}
