package com.tienda.products.dto;

import com.tienda.products.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param sku
 * @param name
 * @param price
 * @param status
 */
public record ProductRequest(

        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 100, message = "El SKU no puede superar 100 caracteres")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        String name,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 enteros y 2 decimales")
        BigDecimal price,

        ProductStatus status
) {
    public ProductRequest {
        if (status == null) {
            status = ProductStatus.ACTIVE;
        }
    }
}
