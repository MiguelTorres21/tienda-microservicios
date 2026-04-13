package com.tienda.inventory.exception;

public class IdempotencyConflictException extends RuntimeException {
    /**
     * @param key
     */
    public IdempotencyConflictException(String key) {
        super("Compra ya en procesamiento para la clave de idempotencia: " + key);
    }
}
