import {describe, it, expect, vi, beforeEach} from 'vitest'
import {setActivePinia, createPinia} from 'pinia'
import {useProductsStore} from '@/stores/products'
import * as productsApi from '@/api/products'
import {AppError} from '@/api/client'
import type {ProductPage} from '@/api/products'

vi.mock('@/api/products')

const mockPage: ProductPage = {
    products: [{id: '1', sku: 'SKU-001', name: 'Laptop', price: 999, status: 'ACTIVE', createdAt: '', updatedAt: ''}],
    totalItems: 1,
    totalPages: 1,
    currentPage: 0,
    pageSize: 10
}

describe('useProductsStore', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        vi.clearAllMocks()
    })

    it('fetchProducts carga y almacena la página', async () => {
        vi.mocked(productsApi.listProducts).mockResolvedValue(mockPage)

        const store = useProductsStore()
        await store.fetchProducts()

        expect(store.currentPage).toEqual(mockPage)
        expect(store.loading).toBe(false)
        expect(store.error).toBeNull()
    })

    it('usa caché en segunda llamada con mismos filtros', async () => {
        vi.mocked(productsApi.listProducts).mockResolvedValue(mockPage)

        const store = useProductsStore()
        await store.fetchProducts({status: 'ACTIVE'})
        await store.fetchProducts({status: 'ACTIVE'})

        expect(productsApi.listProducts).toHaveBeenCalledTimes(1)
    })

    it('invalidateCache fuerza nueva llamada a la API', async () => {
        vi.mocked(productsApi.listProducts).mockResolvedValue(mockPage)

        const store = useProductsStore()
        await store.fetchProducts()
        store.invalidateCache()
        await store.fetchProducts()

        expect(productsApi.listProducts).toHaveBeenCalledTimes(2)
    })

    it('maneja errores de API y los guarda en error', async () => {
        vi.mocked(productsApi.listProducts).mockRejectedValue(
            new AppError(503, 'SERVICE_UNAVAILABLE', 'Sin respuesta del servidor.')
        )

        const store = useProductsStore()
        await store.fetchProducts()

        expect(store.error).toBe('Sin respuesta del servidor.')
        expect(store.currentPage).toBeNull()
        expect(store.loading).toBe(false)
    })
})
