package com.tienda.inventory.service;

import com.tienda.inventory.client.ProductsClient;
import com.tienda.inventory.domain.Inventory;
import com.tienda.inventory.dto.InventoryResponse;
import com.tienda.inventory.exception.ProductNotFoundException;
import com.tienda.inventory.mapper.InventoryMapper;
import com.tienda.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository repository;
    private final ProductsClient productsClient;
    private final InventoryMapper mapper;

    public InventoryServiceImpl(InventoryRepository repository, ProductsClient productsClient, InventoryMapper mapper) {
        this.repository = repository;
        this.productsClient = productsClient;
        this.mapper = mapper;
    }

    @Override
    public InventoryResponse findByProductId(UUID productId) {
        log.debug("Consultando inventario de productId={}", productId);

        productsClient.getProduct(productId);

        Inventory inventory = repository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return mapper.toResponse(inventory);
    }
}
