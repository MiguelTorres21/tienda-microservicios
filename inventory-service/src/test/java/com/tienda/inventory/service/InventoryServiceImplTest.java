package com.tienda.inventory.service;

import com.tienda.inventory.client.ProductsClient;
import com.tienda.inventory.domain.Inventory;
import com.tienda.inventory.dto.InventoryResponse;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.exception.ProductServiceUnavailableException;
import com.tienda.inventory.mapper.InventoryMapper;
import com.tienda.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl - Unit Tests")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository repository;
    @Mock
    private ProductsClient productsClient;
    @Mock
    private InventoryMapper mapper;

    private InventoryServiceImpl service;

    private static final UUID PRODUCT_ID = UUID.fromString("a1b2c3d4-0001-0001-0001-000000000001");

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(repository, productsClient, mapper);
    }

    @Test
    @DisplayName("producto existente + inventario en BD → devuelve InventoryResponse mapeado")
    void findByProductId_happyPath_returnsResponse() {
        Inventory inventory = buildInventory(50);
        InventoryResponse expected = buildResponse(50);

        doNothing().when(productsClient).getProduct(PRODUCT_ID);
        when(repository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(mapper.toResponse(inventory)).thenReturn(expected);

        InventoryResponse result = service.findByProductId(PRODUCT_ID);

        assertThat(result.available()).isEqualTo(50);
        assertThat(result.productId()).isEqualTo(PRODUCT_ID);

        verify(productsClient).getProduct(PRODUCT_ID);
        verify(repository).findByProductId(PRODUCT_ID);
        verify(mapper).toResponse(inventory);
    }

    @Test
    @DisplayName("products-service retorna 404 → ProductNotFoundException propagada")
    void findByProductId_whenProductNotFoundInClient_throwsProductNotFoundException() {
        doThrow(new ProductNotFoundException(PRODUCT_ID))
                .when(productsClient).getProduct(PRODUCT_ID);

        assertThatThrownBy(() -> service.findByProductId(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("producto existe en products-service pero no tiene inventario → ProductNotFoundException")
    void findByProductId_whenInventoryNotInDb_throwsProductNotFoundException() {
        doNothing().when(productsClient).getProduct(PRODUCT_ID);
        when(repository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByProductId(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("products-service caído → ProductServiceUnavailableException propagada")
    void findByProductId_whenServiceUnavailable_throwsServiceUnavailable() {
        doThrow(new ProductServiceUnavailableException("timeout"))
                .when(productsClient).getProduct(PRODUCT_ID);

        assertThatThrownBy(() -> service.findByProductId(PRODUCT_ID))
                .isInstanceOf(ProductServiceUnavailableException.class);

        verifyNoInteractions(repository, mapper);
    }


    private Inventory buildInventory(int available) {
        return Inventory.builder()
                .id(UUID.randomUUID())
                .productId(PRODUCT_ID)
                .available(available)
                .reserved(0)
                .version(0L)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private InventoryResponse buildResponse(int available) {
        return new InventoryResponse(
                UUID.randomUUID(), PRODUCT_ID, available, 0, LocalDateTime.now());
    }
}
