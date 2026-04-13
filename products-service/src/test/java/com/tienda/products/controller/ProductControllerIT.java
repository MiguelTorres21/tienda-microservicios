package com.tienda.products.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.products.config.JwtService;
import com.tienda.products.domain.Product;
import com.tienda.products.domain.ProductStatus;
import com.tienda.products.integration.BaseIntegrationTest;
import com.tienda.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ProductController — /api/v1/products")
class ProductControllerIT extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtService jwtService;
    @Autowired
    ProductRepository repository;

    private static final String BASE_URL = "/api/v1/products";

    private String authHeader;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        authHeader = "Bearer " + jwtService.generateToken("admin");
    }

    @Nested
    @DisplayName("Autenticación")
    class Auth {

        @Test
        @DisplayName("GET sin JWT devuelve 401 JSON:API")
        void get_returns401_withoutJwt() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors[0].status").value("401"));
        }

        @Test
        @DisplayName("GET con JWT inválido devuelve 401")
        void get_returns401_withInvalidJwt() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer token.invalido.aqui"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST sin JWT devuelve 401")
        void post_returns401_withoutJwt() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-X", "Producto X", "10.00", "ACTIVE")))
                    .andExpect(status().isUnauthorized());
        }
    }


    @Nested
    @DisplayName("GET /api/v1/products")
    class List {

        @Test
        @DisplayName("devuelve 200 con envelope JSON:API y meta de paginación")
        void list_returns200_withJsonApiEnvelope() throws Exception {
            crearProducto("SKU-A", "Producto A", "100.00", ProductStatus.ACTIVE);

            mockMvc.perform(get(BASE_URL).header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.meta.totalItems").isNumber())
                    .andExpect(jsonPath("$.meta.totalPages").isNumber())
                    .andExpect(jsonPath("$.meta.currentPage").value(0))
                    .andExpect(jsonPath("$.meta.correlationId").exists());
        }

        @Test
        @DisplayName("cada item del listado tiene estructura JSON:API correcta (type, id, attributes)")
        void list_itemsHaveCorrectJsonApiStructure() throws Exception {
            crearProducto("SKU-B", "Producto B", "50.00", ProductStatus.ACTIVE);

            mockMvc.perform(get(BASE_URL).header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].type").value("products"))
                    .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                    .andExpect(jsonPath("$.data[0].attributes.sku").value("SKU-B"))
                    .andExpect(jsonPath("$.data[0].attributes.name").value("Producto B"))
                    .andExpect(jsonPath("$.data[0].attributes.price").isNumber())
                    .andExpect(jsonPath("$.data[0].attributes.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data[0].attributes.id").doesNotExist());
        }

        @Test
        @DisplayName("filtro status=ACTIVE excluye productos INACTIVE")
        void list_filterByStatus_onlyReturnsActive() throws Exception {
            crearProducto("SKU-ACT", "Activo", "10.00", ProductStatus.ACTIVE);
            crearProducto("SKU-INA", "Inactivo", "20.00", ProductStatus.INACTIVE);

            mockMvc.perform(get(BASE_URL + "?status=ACTIVE").header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].attributes.sku", not(hasItem("SKU-INA"))))
                    .andExpect(jsonPath("$.data[*].attributes.sku", hasItem("SKU-ACT")));
        }

        @Test
        @DisplayName("filtro status=INACTIVE solo devuelve inactivos")
        void list_filterByStatus_onlyReturnsInactive() throws Exception {
            crearProducto("SKU-ACT2", "Activo2", "10.00", ProductStatus.ACTIVE);
            crearProducto("SKU-INA2", "Inactivo2", "20.00", ProductStatus.INACTIVE);

            mockMvc.perform(get(BASE_URL + "?status=INACTIVE").header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].attributes.sku", not(hasItem("SKU-ACT2"))))
                    .andExpect(jsonPath("$.data[*].attributes.sku", hasItem("SKU-INA2")));
        }

        @Test
        @DisplayName("búsqueda por nombre parcial devuelve resultados coincidentes (case-insensitive)")
        void list_searchByName_returnsMatches() throws Exception {
            crearProducto("SKU-LAP", "Laptop Gaming", "1000.00", ProductStatus.ACTIVE);
            crearProducto("SKU-MOU", "Mouse USB", "25.00", ProductStatus.ACTIVE);

            mockMvc.perform(get(BASE_URL + "?search=laptop").header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].attributes.name", containsStringIgnoringCase("laptop")));
        }

        @Test
        @DisplayName("búsqueda por SKU parcial devuelve resultados coincidentes")
        void list_searchBySku_returnsMatches() throws Exception {
            crearProducto("SKU-001", "Producto Uno", "10.00", ProductStatus.ACTIVE);
            crearProducto("SKU-002", "Producto Dos", "20.00", ProductStatus.ACTIVE);

            mockMvc.perform(get(BASE_URL + "?search=SKU-001").header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].attributes.sku").value("SKU-001"));
        }

        @Test
        @DisplayName("búsqueda sin coincidencias devuelve lista vacía")
        void list_searchWithNoMatches_returnsEmptyArray() throws Exception {
            crearProducto("SKU-X", "Producto X", "10.00", ProductStatus.ACTIVE);

            mockMvc.perform(get(BASE_URL + "?search=NOEXISTE-XYZ").header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.meta.totalItems").value(0));
        }

        @Test
        @DisplayName("sortBy=price devuelve 200 correctamente")
        void list_sortByPrice_returns200() throws Exception {
            crearProducto("SKU-C1", "Barato", "10.00", ProductStatus.ACTIVE);
            crearProducto("SKU-C2", "Caro", "999.00", ProductStatus.ACTIVE);

            mockMvc.perform(get(BASE_URL + "?sortBy=price&sortDir=asc").header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].attributes.price").value(10.00));
        }

        @Test
        @DisplayName("sortBy=precio (campo inválido) devuelve 422 INVALID_SORT_FIELD")
        void list_invalidSortBy_returns422() throws Exception {
            mockMvc.perform(get(BASE_URL + "?sortBy=precio").header("Authorization", authHeader))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SORT_FIELD"));
        }

        @Test
        @DisplayName("paginación: page y size se reflejan correctamente en meta")
        void list_paginationMeta_isCorrect() throws Exception {
            for (int i = 1; i <= 5; i++) {
                crearProducto("SKU-P" + i, "Producto " + i, i + ".00", ProductStatus.ACTIVE);
            }

            mockMvc.perform(get(BASE_URL + "?page=0&size=2").header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.meta.currentPage").value(0))
                    .andExpect(jsonPath("$.meta.pageSize").value(2))
                    .andExpect(jsonPath("$.meta.totalItems").value(5))
                    .andExpect(jsonPath("$.meta.totalPages").value(3));
        }

        @Test
        @DisplayName("page=1 devuelve la segunda página")
        void list_secondPage_returnsCorrectItems() throws Exception {
            for (int i = 1; i <= 5; i++) {
                crearProducto("SKU-PP" + i, "Producto " + i, i + ".00", ProductStatus.ACTIVE);
            }

            mockMvc.perform(get(BASE_URL + "?page=1&size=2&sortBy=price&sortDir=asc")
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.meta.currentPage").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetById {

        @Test
        @DisplayName("producto existente devuelve 200 con datos correctos")
        void getById_returns200_whenProductExists() throws Exception {
            Product saved = crearProducto("SKU-D", "Detalle", "75.00", ProductStatus.ACTIVE);

            mockMvc.perform(get(BASE_URL + "/" + saved.getId()).header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("products"))
                    .andExpect(jsonPath("$.data.id").value(saved.getId().toString()))
                    .andExpect(jsonPath("$.data.attributes.sku").value("SKU-D"))
                    .andExpect(jsonPath("$.data.attributes.name").value("Detalle"))
                    .andExpect(jsonPath("$.data.attributes.price").value(75.00))
                    .andExpect(jsonPath("$.data.attributes.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.attributes.createdAt").exists())
                    .andExpect(jsonPath("$.data.attributes.updatedAt").exists());
        }

        @Test
        @DisplayName("producto inexistente devuelve 404 con código PRODUCT_NOT_FOUND")
        void getById_returns404_whenProductMissing() throws Exception {
            String unknownId = "00000000-0000-0000-0000-000000000001";

            mockMvc.perform(get(BASE_URL + "/" + unknownId).header("Authorization", authHeader))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"))
                    .andExpect(jsonPath("$.errors[0].status").value("404"));
        }

        @Test
        @DisplayName("UUID malformado devuelve 422")
        void getById_returns422_withMalformedUuid() throws Exception {
            mockMvc.perform(get(BASE_URL + "/no-es-uuid").header("Authorization", authHeader))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class Create {

        @Test
        @DisplayName("body válido devuelve 201 con el producto en JSON:API")
        void create_returns201_withValidBody() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-NEW", "Nuevo", "199.99", "ACTIVE")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.type").value("products"))
                    .andExpect(jsonPath("$.data.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.attributes.sku").value("SKU-NEW"))
                    .andExpect(jsonPath("$.data.attributes.name").value("Nuevo"))
                    .andExpect(jsonPath("$.data.attributes.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("SKU duplicado devuelve 409 SKU_ALREADY_EXISTS")
        void create_returns409_whenSkuDuplicated() throws Exception {
            crearProducto("SKU-DUP", "Original", "10.00", ProductStatus.ACTIVE);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-DUP", "Duplicado", "20.00", "ACTIVE")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0].code").value("SKU_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.errors[0].detail", containsString("SKU-DUP")));
        }

        @Test
        @DisplayName("SKU en blanco devuelve 422 VALIDATION_ERROR")
        void create_returns422_whenSkuBlank() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("", "Producto", "10.00", "ACTIVE")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("precio negativo devuelve 422 VALIDATION_ERROR")
        void create_returns422_whenPriceNegative() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-NEG", "Negativo", "-1.00", "ACTIVE")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("precio nulo devuelve 422 VALIDATION_ERROR")
        void create_returns422_whenPriceNull() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "sku", "SKU-NULL", "name", "Producto"));

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("body malformado (no JSON) devuelve 422 MALFORMED_REQUEST")
        void create_returns422_whenBodyMalformed() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("esto no es json"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("status por defecto es ACTIVE cuando no se envía")
        void create_defaultsStatusToActive_whenNotProvided() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "sku", "SKU-NOSTAT", "name", "Sin status",
                    "price", 50.00));

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.attributes.status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class Update {

        @Test
        @DisplayName("actualización válida devuelve 200 con datos actualizados")
        void update_returns200_withValidBody() throws Exception {
            Product saved = crearProducto("SKU-UPD", "Original", "50.00", ProductStatus.ACTIVE);

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-UPD", "Actualizado", "99.00", "INACTIVE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.attributes.name").value("Actualizado"))
                    .andExpect(jsonPath("$.data.attributes.price").value(99.00))
                    .andExpect(jsonPath("$.data.attributes.status").value("INACTIVE"));
        }

        @Test
        @DisplayName("producto inexistente devuelve 404")
        void update_returns404_whenProductMissing() throws Exception {
            String unknownId = "00000000-0000-0000-0000-000000000002";

            mockMvc.perform(put(BASE_URL + "/" + unknownId)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-X", "X", "10.00", "ACTIVE")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        @DisplayName("SKU que pertenece a otro producto devuelve 409 SKU_ALREADY_EXISTS")
        void update_returns409_whenSkuTakenByOtherProduct() throws Exception {
            Product pA = crearProducto("SKU-PA", "Producto A", "10.00", ProductStatus.ACTIVE);
            crearProducto("SKU-PB", "Producto B", "20.00", ProductStatus.ACTIVE);

            mockMvc.perform(put(BASE_URL + "/" + pA.getId())
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-PB", "Producto A renamed", "10.00", "ACTIVE")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0].code").value("SKU_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("permite actualizar con el mismo SKU del propio producto (no conflicto)")
        void update_allowsSameSku_forSameProduct() throws Exception {
            Product saved = crearProducto("SKU-SAME", "Original", "50.00", ProductStatus.ACTIVE);

            mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("SKU-SAME", "Actualizado", "60.00", "ACTIVE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.attributes.name").value("Actualizado"));
        }
    }


    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class Delete {

        @Test
        @DisplayName("elimina producto existente y devuelve 204 sin body")
        void delete_returns204_whenProductExists() throws Exception {
            Product saved = crearProducto("SKU-DEL", "Eliminar", "10.00", ProductStatus.ACTIVE);

            mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                            .header("Authorization", authHeader))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE_URL + "/" + saved.getId())
                            .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("eliminar producto inexistente devuelve 404")
        void delete_returns404_whenProductMissing() throws Exception {
            String unknownId = "00000000-0000-0000-0000-000000000003";

            mockMvc.perform(delete(BASE_URL + "/" + unknownId)
                            .header("Authorization", authHeader))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("PRODUCT_NOT_FOUND"));
        }
    }

    private Product crearProducto(String sku, String name, String price, ProductStatus status) {
        return repository.save(Product.builder()
                .sku(sku)
                .name(name)
                .price(new BigDecimal(price))
                .status(status)
                .build());
    }

    private String productJson(String sku, String name, String price, String status) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "sku", sku,
                "name", name,
                "price", new BigDecimal(price),
                "status", status));
    }
}
