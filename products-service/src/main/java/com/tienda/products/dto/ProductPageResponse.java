package com.tienda.products.dto;

import java.util.List;

/**
 * @param data
 * @param totalItems
 * @param totalPages
 * @param currentPage
 * @param pageSize
 */
public record ProductPageResponse(
        List<ProductResponse> data,
        long totalItems,
        int totalPages,
        int currentPage,
        int pageSize
) {
}
