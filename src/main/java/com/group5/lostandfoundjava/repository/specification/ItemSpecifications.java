package com.group5.lostandfoundjava.repository.specification;

import com.group5.lostandfoundjava.dto.item.ItemSearchFilter;
import com.group5.lostandfoundjava.entity.Item;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the WHERE clause of the item search.
 *
 * <p>A {@link Specification} is a small object that knows how to add conditions to a query. Each
 * filter that the caller actually supplied contributes one condition; the rest are skipped. All the
 * conditions are then joined with AND.
 *
 * <p>The class is final with a private constructor because it only holds a static helper — there is
 * nothing to instantiate.
 */
public final class ItemSpecifications {

    private ItemSpecifications() {}

    public static Specification<Item> matching(ItemSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (hasText(filter.brand())) {
                predicates.add(cb.like(cb.lower(root.get("brand")), contains(filter.brand())));
            }
            if (hasText(filter.color())) {
                predicates.add(cb.like(cb.lower(root.get("color")), contains(filter.color())));
            }
            if (hasText(filter.keyword())) {
                // A keyword matches either the title or the description.
                String like = contains(filter.keyword());
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), like),
                                cb.like(cb.lower(root.get("description")), like)));
            }
            if (filter.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateTime"), filter.dateFrom()));
            }
            if (filter.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateTime"), filter.dateTo()));
            }

            // An empty list means "no filters at all", which cb.and() turns into "always true".
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
