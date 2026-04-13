package com.tienda.inventory.controller;

import com.tienda.inventory.dto.InventoryAttributes;
import com.tienda.inventory.dto.jsonapi.JsonApiResponse;
import com.tienda.inventory.dto.jsonapi.JsonApiResponseFactory;
import com.tienda.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventario", description = "Consulta de stock por producto")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @Operation(summary = "Consultar inventario por producto",
            description = "Devuelve el stock disponible. Valida existencia del producto en products-service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventario encontrado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "503", description = "products-service no disponible",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{productId}")
    public ResponseEntity<JsonApiResponse<InventoryAttributes>> getByProductId(
            @Parameter(description = "UUID del producto") @PathVariable UUID productId) {

        log.debug("GET /api/v1/inventory/{}", productId);
        return ResponseEntity.ok(
                JsonApiResponseFactory.fromInventory(service.findByProductId(productId))
        );
    }
}
