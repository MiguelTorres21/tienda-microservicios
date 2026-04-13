package com.tienda.products.dto;

import com.tienda.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @param sku
 * @param name
 * @param price
 * @param status
 * @param createdAt
 * @param updatedAt
 */
public record ProductAttributes(
        String sku,
        String name,
        BigDecimal price,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}