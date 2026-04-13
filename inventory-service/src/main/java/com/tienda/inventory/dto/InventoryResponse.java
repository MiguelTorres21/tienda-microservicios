package com.tienda.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param id
 * @param productId
 * @param available
 * @param reserved
 * @param updatedAt
 */
public record InventoryResponse(
        UUID id,
        UUID productId,
        Integer available,
        Integer reserved,
        LocalDateTime updatedAt
) {
}
