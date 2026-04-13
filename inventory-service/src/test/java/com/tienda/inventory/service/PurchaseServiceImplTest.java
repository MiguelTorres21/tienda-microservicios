package com.tienda.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tienda.inventory.client.ProductsClient;
import com.tienda.inventory.domain.IdempotencyRecord;
import com.tienda.inventory.domain.IdempotencyStatus;
import com.tienda.inventory.dto.PurchaseResponse;
import com.tienda.inventory.exception.*;
import com.tienda.inventory.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseServiceImpl - Unit Tests")
class PurchaseServiceImplTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRepository;
    @Mock
    private PurchaseExecutor purchaseExecutor;
    @Mock
    private ProductsClient productsClient;

    private ObjectMapper objectMapper;
    private PurchaseServiceImpl service;

    private static final UUID PRODUCT_ID = UUID.fromString("a1b2c3d4-0001-0001-0001-000000000001");
    private static final int QUANTITY = 3;
    private static final String IDEMPOTENCY_KEY = "idem-key-" + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new PurchaseServiceImpl(
                idempotencyRepository, purchaseExecutor, productsClient, objectMapper, 24L);
    }

    @Test
    @DisplayName("null key → MissingIdempotencyKeyException, sin llamar a repositorio ni cliente")
    void purchase_whenKeyNull_throwsMissingIdempotencyKey() {
        assertThatThrownBy(() -> service.purchase(null, PRODUCT_ID, QUANTITY))
                .isInstanceOf(MissingIdempotencyKeyException.class);

        verifyNoInteractions(idempotencyRepository, productsClient, purchaseExecutor);
    }

    @Test
    @DisplayName("key en blanco → MissingIdempotencyKeyException")
    void purchase_whenKeyBlank_throwsMissingIdempotencyKey() {
        assertThatThrownBy(() -> service.purchase("   ", PRODUCT_ID, QUANTITY))
                .isInstanceOf(MissingIdempotencyKeyException.class);

        verifyNoInteractions(idempotencyRepository, productsClient, purchaseExecutor);
    }

    @Test
    @DisplayName("key nueva + producto válido + stock suficiente → compra exitosa 201")
    void purchase_whenNewKey_happyPath_returnsResponse() {
        PurchaseResponse expected = buildPurchaseResponse(10);
        stubNewKeyFlow(expected);

        PurchaseResponse result = service.purchase(IDEMPOTENCY_KEY, PRODUCT_ID, QUANTITY);

        assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.quantityPurchased()).isEqualTo(QUANTITY);
        assertThat(result.remainingStock()).isEqualTo(10);

        verify(productsClient).getProduct(PRODUCT_ID);
        verify(purchaseExecutor).executePurchase(PRODUCT_ID, QUANTITY);
        verify(idempotencyRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("key COMPLETED → devuelve respuesta almacenada sin volver a ejecutar")
    void purchase_whenExistingKeyCompleted_replaysStoredResponse() throws Exception {
        // Arrange
        PurchaseResponse stored = buildPurchaseResponse(10);
        String json = objectMapper.writeValueAsString(stored);

        IdempotencyRecord record = buildRecord(IdempotencyStatus.COMPLETED);
        record.setResponseStatus(201);
        record.setResponseBody(json);

        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(record));

        PurchaseResponse result = service.purchase(IDEMPOTENCY_KEY, PRODUCT_ID, QUANTITY);

        assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.quantityPurchased()).isEqualTo(QUANTITY);

        verifyNoInteractions(productsClient, purchaseExecutor);
        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("key FAILED → lanza StoredPurchaseErrorException con el status original")
    void purchase_whenExistingKeyFailed_throwsStoredPurchaseError() {
        IdempotencyRecord record = buildRecord(IdempotencyStatus.FAILED);
        record.setResponseStatus(409);
        record.setResponseBody("{\"errors\":[{\"code\":\"INSUFFICIENT_STOCK\"}]}");

        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.purchase(IDEMPOTENCY_KEY, PRODUCT_ID, QUANTITY))
                .isInstanceOf(StoredPurchaseErrorException.class)
                .satisfies(ex -> {
                    StoredPurchaseErrorException spee = (StoredPurchaseErrorException) ex;
                    assertThat(spee.getResponseStatus()).isEqualTo(409);
                });

        verifyNoInteractions(productsClient, purchaseExecutor);
    }

    @Test
    @DisplayName("key PROCESSING → IdempotencyConflictException (compra en curso)")
    void purchase_whenExistingKeyProcessing_throwsIdempotencyConflict() {
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(buildRecord(IdempotencyStatus.PROCESSING)));

        assertThatThrownBy(() -> service.purchase(IDEMPOTENCY_KEY, PRODUCT_ID, QUANTITY))
                .isInstanceOf(IdempotencyConflictException.class);

        verifyNoInteractions(productsClient, purchaseExecutor);
    }

    @Test
    @DisplayName("producto no encontrado → registro FAILED con status 404 + relanza excepción")
    void purchase_whenProductNotFound_marksFailedAnd404_rethrows() {
        List<IdempotencyRecord> savedRecords = captureAllSaves();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        doThrow(new ProductNotFoundException(PRODUCT_ID))
                .when(productsClient).getProduct(PRODUCT_ID);

        assertThatThrownBy(() -> service.purchase(IDEMPOTENCY_KEY, PRODUCT_ID, QUANTITY))
                .isInstanceOf(ProductNotFoundException.class);

        assertThat(savedRecords)
                .anySatisfy(r -> {
                    assertThat(r.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
                    assertThat(r.getResponseStatus()).isEqualTo(404);
                });
    }

    @Test
    @DisplayName("products-service caído → registro FAILED con status 503 + relanza excepción")
    void purchase_whenServiceUnavailable_marksFailedAnd503_rethrows() {
        List<IdempotencyRecord> savedRecords = captureAllSaves();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        doThrow(new ProductServiceUnavailableException("Connection refused"))
                .when(productsClient).getProduct(PRODUCT_ID);

        assertThatThrownBy(() -> service.purchase(IDEMPOTENCY_KEY, PRODUCT_ID, QUANTITY))
                .isInstanceOf(ProductServiceUnavailableException.class);

        assertThat(savedRecords)
                .anySatisfy(r -> {
                    assertThat(r.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
                    assertThat(r.getResponseStatus()).isEqualTo(503);
                });
    }

    @Test
    @DisplayName("stock insuficiente → registro FAILED con status 409 + relanza excepción")
    void purchase_whenInsufficientStock_marksFailedAnd409_rethrows() {
        List<IdempotencyRecord> savedRecords = captureAllSaves();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        doNothing().when(productsClient).getProduct(PRODUCT_ID);
        when(purchaseExecutor.executePurchase(PRODUCT_ID, QUANTITY))
                .thenThrow(new InsufficientStockException(PRODUCT_ID, QUANTITY, 1));

        assertThatThrownBy(() -> service.purchase(IDEMPOTENCY_KEY, PRODUCT_ID, QUANTITY))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(savedRecords)
                .anySatisfy(r -> {
                    assertThat(r.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
                    assertThat(r.getResponseStatus()).isEqualTo(409);
                });
    }

    /**
     * @param response
     */
    private void stubNewKeyFlow(PurchaseResponse response) {
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(idempotencyRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(productsClient).getProduct(PRODUCT_ID);
        when(purchaseExecutor.executePurchase(PRODUCT_ID, QUANTITY)).thenReturn(response);
    }

    private List<IdempotencyRecord> captureAllSaves() {
        List<IdempotencyRecord> captured = new ArrayList<>();
        when(idempotencyRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(inv -> {
                    IdempotencyRecord original = inv.getArgument(0);
                    IdempotencyRecord copy = IdempotencyRecord.builder()
                            .idempotencyKey(original.getIdempotencyKey())
                            .status(original.getStatus())
                            .responseStatus(original.getResponseStatus())
                            .responseBody(original.getResponseBody())
                            .expiresAt(original.getExpiresAt())
                            .build();
                    captured.add(copy);
                    return original;
                });
        return captured;
    }

    private PurchaseResponse buildPurchaseResponse(int remaining) {
        return new PurchaseResponse(
                UUID.randomUUID(), PRODUCT_ID, QUANTITY, remaining, LocalDateTime.now());
    }

    private IdempotencyRecord buildRecord(IdempotencyStatus status) {
        return IdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(IDEMPOTENCY_KEY)
                .status(status)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }
}
