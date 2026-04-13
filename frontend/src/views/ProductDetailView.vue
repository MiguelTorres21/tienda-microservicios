<template>
  <div class="page">
    <router-link to="/products" class="back-link">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
           stroke-linecap="round">
        <line x1="19" y1="12" x2="5" y2="12"/>
        <polyline points="12 19 5 12 12 5"/>
      </svg>
      Volver al listado
    </router-link>

    <div v-if="loadingProduct" class="state-loading">
      <span class="spinner spinner-lg"></span>
      <span>Cargando producto...</span>
    </div>

    <div v-else-if="productError" class="alert alert-error flex-between">
      <span>{{ productError }}</span>
      <button class="btn-outline btn-sm" @click="loadProduct">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
             stroke-linecap="round">
          <polyline points="23 4 23 10 17 10"/>
          <polyline points="1 20 1 14 7 14"/>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
        </svg>
        Reintentar
      </button>
    </div>

    <template v-else-if="product">
      <div class="flex-between mb-20">
        <div>
          <h1 style="font-size:24px;font-weight:800;letter-spacing:-.025em">{{ product.name }}</h1>
          <p class="text-sm text-muted" style="margin-top:4px">{{ product.sku }}</p>
        </div>
        <span :class="product.status === 'ACTIVE' ? 'badge badge-active' : 'badge badge-inactive'">
          <span class="badge-dot"></span>
          {{ product.status === 'ACTIVE' ? 'Activo' : 'Inactivo' }}
        </span>
      </div>

      <div class="detail-grid">

        <div class="card" style="padding:0">
          <div style="padding:18px 24px;border-bottom:1px solid var(--border)">
            <h3 style="font-size:13px;font-weight:600;color:var(--text-muted);text-transform:uppercase;letter-spacing:.05em">
              Información del producto</h3>
          </div>
          <table class="info-table">
            <tbody>
            <tr>
              <td>SKU</td>
              <td><span class="font-mono"
                        style="background:var(--surface2);padding:3px 8px;border-radius:4px;border:1px solid var(--border);font-size:13px">{{
                  product.sku
                }}</span></td>
            </tr>
            <tr>
              <td>Nombre</td>
              <td style="font-weight:600">{{ product.name }}</td>
            </tr>
            <tr>
              <td>Precio</td>
              <td><span style="font-size:18px;font-weight:800;color:var(--brand)">${{
                  Number(product.price).toFixed(2)
                }}</span></td>
            </tr>
            <tr>
              <td>Estado</td>
              <td>
                <span :class="product.status === 'ACTIVE' ? 'badge badge-active' : 'badge badge-inactive'">
                  <span class="badge-dot"></span>
                  {{ product.status === 'ACTIVE' ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
            </tr>
            <tr>
              <td>Creado</td>
              <td class="text-sec text-sm">{{ formatDate(product.createdAt) }}</td>
            </tr>
            <tr>
              <td>Actualizado</td>
              <td class="text-sec text-sm">{{ formatDate(product.updatedAt) }}</td>
            </tr>
            </tbody>
          </table>
        </div>

        <div>
          <div class="card" style="padding:0;margin-bottom:16px">
            <div
                style="padding:16px 20px;border-bottom:1px solid var(--border);display:flex;align-items:center;justify-content:space-between">
              <h3 style="font-size:13px;font-weight:600;color:var(--text-muted);text-transform:uppercase;letter-spacing:.05em">
                Inventario</h3>
              <button class="btn-outline btn-sm" @click="loadInventory" :disabled="loadingInventory"
                      title="Actualizar inventario">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                     stroke-linecap="round" :style="loadingInventory ? 'animation:spin .6s linear infinite' : ''">
                  <polyline points="23 4 23 10 17 10"/>
                  <polyline points="1 20 1 14 7 14"/>
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                </svg>
                Actualizar
              </button>
            </div>

            <div v-if="loadingInventory" class="state-loading" style="padding:32px">
              <span class="spinner"></span>
            </div>

            <div v-else-if="inventoryError" style="padding:16px">
              <div class="alert alert-error" style="margin-bottom:8px">{{ inventoryError }}</div>
              <button class="btn-outline btn-sm" @click="loadInventory">Reintentar</button>
            </div>

            <div v-else-if="inventory" class="inv-widget">
              <div class="inv-num">{{ inventory.available }}</div>
              <div class="inv-label">unidades disponibles</div>
              <div v-if="inventory.reserved > 0" class="inv-reserved">
                {{ inventory.reserved }} en reserva
              </div>
            </div>
          </div>

          <router-link v-if="product.status === 'ACTIVE'" :to="`/products/${product.id}/buy`" style="display:block">
            <button class="btn-primary btn-lg w-full">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                   stroke-linecap="round">
                <circle cx="9" cy="21" r="1"/>
                <circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
              Realizar compra
            </button>
          </router-link>

          <div v-else
               style="padding:14px 16px;background:var(--surface2);border:1px solid var(--border);border-radius:var(--radius-sm);text-align:center">
            <svg style="margin:0 auto 8px;display:block;opacity:.4" width="20" height="20" viewBox="0 0 24 24"
                 fill="none" stroke="var(--text-muted)" stroke-width="1.5" stroke-linecap="round">
              <circle cx="12" cy="12" r="10"/>
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
            </svg>
            <p class="text-sm text-muted">Producto inactivo — no disponible para compra</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import {getProduct} from '@/api/products'
import {getInventory, type InventoryInfo} from '@/api/inventory'
import {userMessage} from '@/api/client'
import type {Product} from '@/types/api'

const route = useRoute()
const id = route.params.id as string

const product = ref<Product | null>(null)
const loadingProduct = ref(false)
const productError = ref('')

const inventory = ref<InventoryInfo | null>(null)
const loadingInventory = ref(false)
const inventoryError = ref('')

async function loadProduct() {
  loadingProduct.value = true;
  productError.value = ''
  try {
    product.value = await getProduct(id)
    await loadInventory()
  } catch (err) {
    productError.value = userMessage(err)
  } finally {
    loadingProduct.value = false
  }
}

async function loadInventory() {
  loadingInventory.value = true;
  inventoryError.value = ''
  try {
    inventory.value = await getInventory(id)
  } catch (err) {
    inventoryError.value = userMessage(err)
  } finally {
    loadingInventory.value = false
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('es-CO', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(loadProduct)
</script>
