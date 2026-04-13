import {inventoryClient} from './client'
import type {
    JsonApiResponse, InventoryAttributes, PurchaseRequest, PurchaseResponse
} from '@/types/api'

export interface InventoryInfo extends InventoryAttributes {
    id: string
}

export async function getInventory(productId: string): Promise<InventoryInfo> {
    const {data} = await inventoryClient.get<JsonApiResponse<InventoryAttributes>>(
        `/api/v1/inventory/${productId}`
    )
    return {id: data.data.id!, ...data.data.attributes}
}

export async function purchase(
    req: PurchaseRequest,
    idempotencyKey: string
): Promise<PurchaseResponse> {
    const {data} = await inventoryClient.post<JsonApiResponse<PurchaseResponse>>(
        '/api/v1/purchases',
        req,
        {headers: {'Idempotency-Key': idempotencyKey}}
    )
    return data.data.attributes
}
