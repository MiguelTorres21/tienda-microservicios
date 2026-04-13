package com.tienda.inventory.service;

import com.tienda.inventory.domain.Inventory;
import com.tienda.inventory.dto.PurchaseResponse;
import com.tienda.inventory.exception.ConcurrentPurchaseException;
import com.tienda.inventory.exception.InsufficientStockException;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PurchaseExecutor {

    private static final Logger log = LoggerFactory.getLogger(PurchaseExecutor.class);

    private final InventoryRepository repository;

    /**
     * @param repository
     */
    public PurchaseExecutor(InventoryRepository repository) {
        this.repository = repository;
    }

    /**
     * @param productId
     * @param quantity
     * @return
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            noRetryFor = {InsufficientStockException.class, ProductNotFoundException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public PurchaseResponse executePurchase(UUID productId, Integer quantity) {

        Inventory inventory = repository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (inventory.getAvailable() < quantity) {
            throw new InsufficientStockException(productId, quantity, inventory.getAvailable());
        }

        inventory.setAvailable(inventory.getAvailable() - quantity);
        repository.save(inventory);

        UUID purchaseId = UUID.randomUUID();
        log.info("Compra ejecutada [{}]: purchaseId={} productId={} qty={} remaining={}",
                cid(), purchaseId, productId, quantity, inventory.getAvailable());

        return new PurchaseResponse(
                purchaseId,
                productId,
                quantity,
                inventory.getAvailable(),
                LocalDateTime.now()
        );
    }

    @Recover
    public PurchaseResponse recoverFromOptimisticLock(
            ObjectOptimisticLockingFailureException ex,
            UUID productId,
            Integer quantity) {

        log.error("Reintentos agotados por concurrencia [{}] productId={}: {}",
                cid(), productId, ex.getMessage());
        throw new ConcurrentPurchaseException();
    }

    @Recover
    public PurchaseResponse recoverFromInsufficientStock(
            InsufficientStockException ex,
            UUID productId,
            Integer quantity) {
        throw ex;
    }

    @Recover
    public PurchaseResponse recoverFromProductNotFound(
            ProductNotFoundException ex,
            UUID productId,
            Integer quantity) {
        throw ex;
    }

    private String cid() {
        String id = MDC.get("correlationId");
        return id != null ? id : "";
    }
}
