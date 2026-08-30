package com.group5.lostandfoundjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * Fields shared by every table: the primary key and the two timestamps.
 *
 * <p>{@code @MappedSuperclass} means "copy these columns into each entity that extends me" — it is
 * not a table of its own.
 *
 * <p>The id is a random {@link UUID} generated in Java rather than a database sequence. That lets
 * the application know an object's id before it is saved, and makes ids safe to expose in URLs
 * because they cannot be guessed or counted.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /** Hibernate calls this automatically right before an UPDATE statement. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Two entities are the same row when they have the same type and the same id. Comparing on the
     * id only (never on the other fields) is the safe rule for JPA entities, because Hibernate may
     * hand out proxies and partially loaded copies of the same row.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity that)) {
            return false;
        }
        return getClass() == other.getClass() && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
