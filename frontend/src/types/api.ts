export interface JsonApiData<T> {
    type: string
    id: string | null
    attributes: T
}

export interface JsonApiMeta {
    correlationId: string
}

export interface JsonApiPageMeta extends JsonApiMeta {
    totalItems: number
    totalPages: number
    currentPage: number
    pageSize: number
}

export interface JsonApiResponse<T> {
    data: JsonApiData<T>
    meta: JsonApiMeta
}

export interface JsonApiListResponse<T> {
    data: JsonApiData<T>[]
    meta: JsonApiPageMeta
}

export interface ApiErrorEntry {
    status: string
    code: string
    title: string
    detail: string
    meta?: { correlationId?: string }
}

export interface ApiErrorEnvelope {
    errors: ApiErrorEntry[]
}

export interface LoginRequest {
    username: string
    password: string
}

export interface LoginAttributes {
    token: string
    expiresIn: number
}

export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export interface ProductAttributes {
    sku: string
    name: string
    price: number
    status: ProductStatus
    createdAt: string
    updatedAt: string
}

export interface Product extends ProductAttributes {
    id: string
}

export interface ProductRequest {
    sku: string
    name: string
    price: number
    status?: ProductStatus
}

export interface ProductFilters {
    status?: ProductStatus | ''
    search?: string
    sortBy?: 'price' | 'createdAt'
    sortDir?: 'asc' | 'desc'
    page?: number
    size?: number
}

export interface InventoryAttributes {
    productId: string
    available: number
    reserved: number
    updatedAt: string
}

export interface PurchaseRequest {
    productId: string
    quantity: number
}

export interface PurchaseResponse {
    purchaseId: string
    productId: string
    quantityPurchased: number
    remainingStock: number
    processedAt: string
}
