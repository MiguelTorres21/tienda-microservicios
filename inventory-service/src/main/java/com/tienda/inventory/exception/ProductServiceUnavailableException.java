package com.tienda.inventory.exception;

public class ProductServiceUnavailableException extends RuntimeException {
    /**
     * @param detail
     */
    public ProductServiceUnavailableException(String detail) {
        super("products-service no disponible: " + detail);
    }
}
