package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.SavedItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Database access for {@link SavedItem}. */
@Repository
public interface SavedItemRepository extends JpaRepository<SavedItem, UUID> {

    Optional<SavedItem> findByUserIdAndItemId(UUID userId, UUID itemId);

    Page<SavedItem> findByUserId(UUID userId, Pageable pageable);
}
