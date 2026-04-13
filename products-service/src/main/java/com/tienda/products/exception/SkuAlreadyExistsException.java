package com.tienda.products.exception;


public class SkuAlreadyExistsException extends RuntimeException {

    /**
     * @param sku
     */
    public SkuAlreadyExistsException(String sku) {
        super("Ya existe un producto con el SKU: " + sku);
    }
}
