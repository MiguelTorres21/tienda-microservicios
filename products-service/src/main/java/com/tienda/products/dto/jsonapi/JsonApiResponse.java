package com.tienda.products.dto.jsonapi;

/**
 * @param data
 * @param meta
 * @param <T>
 */
public record JsonApiResponse<T>(
        JsonApiData<T> data,
        JsonApiMeta meta
) {
}
