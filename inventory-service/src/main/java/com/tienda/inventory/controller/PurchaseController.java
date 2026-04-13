package com.tienda.inventory.controller;

import com.tienda.inventory.dto.PurchaseRequest;
import com.tienda.inventory.dto.PurchaseResponse;
import com.tienda.inventory.dto.jsonapi.JsonApiResponse;
import com.tienda.inventory.dto.jsonapi.JsonApiResponseFactory;
import com.tienda.inventory.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchases")
@Tag(name = "Compras", description = "Ejecucion de compras con idempotencia")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseController.class);

    private final PurchaseService service;

    /**
     * @param service
     */
    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    /**
     * @param idempotencyKey
     * @param request
     * @return
     */
    @Operation(
            summary = "Ejecutar compra",
            description = """
                    Descuenta stock del inventario de forma idempotente.
                    
                    **Header requerido:** `Idempotency-Key` (UUID generado por el cliente).
                    Requests repetidos con la misma key devuelven la respuesta original.
                    
                    **Flujo:**
                    1. Valida Idempotency-Key
                    2. Verifica existencia del producto en products-service
                    3. Valida stock suficiente
                    4. Descuenta stock con proteccion de concurrencia
                    5. Emite evento InventoryChanged
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra realizada exitosamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente / compra en proceso / conflicto de concurrencia",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "422", description = "Validacion / Idempotency-Key ausente",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "503", description = "products-service no disponible",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    public ResponseEntity<JsonApiResponse<PurchaseResponse>> purchase(
            @Parameter(description = "Clave de idempotencia (UUID unico por intento de compra)")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,

            @Valid @RequestBody PurchaseRequest request) {

        log.debug("POST /api/v1/purchases productId={} qty={} key={}",
                request.productId(), request.quantity(), idempotencyKey);

        PurchaseResponse response = service.purchase(
                idempotencyKey, request.productId(), request.quantity());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(JsonApiResponseFactory.fromPurchase(response));
    }
}
