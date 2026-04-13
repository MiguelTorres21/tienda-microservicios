package com.tienda.products.dto;

import com.tienda.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param id
 * @param sku
 * @param name
 * @param price
 * @param status
 * @param createdAt
 * @param updatedAt
 */
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


