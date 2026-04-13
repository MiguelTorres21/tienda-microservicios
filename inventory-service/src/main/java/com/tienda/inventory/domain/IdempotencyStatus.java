package com.tienda.inventory.domain;

public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
