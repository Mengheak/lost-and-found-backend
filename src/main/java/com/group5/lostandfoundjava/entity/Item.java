package com.group5.lostandfoundjava.entity;

import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "items")
public class Item extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemType type;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private String brand;

    private String color;

    /** Photo links live in their own small table, {@code item_photo_urls}. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "item_photo_urls", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "photo_url", nullable = false)
    private List<String> photoUrls = new ArrayList<>();

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lng")
    private Double locationLng;

    /** When the item was lost or found — not when the report was created. */
    @Column(name = "date_time")
    private Instant dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.OPEN;

    /** Only meaningful when {@link #type} is {@code LOST}. */
    @Column(name = "reward_amount", precision = 12, scale = 2)
    private BigDecimal rewardAmount;

    /** Only meaningful when {@link #type} is {@code FOUND}. */
    @Column(name = "storage_location")
    private String storageLocation;

    /** Required by JPA. */
    protected Item() {}

    /**
     * Creates an item with the four fields that are always required. Everything else is optional
     * and set afterwards with the generated setters.
     */
    public Item(User user, Category category, ItemType type, String name) {
        this.user = user;
        this.category = category;
        this.type = type;
        this.name = name;
    }
}
