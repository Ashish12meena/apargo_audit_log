package com.apargo.services.audit.application.query;

import java.util.List;

/** One page as read from the store, and whether more items follow. */
public record Slice<T>(List<T> items, boolean hasMore) {

    public Slice {
        items = List.copyOf(items);
    }
}
