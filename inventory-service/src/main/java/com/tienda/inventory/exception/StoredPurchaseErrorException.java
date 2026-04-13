package com.tienda.inventory.exception;

import lombok.Getter;

@Getter
public class StoredPurchaseErrorException extends RuntimeException {

    private final int responseStatus;
    private final String responseBody;

    public StoredPurchaseErrorException(int responseStatus, String responseBody) {
        super("Reproduciendo error previamente almacenado para request idempotente fallido.");
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

}