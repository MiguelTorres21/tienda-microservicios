package com.tienda.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.inventory.client.ProductsClient;
import com.tienda.inventory.domain.IdempotencyRecord;
import com.tienda.inventory.domain.IdempotencyStatus;
import com.tienda.inventory.dto.PurchaseResponse;
import com.tienda.inventory.exception.*;
import com.tienda.inventory.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseServiceImpl.class);

    private final IdempotencyRecordRepository idempotencyRepository;
    private final PurchaseExecutor purchaseExecutor;
    private final ProductsClient productsClient;
    private final ObjectMapper objectMapper;
    private final long ttlHours;

    public PurchaseServiceImpl(
            IdempotencyRecordRepository idempotencyRepository,
            PurchaseExecutor purchaseExecutor,
            ProductsClient productsClient,
            ObjectMapper objectMapper,
            @Value("${idempotency.ttl-hours:24}") long ttlHours) {
        this.idempotencyRepository = idempotencyRepository;
        this.purchaseExecutor = purchaseExecutor;
        this.productsClient = productsClient;
        this.objectMapper = objectMapper;
        this.ttlHours = ttlHours;
    }

    @Override
    public PurchaseResponse purchase(String idempotencyKey, UUID productId, Integer quantity) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }

        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            return switch (record.getStatus()) {
                case COMPLETED -> {
                    log.info("Idempotency HIT COMPLETED [{}] key={}", cid(), idempotencyKey);
                    yield deserializePurchaseResponse(record.getResponseBody());
                }
                case FAILED -> {
                    log.warn("Idempotency HIT FAILED [{}] key={} status={}",
                            cid(), idempotencyKey, record.getResponseStatus());
                    throw new StoredPurchaseErrorException(
                            record.getResponseStatus(),
                            record.getResponseBody()
                    );
                }
                case PROCESSING -> {
                    log.warn("Idempotency HIT PROCESSING [{}] key={}", cid(), idempotencyKey);
                    throw new IdempotencyConflictException(idempotencyKey);
                }
            };
        }

        IdempotencyRecord record = createProcessingRecord(idempotencyKey);

        try {
            productsClient.getProduct(productId);

            PurchaseResponse response = purchaseExecutor.executePurchase(productId, quantity);
            markCompleted(record, response);
            logInventoryChanged(productId, quantity, response.remainingStock());
            return response;

        } catch (ProductNotFoundException ex) {
            markFailed(record, HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                    "Producto no encontrado", ex.getMessage());
            throw ex;

        } catch (ProductServiceUnavailableException ex) {
            markFailed(record, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "Servicio no disponible", ex.getMessage());
            throw ex;

        } catch (InsufficientStockException ex) {
            markFailed(record, HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Stock insuficiente", ex.getMessage());
            throw ex;

        } catch (ConcurrentPurchaseException ex) {
            markFailed(record, HttpStatus.CONFLICT, "CONCURRENT_CONFLICT",
                    "Conflicto de concurrencia", ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            markFailed(record, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Error interno", "Error inesperado. correlationId: " + cid());
            throw ex;
        }
    }

    private void markFailed(IdempotencyRecord record,
                            HttpStatus status, String code, String title, String detail) {
        try {
            Map<String, Object> errorBody = Map.of(
                    "errors", List.of(Map.of(
                            "status", String.valueOf(status.value()),
                            "code", code,
                            "title", title,
                            "detail", detail,
                            "meta", Map.of("correlationId", cid())
                    ))
            );
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseStatus(status.value());
            record.setResponseBody(objectMapper.writeValueAsString(errorBody));
            idempotencyRepository.save(record);
            log.debug("Idempotency FAILED guardado [{}] key={} status={}",
                    cid(), record.getIdempotencyKey(), status.value());
        } catch (JsonProcessingException ex) {
            log.error("markFailed: error de serializacion [{}] key={}: {}",
                    cid(), record.getIdempotencyKey(), ex.getMessage());
        } catch (Exception ex) {
            log.error("markFailed: error al persistir registro FAILED [{}] key={}: {} — {}",
                    cid(), record.getIdempotencyKey(), ex.getClass().getSimpleName(), ex.getMessage());
        }
    }


    private IdempotencyRecord createProcessingRecord(String key) {
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.PROCESSING)
                    .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                    .build();
            return idempotencyRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            throw new IdempotencyConflictException(key);
        }
    }

    private void markCompleted(IdempotencyRecord record, PurchaseResponse response) {
        try {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponseStatus(201);
            record.setResponseBody(objectMapper.writeValueAsString(response));
            idempotencyRepository.save(record);
            log.debug("Idempotency COMPLETED guardado [{}] key={}", cid(), record.getIdempotencyKey());
        } catch (JsonProcessingException ex) {
            log.error("markCompleted: error de serializacion [{}]: {}", cid(), ex.getMessage());
        } catch (Exception ex) {
            log.error("markCompleted: error al persistir [{}]: {} — {}",
                    cid(), ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private PurchaseResponse deserializePurchaseResponse(String json) {
        try {
            return objectMapper.readValue(json, PurchaseResponse.class);
        } catch (JsonProcessingException ex) {
            log.error("Error deserializando PurchaseResponse idempotente [{}]: {}",
                    cid(), ex.getMessage());
            throw new RuntimeException("Error interno al recuperar respuesta previa.");
        }
    }

    private void logInventoryChanged(UUID productId, int quantity, int remaining) {
        log.info("InventoryChanged: productId={} quantityDeducted={} remainingStock={} correlationId={}",
                productId, quantity, remaining, cid());
    }

    private String cid() {
        String id = MDC.get("correlationId");
        return id != null ? id : "";
    }
}
