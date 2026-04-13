package com.tienda.products.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {

        log.warn("Producto no encontrado: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Producto no encontrado", ex.getMessage());
    }

    @ExceptionHandler(SkuAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleSkuConflict(
            SkuAlreadyExistsException ex) {

        log.warn("Conflicto de SKU: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "SKU_ALREADY_EXISTS", "Conflicto de SKU", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        List<Map<String, Object>> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
                    String detail = error.getDefaultMessage();
                    return buildErrorEntry(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "VALIDATION_ERROR",
                            "Error de validación en '" + field + "'",
                            detail
                    );
                })
                .toList();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("errors", errors));
    }

    @ExceptionHandler(InvalidSortFieldException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSort(InvalidSortFieldException ex) {
        log.warn("Campo de sort inválido: {}", ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SORT_FIELD",
                "Campo de ordenamiento inválido", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedBody(HttpMessageNotReadableException ex) {
        log.warn("Body malformado: {}", ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "MALFORMED_REQUEST",
                "Cuerpo de la petición inválido", "El cuerpo de la solicitud no es JSON válido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Tipo de parámetro inválido: {}", ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PARAMETER",
                "Parámetro inválido",
                "El valor '" + ex.getValue() + "' no es válido para el parámetro '" + ex.getName() + "'.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Error interno del servidor",
                "Ocurrió un error inesperado. Consulta los logs con el correlationId."
        );
    }

    /**
     * @param status
     * @param code
     * @param title
     * @param detail
     * @return
     */
    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String code, String title, String detail) {

        return ResponseEntity
                .status(status)
                .body(Map.of("errors", List.of(buildErrorEntry(status, code, title, detail))));
    }

    /**
     * @param status
     * @param code
     * @param title
     * @param detail
     * @return
     */
    private Map<String, Object> buildErrorEntry(
            HttpStatus status, String code, String title, String detail) {

        return Map.of(
                "status", String.valueOf(status.value()),
                "code", code,
                "title", title,
                "detail", detail,
                "meta", Map.of("correlationId", MDC.get("correlationId") != null ? MDC.get("correlationId") : "")
        );
    }
}