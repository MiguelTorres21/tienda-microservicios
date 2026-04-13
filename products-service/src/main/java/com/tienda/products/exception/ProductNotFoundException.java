package com.tienda.products.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    /**
     * @param id
     */
    public ProductNotFoundException(UUID id) {
        super("Producto no encontrado con id: " + id);
    }

    /**
     * @param sku
     */
    public ProductNotFoundException(String sku) {
        super("Producto no encontrado con sku: " + sku);
    }
}
