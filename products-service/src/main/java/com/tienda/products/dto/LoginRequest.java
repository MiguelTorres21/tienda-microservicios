package com.tienda.products.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param username
 * @param password
 */
public record LoginRequest(
        @NotBlank(message = "El username es obligatorio")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
