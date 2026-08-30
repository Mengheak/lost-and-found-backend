package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.Notification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Database access for {@link Notification}. */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    /**
     * Marks the whole feed as read in a single UPDATE statement instead of loading every row.
     * {@code @Modifying} tells Spring Data this query writes rather than reads.
     *
     * @return how many rows were changed
     */
    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllRead(@Param("userId") UUID userId);
}
