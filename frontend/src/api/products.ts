import {productsClient} from './client'
import type {
    JsonApiResponse, JsonApiListResponse,
    LoginAttributes, ProductAttributes, Product,
    ProductRequest, ProductFilters
} from '@/types/api'

export async function login(username: string, password: string): Promise<string> {
    const {data} = await productsClient.post<JsonApiResponse<LoginAttributes>>(
        '/api/v1/auth/login', {username, password}
    )
    return data.data.attributes.token
}


function toProduct(item: { id: string | null; attributes: ProductAttributes }): Product {
    return {id: item.id!, ...item.attributes}
}


export interface ProductPage {
    products: Product[]
    totalItems: number
    totalPages: number
    currentPage: number
    pageSize: number
}

export async function listProducts(filters: ProductFilters = {}): Promise<ProductPage> {
    const params: Record<string, string | number> = {}
    if (filters.status) params.status = filters.status
    if (filters.search) params.search = filters.search
    if (filters.sortBy) params.sortBy = filters.sortBy
    if (filters.sortDir) params.sortDir = filters.sortDir
    params.page = filters.page ?? 0
    params.size = filters.size ?? 10

    const {data} = await productsClient.get<JsonApiListResponse<ProductAttributes>>(
        '/api/v1/products', {params}
    )

    return {
        products: data.data.map(toProduct),
        totalItems: data.meta.totalItems,
        totalPages: data.meta.totalPages,
        currentPage: data.meta.currentPage,
        pageSize: data.meta.pageSize,
    }
}

export async function getProduct(id: string): Promise<Product> {
    const {data} = await productsClient.get<JsonApiResponse<ProductAttributes>>(
        `/api/v1/products/${id}`
    )
    return toProduct(data.data)
}

export async function createProduct(req: ProductRequest): Promise<Product> {
    const {data} = await productsClient.post<JsonApiResponse<ProductAttributes>>(
        '/api/v1/products', req
    )
    return toProduct(data.data)
}

export async function updateProduct(id: string, req: ProductRequest): Promise<Product> {
    const {data} = await productsClient.put<JsonApiResponse<ProductAttributes>>(
        `/api/v1/products/${id}`, req
    )
    return toProduct(data.data)
}

export async function deleteProduct(id: string): Promise<void> {
    await productsClient.delete(`/api/v1/products/${id}`)
}
