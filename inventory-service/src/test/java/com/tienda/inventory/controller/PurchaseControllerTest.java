package com.tienda.inventory.controller;

import com.tienda.inventory.config.*;
import com.tienda.inventory.dto.PurchaseResponse;
import com.tienda.inventory.exception.*;
import com.tienda.inventory.service.PurchaseService;
import com.tienda.inventory.util.JwtTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseController.class)
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
@DisplayName("PurchaseController - WebMvcTest")
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PurchaseService purchaseService;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String IDEM_KEY = "idem-" + UUID.randomUUID();

    @Test
    @DisplayName("POST /purchases con JWT y key válidos → 201 con datos de la compra")
    void purchase_validJwtAndKey_returns201() throws Exception {
        PurchaseResponse response = new PurchaseResponse(
                UUID.randomUUID(), PRODUCT_ID, 3, 47, LocalDateTime.now());
        when(purchaseService.purchase(eq(IDEM_KEY), eq(PRODUCT_ID), eq(3)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PRODUCT_ID, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("purchases"))
                .andExpect(jsonPath("$.data.attributes.quantityPurchased").value(3))
                .andExpect(jsonPath("$.data.attributes.remainingStock").value(47))
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @DisplayName("POST /purchases sin Authorization header → 401")
    void purchase_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PRODUCT_ID, 3)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].status").value("401"));
    }

    @Test
    @DisplayName("POST /purchases con token inválido → 401")
    void purchase_withInvalidJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt")
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PRODUCT_ID, 3)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /purchases sin Idempotency-Key header → 422 MISSING_IDEMPOTENCY_KEY")
    void purchase_withoutIdempotencyKey_returns422() throws Exception {
        when(purchaseService.purchase(isNull(), any(), anyInt()))
                .thenThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PRODUCT_ID, 3)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    @DisplayName("POST /purchases con productId null → 422 VALIDATION_ERROR")
    void purchase_withNullProductId_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": null, \"quantity\": 3}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /purchases con quantity = 0 → 422 VALIDATION_ERROR")
    void purchase_withQuantityZero_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": \"" + PRODUCT_ID + "\", \"quantity\": 0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /purchases con body malformado → 422 MALFORMED_REQUEST")
    void purchase_withMalformedJson_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json }"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /purchases → producto no existe → 404 PRODUCT_NOT_FOUND")
    void purchase_whenProductNotFound_returns404() throws Exception {
        when(purchaseService.purchase(anyString(), any(), anyInt()))
                .thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PRODUCT_ID, 3)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /purchases → stock insuficiente → 409 INSUFFICIENT_STOCK")
    void purchase_whenInsufficientStock_returns409() throws Exception {
        when(purchaseService.purchase(anyString(), any(), anyInt()))
                .thenThrow(new InsufficientStockException(PRODUCT_ID, 10, 2));

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PRODUCT_ID, 10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("POST /purchases → products-service caído → 503 SERVICE_UNAVAILABLE")
    void purchase_whenProductsServiceDown_returns503() throws Exception {
        when(purchaseService.purchase(anyString(), any(), anyInt()))
                .thenThrow(new ProductServiceUnavailableException("Connection refused"));

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", JwtTestHelper.bearerHeader())
                        .header("Idempotency-Key", IDEM_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PRODUCT_ID, 3)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"));
    }

    private String body(UUID productId, int quantity) {
        return String.format("{\"productId\": \"%s\", \"quantity\": %d}", productId, quantity);
    }
}
