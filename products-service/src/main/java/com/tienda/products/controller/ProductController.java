package com.tienda.products.controller;

import com.tienda.products.domain.ProductStatus;
import com.tienda.products.dto.ProductAttributes;
import com.tienda.products.dto.jsonapi.JsonApiListResponse;
import com.tienda.products.dto.jsonapi.JsonApiResponse;
import com.tienda.products.dto.jsonapi.JsonApiResponseFactory;
import com.tienda.products.dto.ProductRequest;
import com.tienda.products.dto.ProductResponse;
import com.tienda.products.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Validated
@Tag(name = "Productos", description = "CRUD de productos con paginacion, filtros y busqueda")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService service;

    /**
     * @param service
     */
    public ProductController(ProductService service) {
        this.service = service;
    }

    @Operation(
            summary = "Listar productos",
            description = """
                    Devuelve una pagina de productos en envelope JSON:API.
                    - Filtro por status (ACTIVE, INACTIVE)
                    - Busqueda libre en sku y name (case-insensitive)
                    - Ordenamiento por price o createdAt
                    - Paginacion con page y size
                    La respuesta incluye meta con paginacion y correlationId.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "422", description = "Campo de ordenamiento invalido",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    public ResponseEntity<JsonApiListResponse<ProductAttributes>> list(
            @Parameter(description = "Filtro por estado", in = ParameterIn.QUERY)
            @RequestParam(required = false) ProductStatus status,

            @Parameter(description = "Busqueda en sku y nombre (parcial, case-insensitive)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String search,

            @Parameter(description = "Campo de ordenamiento: price | createdAt", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Direccion: asc | desc", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "desc") String sortDir,

            @Parameter(description = "Pagina 0-indexed", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Elementos por pagina (1-100)", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        log.debug("GET /api/v1/products status={} search={} sortBy={} dir={} page={} size={}",
                status, search, sortBy, sortDir, page, size);

        return ResponseEntity.ok(
                JsonApiResponseFactory.fromProductPage(
                        service.findAll(status, search, sortBy, sortDir, page, size)
                )
        );
    }

    @Operation(summary = "Obtener producto por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<JsonApiResponse<ProductAttributes>> getById(
            @Parameter(description = "UUID del producto") @PathVariable UUID id) {

        log.debug("GET /api/v1/products/{}", id);
        return ResponseEntity.ok(
                JsonApiResponseFactory.fromProduct(service.findById(id))
        );
    }

    /**
     * @param request
     * @return
     */
    @Operation(
            summary = "Crear producto",
            description = "Crea un producto nuevo. Devuelve 409 si el SKU ya existe."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado en envelope JSON:API"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "SKU ya existe",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "422", description = "Errores de validacion",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    public ResponseEntity<JsonApiResponse<ProductAttributes>> create(
            @Valid @RequestBody ProductRequest request) {

        log.debug("POST /api/v1/products sku={}", request.sku());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(JsonApiResponseFactory.fromProduct(service.create(request)));
    }

    /**
     * @param id
     * @param request
     * @return
     */
    @Operation(
            summary = "Actualizar producto",
            description = "Actualiza un producto. 404 si no existe, 409 si el nuevo SKU pertenece a otro producto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "SKU ya existe en otro producto",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "422", description = "Errores de validacion",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<JsonApiResponse<ProductAttributes>> update(
            @Parameter(description = "UUID del producto") @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {

        log.debug("PUT /api/v1/products/{}", id);
        return ResponseEntity.ok(
                JsonApiResponseFactory.fromProduct(service.update(id, request))
        );
    }

    /**
     * @param id
     * @return
     */
    @Operation(summary = "Eliminar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado (sin body)"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID del producto") @PathVariable UUID id) {

        log.debug("DELETE /api/v1/products/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
