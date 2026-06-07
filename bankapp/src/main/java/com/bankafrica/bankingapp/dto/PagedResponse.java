package com.bankafrica.bankingapp.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A stable, explicit pagination envelope. We map Spring Data's {@link Page} into this record
 * rather than serialising {@code PageImpl} directly — {@code PageImpl}'s JSON shape is not
 * contractually stable across versions (Spring even warns about it), whereas these fields are
 * a documented part of our API.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /** Builds a response from a {@link Page} of entities, mapping each element to a DTO. */
    public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
