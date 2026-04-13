import {defineStore} from 'pinia'
import {ref} from 'vue'
import {listProducts, getProduct, type ProductPage} from '@/api/products'
import type {Product, ProductFilters} from '@/types/api'

const CACHE_TTL_MS = 60_000

interface CacheEntry {
    data: ProductPage
    expiresAt: number
}

export const useProductsStore = defineStore('products', () => {
    const currentPage = ref<ProductPage | null>(null)
    const selectedProduct = ref<Product | null>(null)
    const loading = ref(false)
    const error = ref<string | null>(null)

    const cache = new Map<string, CacheEntry>()

    async function fetchProducts(filters: ProductFilters = {}): Promise<void> {
        const key = JSON.stringify(filters)
        const cached = cache.get(key)

        if (cached && cached.expiresAt > Date.now()) {
            currentPage.value = cached.data
            return
        }

        loading.value = true
        error.value = null

        try {
            const page = await listProducts(filters)
            currentPage.value = page
            cache.set(key, {data: page, expiresAt: Date.now() + CACHE_TTL_MS})
        } catch (e: unknown) {
            error.value = e instanceof Error ? e.message : 'Error al cargar productos'
        } finally {
            loading.value = false
        }
    }

    async function fetchProduct(id: string): Promise<void> {
        loading.value = true
        error.value = null
        try {
            selectedProduct.value = await getProduct(id)
        } catch (e: unknown) {
            error.value = e instanceof Error ? e.message : 'Error al cargar el producto'
        } finally {
            loading.value = false
        }
    }

    function invalidateCache(): void {
        cache.clear()
    }

    return {
        currentPage, selectedProduct, loading, error,
        fetchProducts, fetchProduct, invalidateCache
    }
})
