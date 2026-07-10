package com.apargo.service.auditlog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PagedResponse<T> {
    private List<T> items;
    private long totalItems;
    private int totalPages;
    private int page;
    private int limit;
}
