package com.group5.lostandfoundjava.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A trimmed-down version of Spring Data's {@link Page}.
 *
 * <p>{@code Page} serialises a lot of internal detail that clients do not need, and its JSON shape
 * is not guaranteed to stay stable between Spring versions. Converting it here keeps the API
 * contract in our own hands.
 *
 * @param <T> type of a single row in {@code content}
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
