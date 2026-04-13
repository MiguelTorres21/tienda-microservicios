package com.tienda.inventory.service;

import com.tienda.inventory.dto.InventoryResponse;

import java.util.UUID;

public interface InventoryService {
    /**
     * @param productId
     * @return
     */
    InventoryResponse findByProductId(UUID productId);
}
