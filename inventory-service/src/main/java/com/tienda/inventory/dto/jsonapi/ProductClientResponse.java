package com.tienda.inventory.dto.jsonapi;

import com.tienda.inventory.dto.ProductDto;

/**
 * @param data
 * @param meta
 */
public record ProductClientResponse(
        JsonApiData<ProductDto> data,
        JsonApiMeta meta
) {
}
