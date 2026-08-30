package com.group5.lostandfoundjava.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * One entry in a user's personal shortlist. The unique constraint means the same user cannot save
 * the same item twice, even if two requests arrive at the same moment.
 */
@Getter
@Setter
@Entity
@Table(
        name = "saved_items",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_saved_items_user_item",
                        columnNames = {"user_id", "item_id"}))
public class SavedItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** Required by JPA. */
    protected SavedItem() {}

    public SavedItem(User user, Item item) {
        this.user = user;
        this.item = item;
    }
}
