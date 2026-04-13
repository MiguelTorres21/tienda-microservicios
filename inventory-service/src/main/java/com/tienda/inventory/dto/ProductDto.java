package com.tienda.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param id
 * @param sku
 * @param name
 * @param price
 * @param status
 */
public record ProductDto(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        String status
) {
}
