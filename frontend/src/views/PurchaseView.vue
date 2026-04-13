<template>
  <div class="page">
    <div class="purchase-wrap">
      <router-link :to="`/products/${productId}`" class="back-link">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
             stroke-linecap="round">
          <line x1="19" y1="12" x2="5" y2="12"/>
          <polyline points="12 19 5 12 12 5"/>
        </svg>
        Volver al producto
      </router-link>

      <div v-if="loadingProduct" class="state-loading">
        <span class="spinner spinner-lg"></span>
        <span>Cargando...</span>
      </div>

      <div v-else-if="loadProductError" class="alert alert-error flex-between">
        <span>{{ loadProductError }}</span>
        <button class="btn-outline btn-sm" @click="init">Reintentar</button>
      </div>

      <div v-else-if="product && product.status === 'INACTIVE'" class="card blocked-card">
        <svg class="blocked-icon" width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="1" stroke-linecap="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
        </svg>
        <h2 style="font-size:17px;font-weight:700;margin-bottom:8px;color:var(--text)">Producto no disponible</h2>
        <p class="text-sm text-muted" style="margin-bottom:20px;max-width:280px;margin-left:auto;margin-right:auto">
          <strong>{{ product.name }}</strong> está inactivo y no puede comprarse en este momento.
        </p>
        <router-link :to="`/products/${productId}`">
          <button class="btn-outline">Ver detalle del producto</button>
        </router-link>
      </div>

      <template v-else-if="product">
        <div style="margin-bottom:20px">
          <h1 style="font-size:22px;font-weight:800;letter-spacing:-.025em;margin-bottom:4px">Realizar compra</h1>
          <p class="text-sm text-muted">
            {{ product.name }}
            <span style="margin:0 6px;opacity:.4">·</span>
            <strong style="color:var(--brand)">${{ Number(product.price).toFixed(2) }}</strong>
          </p>
        </div>

        <div class="stock-panel">
          <div>
            <div
                style="font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.05em;color:var(--text-muted);margin-bottom:6px">
              Stock disponible
            </div>
            <div v-if="loadingInventory" class="spinner" style="margin-top:4px"></div>
            <div v-else-if="inventory">
              <span class="stock-number">{{ inventory.available }}</span>
              <span class="stock-label" style="margin-left:8px">unidades</span>
            </div>
            <span v-else class="text-sm" style="color:var(--warning)">Sin datos de stock</span>
          </div>
          <button class="btn-outline btn-sm" @click="reloadInventory" :disabled="loadingInventory"
                  title="Actualizar stock">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                 stroke-linecap="round">
              <polyline points="23 4 23 10 17 10"/>
              <polyline points="1 20 1 14 7 14"/>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
            </svg>
            Actualizar
          </button>
        </div>
        <div v-if="inventoryError" class="alert alert-warn" style="margin-top:-8px;margin-bottom:16px">
          {{ inventoryError }}
          <button class="btn-outline btn-sm" style="margin-top:8px;display:block" @click="reloadInventory">Reintentar
          </button>
        </div>

        <div v-if="purchaseResult" class="success-card">
          <div class="success-hd">
            <div class="success-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                   stroke-linecap="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <div>
              <p class="success-title">Compra realizada</p>
              <p class="success-sub">La transacción fue procesada correctamente</p>
            </div>
          </div>

          <table class="success-table" style="width:100%">
            <tbody>
            <tr>
              <td>Unidades compradas</td>
              <td>{{ purchaseResult.quantityPurchased }}</td>
            </tr>
            <tr>
              <td>Stock restante</td>
              <td>{{ purchaseResult.remainingStock }}</td>
            </tr>
            <tr>
              <td>ID de compra</td>
              <td><span class="font-mono" style="font-size:11px">{{ purchaseResult.purchaseId }}</span></td>
            </tr>
            <tr>
              <td>Procesado</td>
              <td>{{ formatDate(purchaseResult.processedAt) }}</td>
            </tr>
            </tbody>
          </table>

          <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:20px">
            <button class="btn-outline" @click="resetPurchase">Nueva compra</button>
            <router-link :to="`/products/${productId}`">
              <button class="btn-primary">Volver al producto</button>
            </router-link>
          </div>
        </div>

        <div v-else class="card">
          <div v-if="purchaseError" :class="['alert', purchaseErrorClass]" style="margin-bottom:16px">
            <div>{{ purchaseError }}</div>
            <button v-if="canRetry" class="btn-outline btn-sm" style="margin-top:10px;display:inline-flex"
                    @click="submitPurchase" :disabled="purchasing">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                   stroke-linecap="round">
                <polyline points="23 4 23 10 17 10"/>
                <polyline points="1 20 1 14 7 14"/>
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
              </svg>
              Reintentar compra
            </button>
          </div>

          <form @submit.prevent="submitPurchase" novalidate>
            <div class="form-group">
              <label for="qty" style="font-size:14px">Cantidad a comprar</label>
              <input
                  id="qty"
                  v-model.number="quantity"
                  type="number" min="1"
                  :max="inventory?.available ?? 9999"
                  placeholder="0"
                  class="qty-input"
                  :class="{ error: formTouched && !validQuantity }"
              />
              <span v-if="formTouched && quantity != null && quantity < 1" class="field-error">La cantidad debe ser al menos 1</span>
              <span v-else-if="formTouched && exceedsStock" class="field-error">
                Stock insuficiente (disponible: {{ inventory?.available }})
              </span>
              <span v-else-if="formTouched && !quantity" class="field-error">Campo obligatorio</span>
            </div>

            <div v-if="quantity && quantity > 0 && inventory" class="summary-box">
              <div class="summary-row">
                <span>Precio unitario</span>
                <span>${{ Number(product.price).toFixed(2) }}</span>
              </div>
              <div class="summary-row">
                <span>Cantidad</span>
                <span>{{ quantity }} unidades</span>
              </div>
              <div class="summary-row summary-total">
                <span>Total estimado</span>
                <span>${{ (Number(product.price) * quantity).toFixed(2) }}</span>
              </div>
            </div>

            <div v-if="inventory && inventory.available === 0" class="alert alert-warn"
                 style="margin-bottom:12px;text-align:center">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                   stroke-linecap="round" style="display:inline;margin-right:4px">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              Sin stock disponible
            </div>

            <button
                type="submit"
                class="btn-primary btn-lg w-full"
                :disabled="purchasing || !inventory || inventory.available === 0"
            >
              <span v-if="purchasing" class="spinner"></span>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.5" stroke-linecap="round">
                <circle cx="9" cy="21" r="1"/>
                <circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
              {{ purchasing ? 'Procesando compra...' : 'Confirmar compra' }}
            </button>
          </form>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import {v4 as uuidv4} from 'uuid'
