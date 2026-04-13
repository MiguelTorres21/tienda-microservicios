package com.tienda.inventory.integration;

import com.tienda.inventory.client.ProductsClient;
import com.tienda.inventory.domain.Inventory;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.exception.ProductServiceUnavailableException;
import com.tienda.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("InventoryIntegrationTest - Testcontainers")
class InventoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InventoryRepository inventoryRepository;

    @MockBean
    private ProductsClient productsClient;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .available(50)
                .reserved(0)
                .version(0L)
                .build());

        doNothing().when(productsClient).getProduct(productId);
    }

    @Test
    @DisplayName("GET /inventory/{id} con JWT y producto existente → 200 con datos de stock")
    void getInventory_happyPath_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{id}", productId)
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("inventory"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.attributes.productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.attributes.available").value(50))
                .andExpect(jsonPath("$.data.attributes.reserved").value(0))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @DisplayName("GET /inventory/{id} sin Authorization → 401 con error JSON:API")
    void getInventory_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{id}", productId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].status").value("401"));

        verifyNoInteractions(productsClient);
    }

    @Test
    @DisplayName("products-service retorna 404 → 404 PRODUCT_NOT_FOUND")
    void getInventory_whenProductNotFoundInProductsService_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        doThrow(new ProductNotFoundException(unknownId))
                .when(productsClient).getProduct(unknownId);

        mockMvc.perform(get("/api/v1/inventory/{id}", unknownId)
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("producto existe en products-service pero sin inventario en BD → 404")
    void getInventory_whenNoInventoryInDb_returns404() throws Exception {
        UUID noInventoryProduct = UUID.randomUUID();
        doNothing().when(productsClient).getProduct(noInventoryProduct);

        mockMvc.perform(get("/api/v1/inventory/{id}", noInventoryProduct)
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("products-service caído → 503 SERVICE_UNAVAILABLE con error claro")
    void getInventory_whenProductsServiceDown_returns503() throws Exception {
        UUID pid = UUID.randomUUID();
        doThrow(new ProductServiceUnavailableException("Connection timed out"))
                .when(productsClient).getProduct(pid);

        inventoryRepository.save(Inventory.builder()
                .productId(pid).available(10).reserved(0).version(0L).build());

        mockMvc.perform(get("/api/v1/inventory/{id}", pid)
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.errors[0].title").isNotEmpty())
                .andExpect(jsonPath("$.errors[0].meta.correlationId").exists());
    }

    @Test
    @DisplayName("respuesta exitosa cumple estructura JSON:API: data.type, data.id, data.attributes, meta")
    void getInventory_responseStructure_isJsonApiCompliant() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{id}", productId)
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.type").value("inventory"))
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.attributes.productId").isString())
                .andExpect(jsonPath("$.data.attributes.available").isNumber())
                .andExpect(jsonPath("$.data.attributes.reserved").isNumber())
                .andExpect(jsonPath("$.meta.correlationId").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }
}
