package com.apargo.services.audit.common.response;

import java.util.List;

/** {@code data} of a cursor-paged list: {@code { items, pagination }}. */
public record CursorPage<T>(List<T> items, CursorPagination pagination) {

    public static <T> CursorPage<T> of(List<T> items, int size, String nextCursor) {
        return new CursorPage<>(List.copyOf(items), new CursorPagination(size, nextCursor, nextCursor != null));
    }
}
