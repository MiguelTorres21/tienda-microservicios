package com.tienda.inventory.integration;

import com.tienda.inventory.client.ProductsClient;
import com.tienda.inventory.domain.Inventory;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.exception.ProductServiceUnavailableException;
import com.tienda.inventory.repository.IdempotencyRecordRepository;
import com.tienda.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PurchaseIntegrationTest - Testcontainers")
class PurchaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private IdempotencyRecordRepository idempotencyRepository;

    @MockBean
    private ProductsClient productsClient;

    private UUID productId;

    @BeforeEach
    void setUp() {
        idempotencyRepository.deleteAll();
        productId = UUID.randomUUID();
        Inventory inv = Inventory.builder()
                .productId(productId)
                .available(50)
                .reserved(0)
                .version(0L)
                .build();
        inventoryRepository.save(inv);

        doNothing().when(productsClient).getProduct(productId);
    }

    @Test
    @DisplayName("compra válida → 201, stock reducido en BD")
    void purchase_happyPath_returns201_andReducesStock() throws Exception {
        String idemKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("purchases"))
                .andExpect(jsonPath("$.data.attributes.quantityPurchased").value(5))
                .andExpect(jsonPath("$.data.attributes.remainingStock").value(45));

        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(updated.getAvailable()).isEqualTo(45);
    }

    @Test
    @DisplayName("sin Authorization header → 401, stock sin modificar")
    void purchase_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 5)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].status").value("401"));

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailable())
                .isEqualTo(50);
    }

    @Test
    @DisplayName("sin Idempotency-Key header → 422 MISSING_IDEMPOTENCY_KEY")
    void purchase_withoutIdempotencyKey_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 5)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    @DisplayName("quantity < 1 → 422 VALIDATION_ERROR")
    void purchase_withQuantityZero_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": \"" + productId + "\", \"quantity\": 0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("producto no existe en products-service → 404 PRODUCT_NOT_FOUND")
    void purchase_whenProductNotFound_returns404() throws Exception {
        UUID unknownProduct = UUID.randomUUID();
        doThrow(new ProductNotFoundException(unknownProduct))
                .when(productsClient).getProduct(unknownProduct);

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(unknownProduct, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("cantidad > stock disponible → 409 INSUFFICIENT_STOCK, stock intacto")
    void purchase_whenInsufficientStock_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 999)))  // más de los 50 disponibles
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].code").value("INSUFFICIENT_STOCK"));

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailable())
                .isEqualTo(50);
    }

    @Test
    @DisplayName("products-service no disponible → 503 SERVICE_UNAVAILABLE")
    void purchase_whenProductsServiceDown_returns503() throws Exception {
        UUID pid = UUID.randomUUID();
        doThrow(new ProductServiceUnavailableException("Connection refused"))
                .when(productsClient).getProduct(pid);

        inventoryRepository.save(Inventory.builder()
                .productId(pid).available(10).reserved(0).version(0L).build());

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(pid, 1)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("mismo Idempotency-Key repetido → 201 dos veces, stock descontado solo una vez")
    void purchase_sameKeyTwice_idempotent_stockDeductedOnce() throws Exception {
        String idemKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attributes.remainingStock").value(45));

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attributes.remainingStock").value(45));

        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(updated.getAvailable()).isEqualTo(45);

        verify(productsClient, times(1)).getProduct(productId);
    }

    @Test
    @DisplayName("mismo key después de error → reproduce el error original (409)")
    void purchase_sameKeyAfterFailure_replaysOriginalError() throws Exception {
        String idemKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 999)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", bearerHeader())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(productId, 999)))
                .andExpect(status().isConflict());

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailable())
                .isEqualTo(50);
    }

    @Test
    @DisplayName("10 compras concurrentes de 1 unidad con stock=5 → stock=0, nunca negativo")
    void purchase_concurrentRequests_neverNegativeStock() throws Exception {
        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        inv.setAvailable(5);
        inventoryRepository.save(inv);

        int totalThreads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            final String idemKey = UUID.randomUUID().toString();
            tasks.add(() -> {
                try {
                    var result = mockMvc.perform(post("/api/v1/purchases")
                                    .header("Authorization", bearerHeader())
                                    .header("Idempotency-Key", idemKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body(productId, 1)))
                            .andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    return 500;
                }
            });
        }

        List<Future<Integer>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        long successCount = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return 500;
                    }
                })
                .filter(s -> s == 201)
                .count();
        long conflictCount = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return 500;
                    }
                })
                .filter(s -> s == 409)
                .count();

        Inventory finalInv = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(finalInv.getAvailable())
                .as("Stock nunca debe ser negativo")
                .isGreaterThanOrEqualTo(0);

        assertThat(successCount)
                .as("Compras exitosas <= stock inicial (5)")
                .isLessThanOrEqualTo(5);

        assertThat(finalInv.getAvailable())
                .as("Stock final = 5 - compras exitosas")
                .isEqualTo((int) (5 - successCount));

        assertThat(successCount + conflictCount)
                .as("Todas las requests terminaron con 201 o 409")
                .isEqualTo(totalThreads);
    }

    private String body(UUID pid, int quantity) {
        return String.format("{\"productId\": \"%s\", \"quantity\": %d}", pid, quantity);
    }
}
