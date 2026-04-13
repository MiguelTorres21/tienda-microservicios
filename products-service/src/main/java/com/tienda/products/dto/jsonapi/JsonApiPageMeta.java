package com.tienda.products.dto.jsonapi;

/**
 * @param totalItems
 * @param totalPages
 * @param currentPage
 * @param pageSize
 * @param correlationId
 */
public record JsonApiPageMeta(
        long totalItems,
        int totalPages,
        int currentPage,
        int pageSize,
        String correlationId
) {
}
