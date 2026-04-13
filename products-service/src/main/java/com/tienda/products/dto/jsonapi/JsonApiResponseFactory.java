package com.tienda.products.dto.jsonapi;

import com.tienda.products.dto.LoginResponse;
import com.tienda.products.dto.ProductAttributes;
import com.tienda.products.dto.ProductPageResponse;
import com.tienda.products.dto.ProductResponse;
import org.slf4j.MDC;

import java.util.List;

public final class JsonApiResponseFactory {

    private static final String TYPE_PRODUCTS = "products";
    private static final String TYPE_AUTH_TOKENS = "auth-tokens";

    private JsonApiResponseFactory() {
    }

    /**
     * @param product
     * @return
     */
    public static JsonApiResponse<ProductAttributes> fromProduct(ProductResponse product) {
        ProductAttributes attributes = new ProductAttributes(
                product.sku(),
                product.name(),
                product.price(),
                product.status(),
                product.createdAt(),
                product.updatedAt()
        );

        return new JsonApiResponse<>(
                new JsonApiData<>(TYPE_PRODUCTS, product.id().toString(), attributes),
                new JsonApiMeta(correlationId())
        );
    }

    /**
     * @param page
     * @return
     */
    public static JsonApiListResponse<ProductAttributes> fromProductPage(ProductPageResponse page) {
        List<JsonApiData<ProductAttributes>> data = page.data().stream()
                .map(p -> new JsonApiData<>(
                        TYPE_PRODUCTS,
                        p.id().toString(),
                        new ProductAttributes(
                                p.sku(),
                                p.name(),
                                p.price(),
                                p.status(),
                                p.createdAt(),
                                p.updatedAt()
                        )
                ))
                .toList();

        return new JsonApiListResponse<>(
                data,
                new JsonApiPageMeta(
                        page.totalItems(),
                        page.totalPages(),
                        page.currentPage(),
                        page.pageSize(),
                        correlationId()
                )
        );
    }

    /**
     * @param loginResponse
     * @return
     */
    public static JsonApiResponse<LoginResponse> fromLogin(LoginResponse loginResponse) {
        return new JsonApiResponse<>(
                new JsonApiData<>(TYPE_AUTH_TOKENS, null, loginResponse),
                new JsonApiMeta(correlationId())
        );
    }

    /**
     * @param type
     * @param id
     * @param attributes
     * @param <T>
     * @return
     */
    public static <T> JsonApiResponse<T> of(String type, String id, T attributes) {
        return new JsonApiResponse<>(
                new JsonApiData<>(type, id, attributes),
                new JsonApiMeta(correlationId())
        );
    }

    private static String correlationId() {
        String id = MDC.get("correlationId");
        return id != null ? id : "";
    }
}