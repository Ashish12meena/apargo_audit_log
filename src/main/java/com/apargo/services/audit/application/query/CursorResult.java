package com.apargo.services.audit.application.query;

import java.util.List;

/** A page ready for the API: items, the page size used, and the opaque cursor of the next page. */
public record CursorResult<T>(List<T> items, int size, String nextCursor) {

    public CursorResult {
        items = List.copyOf(items);
    }
}
