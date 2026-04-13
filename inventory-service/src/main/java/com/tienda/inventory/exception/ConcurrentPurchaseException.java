package com.tienda.inventory.exception;


import java.util.UUID;

public class ConcurrentPurchaseException extends RuntimeException {
    public ConcurrentPurchaseException() {
        super("No se pudo completar la compra debido a conflictos de concurrencia. Intenta de nuevo.");
    }
}
