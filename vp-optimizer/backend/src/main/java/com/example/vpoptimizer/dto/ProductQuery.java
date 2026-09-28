package com.example.vpoptimizer.dto;

/**
 * Query parameters for the product catalogue endpoint.
 *
 * @param search     free text matched against name and SKU (case insensitive, optional)
 * @param categoryId restrict to one category (optional)
 * @param active     {@code true}/{@code false} to filter, {@code null} for both
 * @param sortBy     whitelisted sort field, defaults to {@code name}
 * @param direction  {@code asc} or {@code desc}
 */
public record ProductQuery(String search,
                           Long categoryId,
                           Boolean active,
                           String sortBy,
                           String direction,
                           int page,
                           int size) {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public static ProductQuery of(String search, Long categoryId, Boolean active,
                                  String sortBy, String direction, Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return new ProductQuery(search, categoryId, active, sortBy, direction, safePage, safeSize);
    }
}
