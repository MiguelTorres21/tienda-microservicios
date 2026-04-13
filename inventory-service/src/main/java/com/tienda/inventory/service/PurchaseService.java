package com.tienda.inventory.service;

import com.tienda.inventory.dto.PurchaseResponse;
import java.util.UUID;

public interface PurchaseService {
    /**
     * @param idempotencyKey
     * @param productId
     * @param quantity
     * @return
     */
    PurchaseResponse purchase(String idempotencyKey, UUID productId, Integer quantity);
}
