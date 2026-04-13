package com.tienda.products.repository;

import com.tienda.products.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    /**
     * @param sku
     * @return
     */
    boolean existsBySku(String sku);

    /**
     * @param sku
     * @param id
     * @return
     */
    boolean existsBySkuAndIdNot(String sku, UUID id);

    /**
     * @param sku
     * @return
     */
    Optional<Product> findBySku(String sku);
}
