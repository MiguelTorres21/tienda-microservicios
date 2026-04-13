package com.tienda.products.service;

import com.tienda.products.domain.ProductStatus;
import com.tienda.products.dto.ProductPageResponse;
import com.tienda.products.dto.ProductRequest;
import com.tienda.products.dto.ProductResponse;

import java.util.UUID;

public interface ProductService {

    /**
     * @param request
     * @return
     */
    ProductResponse create(ProductRequest request);

    /**
     * @param id
     * @return
     */
    ProductResponse findById(UUID id);

    /**
     * @param sku
     * @return
     */
    ProductResponse findBySku(String sku);

    /**
     * @param status
     * @param search
     * @param sortBy
     * @param sortDir
     * @param page
     * @param size
     * @return
     */
    ProductPageResponse findAll(
            ProductStatus status,
            String search,
            String sortBy,
            String sortDir,
            int page,
            int size
    );


    /**
     * @param id
     * @param request
     * @return
     */
    ProductResponse update(UUID id, ProductRequest request);

    /**
     * @param id
     */
    void delete(UUID id);
}
