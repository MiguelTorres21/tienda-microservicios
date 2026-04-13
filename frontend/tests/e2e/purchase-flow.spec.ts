import {test, expect, type Page} from '@playwright/test'

const BASE = 'http://localhost:5173'

const ACTIVE_SKU = 'SKU-002'
const ACTIVE_NAME = 'Mouse Inalámbrico'
const INACTIVE_UUID = 'a1b2c3d4-0005-0005-0005-000000000005'

async function login(page: Page) {
    await page.goto(`${BASE}/login`)
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button[type="submit"]')
    await expect(page).toHaveURL(/\/products/, {timeout: 8000})
}

async function waitForTable(page: Page) {
    await expect(page.locator('table tbody tr').first()).toBeVisible({timeout: 8000})
}

function rowBySku(page: Page, sku: string) {
    return page.locator('tbody tr').filter({hasText: sku})
}

test.describe('Autenticación', () => {
    test('login correcto redirige al listado', async ({page}) => {
        await login(page)
        await expect(page.locator('h1')).toContainText('Productos')
    })

    test('credenciales incorrectas muestran error 401', async ({page}) => {
        await page.goto(`${BASE}/login`)
        await page.fill('input[type="text"]', 'admin')
        await page.fill('input[type="password"]', 'wrongpassword')
        await page.click('button[type="submit"]')

        await expect(page.locator('.alert-error')).toBeVisible({timeout: 5000})
        await expect(page.locator('.alert-error')).toContainText(/incorrectos/i)
        await expect(page).toHaveURL(/\/login/)
    })

    test('ruta protegida sin token redirige a login', async ({page}) => {
        await page.goto(`${BASE}/products`)
        await expect(page).toHaveURL(/\/login/)
    })
})

test.describe('Listado de productos', () => {
    test.beforeEach(({page}) => login(page))

    test('muestra al menos un producto en la tabla', async ({page}) => {
        await waitForTable(page)
        const count = await page.locator('tbody tr').count()
        expect(count).toBeGreaterThan(0)
    })

    test('SKU-002 está visible en el listado', async ({page}) => {
        await waitForTable(page)
        await expect(rowBySku(page, ACTIVE_SKU)).toBeVisible()
        await expect(rowBySku(page, ACTIVE_SKU)).toContainText(ACTIVE_NAME)
    })

    test('filtro por ACTIVE no muestra INACTIVE (SKU-005)', async ({page}) => {
        await waitForTable(page)
        await page.locator('select').nth(0).selectOption('ACTIVE')
        await page.waitForTimeout(500)
        const inactiveRow = page.locator('tbody tr').filter({hasText: 'SKU-005'})
        await expect(inactiveRow).toHaveCount(0)
        expect(await page.locator('tbody tr').count()).toBeGreaterThan(0)
    })

    test('búsqueda por SKU filtra correctamente', async ({page}) => {
        await waitForTable(page)
        await page.fill('input[placeholder*="SKU"]', ACTIVE_SKU)
        await page.waitForTimeout(500)

        const rows = page.locator('tbody tr')
        expect(await rows.count()).toBeGreaterThan(0)
        await expect(rows.filter({hasText: ACTIVE_SKU})).toBeVisible()
    })

    test('búsqueda sin resultados muestra estado vacío', async ({page}) => {
        await waitForTable(page)
        await page.fill('input[placeholder*="SKU"]', 'PRODUCTO-QUE-NO-EXISTE-XYZ')
        await page.waitForTimeout(500)
        await expect(page.locator('text=No hay productos')).toBeVisible({timeout: 5000})
    })
})

test.describe('Detalle de producto', () => {
    test.beforeEach(({page}) => login(page))

    test('muestra datos del producto y su inventario', async ({page}) => {
        await waitForTable(page)
        await rowBySku(page, ACTIVE_SKU).click()
        await expect(page).toHaveURL(/\/products\/[a-f0-9-]+$/, {timeout: 5000})

        await expect(page.locator('h1')).toContainText(ACTIVE_NAME)

        await expect(page.locator('text=unidades disponibles')).toBeVisible({timeout: 8000})
    })

    test('producto INACTIVE muestra badge inactivo y no muestra botón Comprar', async ({page}) => {
        await waitForTable(page)
        await page.locator('select').nth(0).selectOption('INACTIVE')
        await page.waitForTimeout(500)

        const row = page.locator('tbody tr').filter({hasText: 'SKU-005'})
        await expect(row).toBeVisible({timeout: 5000})
        await row.click()

        await expect(page).toHaveURL(/\/products\/[a-f0-9-]+$/)
        await expect(page.getByText('Inactivo').first()).toBeVisible()
        await expect(page.locator('a[href*="/buy"]')).toHaveCount(0)
    })
})

test.describe('Flujo de compra', () => {
    test.beforeEach(({page}) => login(page))

    test('compra exitosa con SKU-002 (stock >= 200)', async ({page}) => {
        await page.goto(`${BASE}/products/a1b2c3d4-0002-0002-0002-000000000002/buy`)

        await expect(page.locator('text=Stock disponible')).toBeVisible({timeout: 8000})
        await expect(page.locator('text=unidades')).toBeVisible()

        await page.fill('input[type="number"]', '1')
        await page.click('button[type="submit"]')

        await expect(page.locator('text=Compra realizada')).toBeVisible({timeout: 10000})
        await expect(page.locator('text=Unidades compradas')).toBeVisible()
    })

    test('compra con cantidad mayor al stock muestra error de stock', async ({page}) => {
        await page.goto(`${BASE}/products/a1b2c3d4-0002-0002-0002-000000000002/buy`)
        await expect(page.locator('text=Stock disponible')).toBeVisible({timeout: 8000})

        await page.fill('input[type="number"]', '999999')
        await page.click('button[type="submit"]')

        await expect(
            page.locator('.field-error').filter({hasText: /suficiente stock|disponible/i})
        ).toBeVisible({timeout: 10000})
    })

    test('producto INACTIVE bloqueado aunque se acceda directo por URL /buy', async ({page}) => {
        await page.goto(`${BASE}/products/${INACTIVE_UUID}/buy`)

        await expect(page.locator('form')).toHaveCount(0, {timeout: 5000})

        await expect(page.locator('text=inactivo')).toBeVisible({timeout: 5000})
    })

    test('nueva compra genera nueva Idempotency-Key (botón "Nueva compra")', async ({page}) => {
        await page.goto(`${BASE}/products/a1b2c3d4-0002-0002-0002-000000000002/buy`)
        await expect(page.locator('text=Stock disponible')).toBeVisible({timeout: 8000})

        await page.fill('input[type="number"]', '1')
        await page.click('button[type="submit"]')
        await expect(page.locator('text=Compra realizada')).toBeVisible({timeout: 10000})

        const firstId = await page.locator('td').filter({hasText: /^[a-f0-9-]{36}$/}).first().textContent()

        await page.click('text=Nueva compra')
        await expect(page.locator('form')).toBeVisible()

        await page.fill('input[type="number"]', '1')
        await page.click('button[type="submit"]')
        await expect(page.locator('text=Compra realizada')).toBeVisible({timeout: 10000})

        const secondId = await page.locator('td').filter({hasText: /^[a-f0-9-]{36}$/}).first().textContent()
        expect(firstId).not.toBe(secondId)
    })
})
