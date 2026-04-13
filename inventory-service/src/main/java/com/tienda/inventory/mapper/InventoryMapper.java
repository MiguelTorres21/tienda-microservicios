package com.tienda.inventory.mapper;

import com.tienda.inventory.domain.Inventory;
import com.tienda.inventory.dto.InventoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    /**
     * @param inventory
     * @return
     */
    InventoryResponse toResponse(Inventory inventory);
}
