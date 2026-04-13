package com.tienda.products.exception;

public class InvalidSortFieldException extends RuntimeException {

    /**
     * @param field
     */
    public InvalidSortFieldException(String field) {
        super("Campo de ordenamiento no permitido: '" + field +
                "'. Valores válidos: price, createdAt");
    }
}
