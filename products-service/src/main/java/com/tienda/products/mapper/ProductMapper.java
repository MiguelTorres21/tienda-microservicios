package com.tienda.products.mapper;

import com.tienda.products.domain.Product;
import com.tienda.products.dto.ProductRequest;
import com.tienda.products.dto.ProductResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    /**
     * @param product
     * @return
     */
    ProductResponse toResponse(Product product);

    /**
     * @param request
     * @return
     */
    Product toEntity(ProductRequest request);

    /**
     * @param request
     * @param product
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);
}
