package com.tienda.inventory.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    /**
     * @param productId
     * @param requested
     * @param available
     */
    public InsufficientStockException(UUID productId, int requested, int available) {
        super(String.format(
                "Stock insuficiente para el producto %s. Solicitado: %d, Disponible: %d",
                productId, requested, available
        ));
    }
}
