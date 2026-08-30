package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.Item;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Database access for {@link Item}.
 *
 * <p>{@link JpaSpecificationExecutor} adds {@code findAll(Specification, Pageable)}, which is what
 * the search endpoint uses: its eight optional filters would need 256 different method names
 * otherwise. See
 * {@link com.group5.lostandfoundjava.repository.specification.ItemSpecifications}.
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    Page<Item> findByUserId(UUID userId, Pageable pageable);
}
