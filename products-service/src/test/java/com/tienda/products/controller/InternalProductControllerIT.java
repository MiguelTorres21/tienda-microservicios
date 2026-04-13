package com.tienda.products.controller;

import com.tienda.products.domain.Product;
import com.tienda.products.domain.ProductStatus;
import com.tienda.products.integration.BaseIntegrationTest;
import com.tienda.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("InternalProductController — /internal/v1/products")
class InternalProductControllerIT extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ProductRepository repository;

    @Value("${internal.api-key:dev-internal-key-cambiar-en-produccion}")
    private String validApiKey;

    private static final String INTERNAL_URL = "/internal/v1/products";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("API Key válida + producto existente devuelve 200 con JSON:API")
    void getById_returns200_withValidApiKeyAndExistingProduct() throws Exception {
        Product saved = repository.save(Product.builder()
                .sku("SKU-INT")
                .name("Producto Interno")
                .price(new BigDecimal("100.00"))
                .status(ProductStatus.ACTIVE)
                .build());

        mockMvc.perform(get(INTERNAL_URL + "/" + saved.getId())
                        .header("X-API-Key", validApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("products"))
                .andExpect(jsonPath("$.data.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.attributes.sku").value("SKU-INT"));
    }

    @Test
    @DisplayName("API Key válida + producto inexistente devuelve 404 PRODUCT_NOT_FOUND")
    void getById_returns404_withValidApiKeyAndMissingProduct() throws Exception {
        String unknownId = "00000000-0000-0000-0000-000000000099";

        mockMvc.perform(get(INTERNAL_URL + "/" + unknownId)
                        .header("X-API-Key", validApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("sin X-API-Key devuelve 401 JSON:API con INVALID_API_KEY")
    void getById_returns401_withoutApiKey() throws Exception {
        mockMvc.perform(get(INTERNAL_URL + "/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_API_KEY"));
    }

    @Test
    @DisplayName("API Key incorrecta devuelve 401 JSON:API con INVALID_API_KEY")
    void getById_returns401_withWrongApiKey() throws Exception {
        mockMvc.perform(get(INTERNAL_URL + "/00000000-0000-0000-0000-000000000001")
                        .header("X-API-Key", "clave-incorrecta"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_API_KEY"));
    }

    @Test
    @DisplayName("JWT de usuario no es válido para rutas internas (requiere API Key)")
    void getById_returns401_withJwtInsteadOfApiKey() throws Exception {
        mockMvc.perform(get(INTERNAL_URL + "/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer cualquier.token.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rutas /api/v1 no aceptan API Key (son solo para JWT)")
    void publicRoutes_areNotAccessibleWithApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("X-API-Key", validApiKey))
                .andExpect(status().isUnauthorized());
    }
}