import {getProduct} from '@/api/products'
import {getInventory, purchase, type InventoryInfo} from '@/api/inventory'
import {userMessage, AppError} from '@/api/client'
import type {Product, PurchaseResponse} from '@/types/api'

const route = useRoute()
const productId = route.params.id as string

const product = ref<Product | null>(null)
const loadingProduct = ref(false)
const loadProductError = ref('')

const inventory = ref<InventoryInfo | null>(null)
const loadingInventory = ref(false)
const inventoryError = ref('')

const quantity = ref<number | null>(null)
const formTouched = ref(false)
const purchasing = ref(false)
const purchaseError = ref('')
const purchaseResult = ref<PurchaseResponse | null>(null)

const currentIdempotencyKey = ref<string>(uuidv4())

const purchaseErrorClass = computed(() =>
    canRetry.value ? 'alert alert-warn' : 'alert alert-error'
)

const canRetry = computed(() => {
  if (!purchaseError.value) return false
  return (
      purchaseError.value.includes('disponible ahora') ||
      purchaseError.value.includes('conectar') ||
      purchaseError.value.includes('servidor') ||
      purchaseError.value.includes('segundos')
  )
})

const validQuantity = computed(() => {
  if (!quantity.value || quantity.value < 1) return false
  if (inventory.value && quantity.value > inventory.value.available) return false
  return true
})

const exceedsStock = computed(() => {
  return quantity.value != null &&
      inventory.value != null &&
      quantity.value > inventory.value.available
})

async function init() {
  loadingProduct.value = true
  loadProductError.value = ''
  try {
    product.value = await getProduct(productId)
    if (product.value.status === 'ACTIVE') {
      await reloadInventory()
    }
  } catch (err) {
    loadProductError.value = userMessage(err)
  } finally {
    loadingProduct.value = false
  }
}

async function reloadInventory() {
  loadingInventory.value = true
  inventoryError.value = ''
  try {
    inventory.value = await getInventory(productId)
  } catch (err) {
    inventoryError.value = userMessage(err)
  } finally {
    loadingInventory.value = false
  }
}

async function submitPurchase() {
  formTouched.value = true
  if (!validQuantity.value) return

  purchasing.value = true
  purchaseError.value = ''
  try {
    purchaseResult.value = await purchase(
        {productId, quantity: quantity.value!},
        currentIdempotencyKey.value
    )
    await reloadInventory()
  } catch (err) {
    purchaseError.value = userMessage(err)
    if (err instanceof AppError && err.httpStatus === 409) {
      await reloadInventory()
    }
  } finally {
    purchasing.value = false
  }
}

function resetPurchase() {
  purchaseResult.value = null
  quantity.value = null
  formTouched.value = false
  purchaseError.value = ''
  currentIdempotencyKey.value = uuidv4()
  reloadInventory()
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('es-CO', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(init)
</script>