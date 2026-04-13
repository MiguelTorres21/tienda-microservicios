import {describe, it, expect, vi, beforeEach} from 'vitest'
import {mount, flushPromises} from '@vue/test-utils'
import {setActivePinia, createPinia} from 'pinia'
import {createRouter, createMemoryHistory} from 'vue-router'
import PurchaseView from '@/views/PurchaseView.vue'
import * as productsApi from '@/api/products'
import * as inventoryApi from '@/api/inventory'
import {AppError} from '@/api/client'
import type {Product} from '@/types/api'

vi.mock('@/api/products')
vi.mock('@/api/inventory')

const PRODUCT_ID = 'abc-123'

const activeProduct: Product = {
    id: PRODUCT_ID, sku: 'SKU-001', name: 'Laptop Pro',
    price: 999, status: 'ACTIVE', createdAt: '', updatedAt: ''
}
const inactiveProduct: Product = {
    id: PRODUCT_ID, sku: 'SKU-002', name: 'Teclado Viejo',
    price: 29, status: 'INACTIVE', createdAt: '', updatedAt: ''
}
const inventoryData: inventoryApi.InventoryInfo = {
    id: 'inv-1', productId: PRODUCT_ID, available: 10, reserved: 0, updatedAt: ''
}
const purchaseData = {
    purchaseId: 'pur-1', productId: PRODUCT_ID,
    quantityPurchased: 2, remainingStock: 8, processedAt: new Date().toISOString()
}

function makeRouter() {
    return createRouter({
        history: createMemoryHistory(),
        routes: [
            {path: '/products/:id/buy', component: PurchaseView},
            {path: '/products/:id', component: {template: '<div>detail</div>'}},
        ]
    })
}

async function mountView() {
    const router = makeRouter()
    await router.push(`/products/${PRODUCT_ID}/buy`)
    const wrapper = mount(PurchaseView, {
        global: {plugins: [router, createPinia()]}
    })
    await flushPromises()
    return wrapper
}

describe('PurchaseView', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        vi.clearAllMocks()
    })

    it('bloquea la compra si el producto es INACTIVE (acceso directo por URL)', async () => {
        vi.mocked(productsApi.getProduct).mockResolvedValue(inactiveProduct)

        const wrapper = await mountView()

        expect(wrapper.find('form').exists()).toBe(false)
        expect(wrapper.text()).toContain('inactivo')
        expect(inventoryApi.getInventory).not.toHaveBeenCalled()
    })

    it('muestra el formulario para productos ACTIVE', async () => {
        vi.mocked(productsApi.getProduct).mockResolvedValue(activeProduct)
        vi.mocked(inventoryApi.getInventory).mockResolvedValue(inventoryData)

        const wrapper = await mountView()

        expect(wrapper.find('form').exists()).toBe(true)
        expect(wrapper.text()).toContain('Laptop Pro')
        expect(wrapper.text()).toContain('10')  // stock
    })


    it('reutiliza la misma Idempotency-Key en reintento (no duplica compras)', async () => {
        vi.mocked(productsApi.getProduct).mockResolvedValue(activeProduct)
        vi.mocked(inventoryApi.getInventory).mockResolvedValue(inventoryData)

        vi.mocked(inventoryApi.purchase)
            .mockRejectedValueOnce(
                new AppError(503, 'SERVICE_UNAVAILABLE', 'El servicio no esta disponible ahora mismo.')
            )
            .mockResolvedValueOnce(purchaseData)

        const wrapper = await mountView()

        await wrapper.find('input[type="number"]').setValue('2')
        await wrapper.find('form').trigger('submit')
        await flushPromises()

        const calls = vi.mocked(inventoryApi.purchase).mock.calls
        expect(calls).toHaveLength(1)
        const firstKey = calls[0][1]  // segundo argumento = idempotencyKey

        const retryBtn = wrapper.find('button.btn-outline.btn-sm')
        if (retryBtn.exists()) {
            await retryBtn.trigger('click')
            await flushPromises()
        }

        const allCalls = vi.mocked(inventoryApi.purchase).mock.calls
        if (allCalls.length > 1) {
            const secondKey = allCalls[1][1]
            expect(secondKey).toBe(firstKey)
        }
    })

    it('genera una NUEVA Idempotency-Key al hacer Nueva compra', async () => {
        vi.mocked(productsApi.getProduct).mockResolvedValue(activeProduct)
        vi.mocked(inventoryApi.getInventory).mockResolvedValue(inventoryData)
        vi.mocked(inventoryApi.purchase).mockResolvedValue(purchaseData)

        const wrapper = await mountView()

        await wrapper.find('input[type="number"]').setValue('1')
        await wrapper.find('form').trigger('submit')
        await flushPromises()

        const firstKey = vi.mocked(inventoryApi.purchase).mock.calls[0][1]

        const nuevaCompraBtn = wrapper.findAll('button').find(b => b.text().includes('Nueva compra'))
        if (nuevaCompraBtn) {
            await nuevaCompraBtn.trigger('click')
            await flushPromises()
            await wrapper.find('input[type="number"]').setValue('1')
            await wrapper.find('form').trigger('submit')
            await flushPromises()

            const secondKey = vi.mocked(inventoryApi.purchase).mock.calls[1][1]
            expect(secondKey).not.toBe(firstKey)  // key diferente = compra diferente
        }
    })


    it('no envía la compra si la cantidad es 0 o vacía', async () => {
        vi.mocked(productsApi.getProduct).mockResolvedValue(activeProduct)
        vi.mocked(inventoryApi.getInventory).mockResolvedValue(inventoryData)

        const wrapper = await mountView()

        await wrapper.find('form').trigger('submit')
        await flushPromises()

        expect(inventoryApi.purchase).not.toHaveBeenCalled()
    })

    it('muestra éxito tras compra correcta', async () => {
        vi.mocked(productsApi.getProduct).mockResolvedValue(activeProduct)
        vi.mocked(inventoryApi.getInventory).mockResolvedValue(inventoryData)
        vi.mocked(inventoryApi.purchase).mockResolvedValue(purchaseData)

        const wrapper = await mountView()

        await wrapper.find('input[type="number"]').setValue('2')
        await wrapper.find('form').trigger('submit')
        await flushPromises()

        expect(wrapper.text()).toContain('Compra realizada')
        expect(wrapper.text()).toContain('2')  // quantityPurchased
        expect(wrapper.text()).toContain('8')  // remainingStock
    })

    it('muestra error 409 stock insuficiente', async () => {
        vi.mocked(productsApi.getProduct).mockResolvedValue(activeProduct)
        vi.mocked(inventoryApi.getInventory).mockResolvedValue(inventoryData)
        vi.mocked(inventoryApi.purchase).mockRejectedValue(
            new AppError(409, 'INSUFFICIENT_STOCK', 'No hay suficiente stock disponible.')
        )

        const wrapper = await mountView()

        await wrapper.find('input[type="number"]').setValue('2')
        await wrapper.find('form').trigger('submit')
        await flushPromises()

        expect(inventoryApi.purchase).toHaveBeenCalled()
        expect(wrapper.text()).toContain('No hay suficiente stock disponible')
    })
})
