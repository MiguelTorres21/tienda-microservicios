package com.tienda.products.dto.jsonapi;

import java.util.List;

/**
 * @param data
 * @param meta
 * @param <T>
 */
public record JsonApiListResponse<T>(
        List<JsonApiData<T>> data,
        JsonApiPageMeta meta
) {
}
