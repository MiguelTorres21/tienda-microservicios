package com.tienda.inventory.dto.jsonapi;

import com.tienda.inventory.dto.InventoryAttributes;
import com.tienda.inventory.dto.InventoryResponse;
import com.tienda.inventory.dto.PurchaseResponse;
import org.slf4j.MDC;

public final class JsonApiResponseFactory {

    private static final String TYPE_INVENTORY = "inventory";
    private static final String TYPE_PURCHASES = "purchases";

    private JsonApiResponseFactory() {
    }

    /**
     * @param inv
     * @return
     */
    public static JsonApiResponse<InventoryAttributes> fromInventory(InventoryResponse inv) {
        InventoryAttributes attributes = new InventoryAttributes(
                inv.productId(),
                inv.available(),
                inv.reserved(),
                inv.updatedAt()
        );
        return new JsonApiResponse<>(
                new JsonApiData<>(TYPE_INVENTORY, inv.id().toString(), attributes),
                new JsonApiMeta(correlationId())
        );
    }

    /**
     * @param purchase
     * @return
     */
    public static JsonApiResponse<PurchaseResponse> fromPurchase(PurchaseResponse purchase) {
        return new JsonApiResponse<>(
                new JsonApiData<>(TYPE_PURCHASES, purchase.purchaseId().toString(), purchase),
                new JsonApiMeta(correlationId())
        );
    }

    private static String correlationId() {
        String id = MDC.get("correlationId");
        return id != null ? id : "";
    }
}
