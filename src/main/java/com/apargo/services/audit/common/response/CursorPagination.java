package com.apargo.services.audit.common.response;

/** Cursor paging details (API standard §5, cursor pagination). */
public record CursorPagination(int size, String nextCursor, boolean hasNext) {
}
