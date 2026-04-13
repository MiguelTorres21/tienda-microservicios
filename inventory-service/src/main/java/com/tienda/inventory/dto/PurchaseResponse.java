package com.tienda.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param purchaseId
 * @param productId
 * @param quantityPurchased
 * @param remainingStock
 * @param processedAt
 */
public record PurchaseResponse(
        UUID purchaseId,
        UUID productId,
        Integer quantityPurchased,
        Integer remainingStock,
        LocalDateTime processedAt
) {
}
