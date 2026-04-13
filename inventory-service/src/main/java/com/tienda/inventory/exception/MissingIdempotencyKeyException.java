package com.tienda.inventory.exception;

public class MissingIdempotencyKeyException extends RuntimeException {
    public MissingIdempotencyKeyException() {
        super("El header Idempotency-Key es obligatorio para POST /purchases.");
    }
}
