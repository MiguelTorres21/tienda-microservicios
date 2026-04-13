package com.tienda.products.dto;

/**
 * @param token
 * @param expiresIn
 */
public record LoginResponse(
        String token,
        long expiresIn
) {
}
