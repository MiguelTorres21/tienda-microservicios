package com.tienda.inventory.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper
     */
    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(
            ProductNotFoundException ex) {
        log.warn("404 [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                "Producto no encontrado", ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(
            InsufficientStockException ex) {
        log.warn("409 [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                "Stock insuficiente", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotencyConflict(
            IdempotencyConflictException ex) {
        log.warn("409 [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.CONFLICT, "PURCHASE_IN_PROGRESS",
                "Compra en procesamiento", ex.getMessage());
    }

    @ExceptionHandler(ConcurrentPurchaseException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrentPurchase(
            ConcurrentPurchaseException ex) {
        log.warn("409 [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.CONFLICT, "CONCURRENT_CONFLICT",
                "Conflicto de concurrencia", ex.getMessage());
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(
            ProductServiceUnavailableException ex) {
        log.error("503 [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "Servicio no disponible",
                "products-service no responde. Intenta de nuevo en unos segundos.");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCallNotPermitted(
            CallNotPermittedException ex) {
        log.error("503 CB abierto [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "CIRCUIT_BREAKER_OPEN",
                "Servicio temporalmente no disponible",
                "El circuit breaker esta abierto. products-service no disponible en este momento.");
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientRequest(
            WebClientRequestException ex) {
        log.error("503 WebClientRequestException [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "Error de conexion con dependencia externa",
                "No se pudo conectar con products-service: " + ex.getMessage());
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<Map<String, Object>> handleMissingIdempotencyKey(
            MissingIdempotencyKeyException ex) {
        log.warn("422 [{}] {}", cid(), ex.getMessage());
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_IDEMPOTENCY_KEY",
                "Header obligatorio ausente", ex.getMessage());
    }

    @ExceptionHandler(StoredPurchaseErrorException.class)
    public ResponseEntity<Object> handleStoredError(StoredPurchaseErrorException ex) {
        int status = ex.getResponseStatus();
        String responseBody = ex.getResponseBody();

        log.warn("{} [{}] Replay FAILED — reproduciendo respuesta almacenada", status, cid());

        if (responseBody == null || responseBody.isBlank()) {
            log.error("Registro FAILED sin responseBody [{}] — devolviendo error generico", cid());
            return ResponseEntity.status(status).body(Map.of(
                    "errors", List.of(entry(
                            HttpStatus.valueOf(status),
                            "PURCHASE_PREVIOUSLY_FAILED",
                            "Compra previa fallida",
                            "Esta compra fallo anteriormente. Usa una nueva Idempotency-Key para reintentar."
                    ))
            ));
        }

        try {
            Object body = objectMapper.readValue(responseBody, Object.class);
            return ResponseEntity.status(status).body(body);
        } catch (JsonProcessingException e) {
            log.error("Error deserializando responseBody almacenado [{}]: {}", cid(), e.getMessage());
            return ResponseEntity.status(status).body(Map.of(
                    "errors", List.of(entry(
                            HttpStatus.valueOf(status),
                            "PURCHASE_PREVIOUSLY_FAILED",
                            "Compra previa fallida",
                            "No se pudo recuperar el detalle del error original."
                    ))
            ));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        log.warn("422 [{}] {} errores de validacion", cid(),
                ex.getBindingResult().getErrorCount());
        List<Map<String, Object>> errors = ex.getBindingResult().getAllErrors().stream()
                .map(e -> {
                    String field = e instanceof FieldError fe ? fe.getField() : e.getObjectName();
                    return entry(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                            "Error de validacion en '" + field + "'", e.getDefaultMessage());
                })
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("errors", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(
            HttpMessageNotReadableException ex) {
        log.warn("422 [{}] body malformado", cid());
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "MALFORMED_REQUEST",
                "Cuerpo invalido",
                "El body no es JSON valido o contiene tipos incorrectos.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String detail = String.format("El parametro '%s' recibio '%s' con tipo incorrecto.",
                ex.getName(), ex.getValue());
        log.warn("422 [{}] {}", cid(), detail);
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PARAMETER",
                "Parametro invalido", detail);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex) {
        String detail = "Header requerido ausente: " + ex.getHeaderName();
        log.warn("400 [{}] {}", cid(), detail);
        return error(HttpStatus.BAD_REQUEST, "MISSING_HEADER", "Header ausente", detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("500 [{}] error no clasificado: {} — {}",
                cid(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Error interno del servidor",
                "Error inesperado. correlationId: " + cid());
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String code, String title, String detail) {
        return ResponseEntity.status(status)
                .body(Map.of("errors", List.of(entry(status, code, title, detail))));
    }

    private Map<String, Object> entry(
            HttpStatus status, String code, String title, String detail) {
        return Map.of(
                "status", String.valueOf(status.value()),
                "code", code,
                "title", title,
                "detail", detail,
                "meta", Map.of("correlationId", cid())
        );
    }

    private String cid() {
        String id = MDC.get("correlationId");
        return id != null ? id : "";
    }
}
