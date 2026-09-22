package com.suresh.sms.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * A stable, explicit JSON shape for paged results.
 *
 * <p>Serializing Spring's Page object directly is discouraged (its JSON shape
 * is not guaranteed to stay the same between versions), so the API exposes
 * this small record instead.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
