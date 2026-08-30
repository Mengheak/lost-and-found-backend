package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.Rating;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Database access for {@link Rating}. */
@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    /** Guards the "one rating per rater, rated user and item" rule before the insert is attempted. */
    boolean existsByFromUserIdAndToUserIdAndItemId(UUID fromUserId, UUID toUserId, UUID itemId);

    Page<Rating> findByToUserId(UUID toUserId, Pageable pageable);

    /**
     * Average of every score a user has received. {@code coalesce} turns the {@code null} that SQL
     * returns for an empty set into 0, so the caller never has to handle it.
     */
    @Query("select coalesce(avg(r.score), 0) from Rating r where r.toUser.id = :userId")
    double averageScoreFor(@Param("userId") UUID userId);
}
