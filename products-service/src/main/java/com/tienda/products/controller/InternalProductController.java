package com.tienda.products.controller;

import com.tienda.products.dto.ProductAttributes;
import com.tienda.products.dto.ProductResponse;
import com.tienda.products.dto.jsonapi.JsonApiResponse;
import com.tienda.products.dto.jsonapi.JsonApiResponseFactory;
import com.tienda.products.service.ProductService;
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
@RequestMapping("/internal/v1/products")
@Tag(name = "Productos (interno)", description = "Endpoints para comunicacion inter-servicios")
@SecurityRequirement(name = "apiKeyAuth")
public class InternalProductController {

    private static final Logger log = LoggerFactory.getLogger(InternalProductController.class);

    private final ProductService service;

    /**
     * @param service
     */
    public InternalProductController(ProductService service) {
        this.service = service;
    }

    @Operation(
            summary = "Verificar existencia de producto por ID",
            description = """
                    Consultado por inventory-service antes de procesar una compra.
                    Requiere header X-API-Key. Devuelve 404 si el producto no existe.
                    Respuesta en formato JSON:API.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "401", description = "API Key invalida o ausente",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<JsonApiResponse<ProductAttributes>> getById(
            @Parameter(description = "UUID del producto a verificar")
            @PathVariable UUID id) {

        log.debug("INTERNAL GET /internal/v1/products/{}", id);
        return ResponseEntity.ok(
                JsonApiResponseFactory.fromProduct(service.findById(id))
        );
    }
}