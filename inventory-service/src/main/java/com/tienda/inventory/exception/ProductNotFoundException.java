package com.tienda.inventory.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    /**
     * @param productId
     */
    public ProductNotFoundException(UUID productId) {
        super("Producto no encontrado en products-service: " + productId);
    }
}
