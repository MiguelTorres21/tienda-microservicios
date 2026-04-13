package com.tienda.products.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @param type
 * @param id
 * @param attributes
 * @param <T>
 */
public record JsonApiData<T>(

        String type,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String id,

        T attributes
) {
}
