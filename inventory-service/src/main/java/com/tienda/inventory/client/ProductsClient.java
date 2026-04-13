package com.tienda.inventory.client;

import com.tienda.inventory.dto.ProductDto;
import com.tienda.inventory.dto.jsonapi.ProductClientResponse;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.exception.ProductServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Component
public class ProductsClient {

    private static final Logger log = LoggerFactory.getLogger(ProductsClient.class);
    private static final String CB_NAME = "products-client";

    private final WebClient webClient;
    private final String apiKey;

    public ProductsClient(
            WebClient productsWebClient,
            @Value("${products-service.internal-api-key}") String apiKey) {
        this.webClient = productsWebClient;
        this.apiKey = apiKey;
    }

    @Retry(name = CB_NAME, fallbackMethod = "fallbackGetProduct")
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "fallbackGetProduct")
    public void getProduct(UUID productId) {
        log.debug("Consultando products-service: productId={}", productId);

        try {
            ProductClientResponse response = webClient.get()
                    .uri("/internal/v1/products/{id}", productId)
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .bodyToMono(ProductClientResponse.class)
                    .block();

            if (response == null || response.data() == null
                    || response.data().attributes() == null) {
                throw new ProductServiceUnavailableException("Respuesta vacia de products-service");
            }

            log.debug("Producto {} obtenido de products-service", productId);

        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Producto {} no encontrado en products-service (404)", productId);
                throw new ProductNotFoundException(productId);
            }
            log.error("products-service respondio HTTP {} para producto {}",
                    ex.getStatusCode(), productId);
            throw new ProductServiceUnavailableException(
                    "products-service respondio con " + ex.getStatusCode());

        } catch (ProductNotFoundException | ProductServiceUnavailableException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error("Error de red/conexion con products-service para producto {}: {} — {}",
                    productId, ex.getClass().getSimpleName(), ex.getMessage());
            throw new ProductServiceUnavailableException(
                    "Error de conexion: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /**
     * @param productId
     * @param t
     * @return
     */
    @SuppressWarnings("unused")
    private ProductDto fallbackGetProduct(UUID productId, Throwable t) {
        Throwable root = rootCause(t);

        log.error("Fallback products-service activado para productId={}: {} — {}",
                productId, root.getClass().getSimpleName(), root.getMessage());

        if (root instanceof ProductNotFoundException pnfe) {
            throw pnfe;
        }

        if (root instanceof ProductServiceUnavailableException psue) {
            throw psue;
        }

        throw new ProductServiceUnavailableException(
                "products-service no disponible [fallback: "
                        + root.getClass().getSimpleName() + "]: " + root.getMessage());
    }

    /**
     * @param ex
     * @return
     */
    private Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}