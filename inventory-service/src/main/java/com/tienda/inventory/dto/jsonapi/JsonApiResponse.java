package com.tienda.inventory.dto.jsonapi;

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
