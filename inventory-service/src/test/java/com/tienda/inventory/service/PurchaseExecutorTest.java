package com.tienda.inventory.service;

import com.tienda.inventory.domain.Inventory;
import com.tienda.inventory.dto.PurchaseResponse;
import com.tienda.inventory.exception.InsufficientStockException;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseExecutor - Unit Tests")
class PurchaseExecutorTest {

    @Mock
    private InventoryRepository repository;

    private PurchaseExecutor executor;

    private static final UUID PRODUCT_ID = UUID.fromString("a1b2c3d4-0002-0002-0002-000000000002");

    @BeforeEach
    void setUp() {
        executor = new PurchaseExecutor(repository);
    }

    @Test
    @DisplayName("stock suficiente → descuenta correctamente y devuelve PurchaseResponse")
    void executePurchase_sufficientStock_deductsAndReturns() {
        int initialStock = 20;
        int quantity = 5;
        Inventory inventory = buildInventory(initialStock);
        when(repository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseResponse result = executor.executePurchase(PRODUCT_ID, quantity);

        assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.quantityPurchased()).isEqualTo(quantity);
        assertThat(result.remainingStock()).isEqualTo(initialStock - quantity);
        assertThat(result.purchaseId()).isNotNull();
        assertThat(result.processedAt()).isNotNull();

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAvailable()).isEqualTo(initialStock - quantity);
    }

    @Test
    @DisplayName("compra exacta del stock disponible → stock final 0")
    void executePurchase_exactStock_remainingIsZero() {
        int stock = 7;
        Inventory inventory = buildInventory(stock);
        when(repository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseResponse result = executor.executePurchase(PRODUCT_ID, stock);

        assertThat(result.remainingStock()).isZero();
    }

    @Test
    @DisplayName("cantidad > disponible → InsufficientStockException, sin llamar a save")
    void executePurchase_insufficientStock_throwsAndNoSave() {
        Inventory inventory = buildInventory(2);
        when(repository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> executor.executePurchase(PRODUCT_ID, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Solicitado: 5")
                .hasMessageContaining("Disponible: 2");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("stock = 0 → InsufficientStockException cualquier cantidad > 0")
    void executePurchase_zeroStock_throwsInsufficientStock() {
        Inventory inventory = buildInventory(0);
        when(repository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> executor.executePurchase(PRODUCT_ID, 1))
                .isInstanceOf(InsufficientStockException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("producto sin registro en inventory → ProductNotFoundException")
    void executePurchase_inventoryNotFound_throwsProductNotFound() {
        when(repository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> executor.executePurchase(PRODUCT_ID, 1))
                .isInstanceOf(ProductNotFoundException.class);

        verify(repository, never()).save(any());
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
}
