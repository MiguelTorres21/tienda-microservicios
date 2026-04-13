package com.tienda.inventory.controller;

import com.tienda.inventory.config.*;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.exception.ProductServiceUnavailableException;
import com.tienda.inventory.dto.InventoryResponse;
import com.tienda.inventory.service.InventoryService;
import com.tienda.inventory.util.JwtTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import({
    SecurityConfig.class,
    JwtAuthFilter.class,
    JwtService.class,
    JsonApiAuthenticationEntryPoint.class,
    JsonApiAccessDeniedHandler.class,
    CorrelationIdFilter.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "security.jwt.secret=cambiar-este-secreto-en-produccion-min-32-chars",
    "products-service.base-url=http://localhost:9999",
    "products-service.internal-api-key=test-api-key"
})
@DisplayName("InventoryController - WebMvcTest")
class InventoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private InventoryService inventoryService;

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Test
    @DisplayName("GET /inventory/{productId} con JWT válido → 200 con datos de inventario")
    void getInventory_withValidJwt_returns200() throws Exception {
        InventoryResponse response = new InventoryResponse(
                UUID.randomUUID(), PRODUCT_ID, 50, 0, LocalDateTime.now());
        when(inventoryService.findByProductId(PRODUCT_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/{id}", PRODUCT_ID)
                        .header("Authorization", JwtTestHelper.bearerHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("inventory"))
                .andExpect(jsonPath("$.data.attributes.available").value(50))
                .andExpect(jsonPath("$.data.attributes.reserved").value(0))
                .andExpect(jsonPath("$.meta").exists());
    }


    @Test
    @DisplayName("GET /inventory/{productId} sin Authorization → 401 con body JSON:API")
    void getInventory_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{id}", PRODUCT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].status").value("401"))
                .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));

        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("GET /inventory/{productId} → producto no existe → 404 PRODUCT_NOT_FOUND")
    void getInventory_whenProductNotFound_returns404() throws Exception {
        when(inventoryService.findByProductId(any()))
                .thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(get("/api/v1/inventory/{id}", PRODUCT_ID)
                        .header("Authorization", JwtTestHelper.bearerHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /inventory/{productId} → products-service caído → 503 SERVICE_UNAVAILABLE")
    void getInventory_whenProductsServiceDown_returns503() throws Exception {
        when(inventoryService.findByProductId(any()))
                .thenThrow(new ProductServiceUnavailableException("timeout"));

        mockMvc.perform(get("/api/v1/inventory/{id}", PRODUCT_ID)
                        .header("Authorization", JwtTestHelper.bearerHeader()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("GET /inventory/not-a-uuid → 422 INVALID_PARAMETER")
    void getInventory_withInvalidUuid_returns422() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/not-a-valid-uuid")
                        .header("Authorization", JwtTestHelper.bearerHeader()))
                .andExpect(status().isUnprocessableEntity());
    }
}
