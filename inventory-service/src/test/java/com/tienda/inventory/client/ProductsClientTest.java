package com.tienda.inventory.client;

import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.exception.ProductServiceUnavailableException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductsClient - Unit Tests con MockWebServer")
class ProductsClientTest {

    private MockWebServer mockWebServer;
    private ProductsClient client;

    private static final String API_KEY = "test-api-key";
    private static final UUID PRODUCT_ID = UUID.randomUUID();


    private static final String VALID_RESPONSE = """
            {
              "data": {
                "type": "products",
                "id": "%s",
                "attributes": {
                  "id": "%s",
                  "sku": "SKU-TEST-001",
                  "name": "Producto de Test",
                  "price": 29.99,
                  "status": "ACTIVE"
                }
              },
              "meta": { "correlationId": "test-correlation-id" }
            }
            """.formatted(PRODUCT_ID, PRODUCT_ID);

    private static final String NULL_DATA_RESPONSE = """
            {"data": null, "meta": {"correlationId": ""}}
            """;

    private static final String NULL_ATTRIBUTES_RESPONSE = """
            {
              "data": {"type": "products", "id": "%s", "attributes": null},
              "meta": {"correlationId": ""}
            }
            """.formatted(PRODUCT_ID);

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        client = new ProductsClient(webClient, API_KEY);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("200 con body válido → no lanza excepción, envía X-API-Key correcto")
    void getProduct_when200ValidResponse_succeeds() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(VALID_RESPONSE));

        assertThatNoException().isThrownBy(() -> client.getProduct(PRODUCT_ID));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("X-API-Key")).isEqualTo(API_KEY);
        assertThat(request.getPath()).contains(PRODUCT_ID.toString());
    }

    @Test
    @DisplayName("200 con body vacío → response null → ProductServiceUnavailableException")
    void getProduct_whenEmptyBody_throwsServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(""));

        assertThatThrownBy(() -> client.getProduct(PRODUCT_ID))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    @DisplayName("200 con data=null → rama response.data()==null → ProductServiceUnavailableException")
    void getProduct_whenDataIsNull_throwsServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(NULL_DATA_RESPONSE));

        assertThatThrownBy(() -> client.getProduct(PRODUCT_ID))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("vacia");
    }

    @Test
    @DisplayName("200 con attributes=null → rama attributes()==null → ProductServiceUnavailableException")
    void getProduct_whenAttributesIsNull_throwsServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(NULL_ATTRIBUTES_RESPONSE));

        assertThatThrownBy(() -> client.getProduct(PRODUCT_ID))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("vacia");
    }

    @Test
    @DisplayName("404 de products-service → ProductNotFoundException")
    void getProduct_when404_throwsProductNotFoundException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errors\":[{\"status\":\"404\",\"code\":\"PRODUCT_NOT_FOUND\"}]}"));

        assertThatThrownBy(() -> client.getProduct(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(PRODUCT_ID.toString());
    }

    @Test
    @DisplayName("500 de products-service → ProductServiceUnavailableException con status en mensaje")
    void getProduct_when500_throwsServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Internal Server Error\"}"));

        assertThatThrownBy(() -> client.getProduct(PRODUCT_ID))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("503 de products-service → ProductServiceUnavailableException")
    void getProduct_when503_throwsServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Service Unavailable\"}"));

        assertThatThrownBy(() -> client.getProduct(PRODUCT_ID))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    @DisplayName("servidor cerrado → error de conexión → ProductServiceUnavailableException")
    void getProduct_whenConnectionRefused_throwsServiceUnavailable() throws IOException {
        mockWebServer.shutdown();

        assertThatThrownBy(() -> client.getProduct(PRODUCT_ID))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("Error de conexion");

        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @Test
    @DisplayName("fallback: causa raíz es ProductNotFoundException → la rethrows")
    void fallback_whenRootCauseIsProductNotFoundException_rethrows() {
        ProductNotFoundException pnfe = new ProductNotFoundException(PRODUCT_ID);

        assertThatThrownBy(() -> invokeFallback(PRODUCT_ID, pnfe))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("fallback: causa raíz es ProductServiceUnavailableException → la rethrows")
    void fallback_whenRootCauseIsServiceUnavailable_rethrows() {
        ProductServiceUnavailableException psue =
                new ProductServiceUnavailableException("timeout");

        assertThatThrownBy(() -> invokeFallback(PRODUCT_ID, psue))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("timeout");
    }

    @Test
    @DisplayName("fallback: causa raíz es otra excepción → ProductServiceUnavailableException con msg fallback")
    void fallback_whenRootCauseIsUnknown_throwsServiceUnavailableWithFallbackMsg() {
        RuntimeException unknown = new RuntimeException("unexpected failure");

        assertThatThrownBy(() -> invokeFallback(PRODUCT_ID, unknown))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("fallback");
    }

    @Test
    @DisplayName("fallback: excepción encadenada → extrae la causa raíz correctamente")
    void fallback_whenWrappedException_extractsRootCause() {
        // Simula lo que hace Resilience4j: envuelve la excepción original
        ProductNotFoundException rootCause = new ProductNotFoundException(PRODUCT_ID);
        RuntimeException wrapped = new RuntimeException("wrapper", rootCause);

        assertThatThrownBy(() -> invokeFallback(PRODUCT_ID, wrapped))
                .isInstanceOf(ProductNotFoundException.class);
    }

    /**
     * @param productId
     * @param cause
     * @throws Throwable
     */
    private void invokeFallback(UUID productId, Throwable cause) throws Throwable {
        try {
            Method method = ProductsClient.class.getDeclaredMethod(
                    "fallbackGetProduct", UUID.class, Throwable.class);
            method.setAccessible(true);
            method.invoke(client, productId, cause);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
