package com.tienda.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param productId
 * @param available
 * @param reserved
 * @param updatedAt
 */
public record InventoryAttributes(
        UUID productId,
        Integer available,
        Integer reserved,
        LocalDateTime updatedAt
) {
}
