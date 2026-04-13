<template>
  <div class="page">

    <div class="page-hd">
      <div>
        <h1 class="page-title">Productos</h1>
        <p class="page-sub">Gestiona el catálogo de la tienda</p>
      </div>
      <button class="btn-primary" @click="openCreateModal">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
             stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        Nuevo producto
      </button>
    </div>

    <div class="filter-bar">
      <div>
        <label>Buscar</label>
        <div class="input-wrap">
          <span class="input-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                 stroke-linecap="round">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
          </span>
          <input v-model="filters.search" placeholder="SKU o nombre..." @input="debouncedFetch"/>
        </div>
      </div>
      <div>
        <label>Estado</label>
        <select v-model="filters.status" @change="resetAndFetch">
          <option value="">Todos</option>
          <option value="ACTIVE">Activo</option>
          <option value="INACTIVE">Inactivo</option>
        </select>
      </div>
      <div>
        <label>Ordenar por</label>
        <select v-model="filters.sortBy" @change="resetAndFetch">
          <option value="createdAt">Fecha de creación</option>
          <option value="price">Precio</option>
        </select>
      </div>
      <div>
        <label>Dirección</label>
        <select v-model="filters.sortDir" @change="resetAndFetch">
          <option value="desc">Mayor a menor</option>
          <option value="asc">Menor a mayor</option>
        </select>
      </div>
    </div>

    <div v-if="error" class="alert alert-error flex-between" style="margin-bottom:12px">
      <span>{{ error }}</span>
      <button class="btn-outline btn-sm" @click="doFetch">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
             stroke-linecap="round">
          <polyline points="23 4 23 10 17 10"/>
          <polyline points="1 20 1 14 7 14"/>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
        </svg>
        Reintentar
      </button>
    </div>

    <div v-if="loading && !currentPage" class="card" style="text-align:center;padding:56px">
      <span class="spinner spinner-lg"></span>
      <p style="margin-top:16px;font-size:13px;color:var(--text-muted)">Cargando productos...</p>
    </div>

    <div v-else-if="currentPage" class="card card-flush">
      <div v-if="loading"
           style="padding:8px 18px;font-size:12px;color:var(--text-muted);background:var(--surface2);border-bottom:1px solid var(--border)">
        Actualizando...
      </div>

      <div v-if="currentPage.products.length === 0" class="empty">
        <svg class="empty-icon" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="1" stroke-linecap="round">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
          <line x1="8" y1="21" x2="16" y2="21"/>
          <line x1="12" y1="17" x2="12" y2="21"/>
        </svg>
        <p class="empty-title">Sin resultados</p>
        <p class="empty-desc">No hay productos para los filtros seleccionados.</p>
      </div>

      <div v-else style="overflow-x:auto">
        <table>
          <thead>
          <tr>
            <th>SKU</th>
            <th>Nombre</th>
            <th>Precio</th>
            <th>Estado</th>
            <th style="width:120px">Acciones</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="p in currentPage.products" :key="p.id" class="tr-link" @click="router.push(`/products/${p.id}`)">
            <td>
              <span class="font-mono"
                    style="font-size:12px;background:var(--surface2);padding:3px 8px;border-radius:4px;border:1px solid var(--border)">{{
                  p.sku
                }}</span>
            </td>
            <td>
              <span style="font-weight:600;color:var(--text)">{{ p.name }}</span>
            </td>
            <td style="font-weight:600">${{ Number(p.price).toFixed(2) }}</td>
            <td>
                <span :class="p.status === 'ACTIVE' ? 'badge badge-active' : 'badge badge-inactive'">
                  <span class="badge-dot"></span>
                  {{ p.status === 'ACTIVE' ? 'Activo' : 'Inactivo' }}
                </span>
            </td>
            <td>
              <div class="row-actions">
                <button class="btn-outline btn-sm" @click.stop="openEditModal(p)" title="Editar">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                       stroke-linecap="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                  Editar
                </button>
                <button class="btn-danger btn-sm" @click.stop="confirmDelete(p)" title="Eliminar">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                       stroke-linecap="round">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                  </svg>
                  Eliminar
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div v-if="currentPage.totalPages > 1" class="pagination">
        <button class="btn-outline btn-sm" :disabled="filters.page === 0" @click="changePage(-1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
               stroke-linecap="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="pag-info">
          Página <strong>{{ (filters.page ?? 0) + 1 }}</strong> de <strong>{{ currentPage.totalPages }}</strong>
          &nbsp;·&nbsp; {{ currentPage.totalItems }} productos
        </span>
        <button class="btn-outline btn-sm" :disabled="(filters.page ?? 0) >= currentPage.totalPages - 1"
                @click="changePage(1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
               stroke-linecap="round">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <p class="modal-hd">{{ editingProduct ? 'Editar producto' : 'Nuevo producto' }}</p>
        <p class="modal-sub">
          {{ editingProduct ? 'Modifica los campos del producto.' : 'Completa los datos para crear un producto.' }}</p>

        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>

        <form @submit.prevent="handleSave" novalidate>
          <div class="form-group">
            <label>SKU *</label>
            <input v-model.trim="form.sku" placeholder="ej. SKU-001" :class="{ error: formTouched && !form.sku }"/>
            <span v-if="formTouched && !form.sku" class="field-error">Campo obligatorio</span>
          </div>
          <div class="form-group">
            <label>Nombre *</label>
            <input v-model.trim="form.name" placeholder="ej. Laptop Pro 15"
                   :class="{ error: formTouched && !form.name }"/>
            <span v-if="formTouched && !form.name" class="field-error">Campo obligatorio</span>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Precio *</label>
              <input v-model.number="form.price" type="number" min="0" step="0.01" placeholder="0.00"
                     :class="{ error: formTouched && (form.price == null || form.price < 0) }"/>
              <span v-if="formTouched && (form.price == null || form.price < 0)" class="field-error">Debe ser mayor o igual a 0</span>
            </div>
            <div class="form-group">
              <label>Estado</label>
              <select v-model="form.status">
                <option value="ACTIVE">Activo</option>
                <option value="INACTIVE">Inactivo</option>
              </select>
            </div>
          </div>

          <div class="modal-ft">
            <button type="button" class="btn-outline" @click="closeModal">Cancelar</button>
            <button type="submit" class="btn-primary" :disabled="saving">
              <span v-if="saving" class="spinner"></span>
              {{ saving ? 'Guardando...' : 'Guardar producto' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="deletingProduct" class="modal-overlay" @click.self="deletingProduct = null">
      <div class="modal" style="max-width:400px">
        <p class="modal-hd">Eliminar producto</p>
        <p class="modal-sub">
          Esta acción no se puede deshacer. ¿Confirmas la eliminación de
          <strong>{{ deletingProduct.name }}</strong>?
        </p>
        <div v-if="deleteError" class="alert alert-error">{{ deleteError }}</div>
        <div class="modal-ft">
          <button class="btn-outline" @click="deletingProduct = null">Cancelar</button>
          <button class="btn-danger" :disabled="deleting" @click="handleDelete">
            <span v-if="deleting" class="spinner"></span>
            {{ deleting ? 'Eliminando...' : 'Sí, eliminar' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {useProductsStore} from '@/stores/products'
import {createProduct, updateProduct, deleteProduct} from '@/api/products'
import {userMessage} from '@/api/client'
import type {Product, ProductFilters, ProductStatus} from '@/types/api'
import {storeToRefs} from 'pinia'

const store = useProductsStore()
const {currentPage, loading, error} = storeToRefs(store)
const router = useRouter()

const filters = reactive<ProductFilters>({
  search: '', status: '', sortBy: 'createdAt', sortDir: 'desc', page: 0, size: 10
})

let debounceTimer: ReturnType<typeof setTimeout>

function debouncedFetch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    filters.page = 0;
    doFetch()
  }, 350)
}

function resetAndFetch() {
  filters.page = 0;
  doFetch()
}

function changePage(delta: number) {
  filters.page = (filters.page ?? 0) + delta
  doFetch()
}

async function doFetch() {
  await store.fetchProducts({...filters})
}

onMounted(doFetch)

const showModal = ref(false)
const editingProduct = ref<Product | null>(null)
const modalError = ref('')
const saving = ref(false)
const formTouched = ref(false)

const form = reactive<{ sku: string; name: string; price: number | null; status: ProductStatus }>({
  sku: '', name: '', price: null, status: 'ACTIVE'
})

function openCreateModal() {
  editingProduct.value = null
  form.sku = '';
  form.name = '';
  form.price = null;
  form.status = 'ACTIVE'
  formTouched.value = false;
  modalError.value = ''
  showModal.value = true
}

function openEditModal(p: Product) {
  editingProduct.value = p
  form.sku = p.sku;
  form.name = p.name;
  form.price = p.price;
  form.status = p.status
  formTouched.value = false;
  modalError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function handleSave() {
  formTouched.value = true
  if (!form.sku || !form.name || form.price == null || form.price < 0) return

  saving.value = true;
  modalError.value = ''
  try {
    if (editingProduct.value) {
      await updateProduct(editingProduct.value.id, {
        sku: form.sku,
        name: form.name,
        price: form.price!,
        status: form.status
      })
    } else {
      await createProduct({sku: form.sku, name: form.name, price: form.price!, status: form.status})
    }
    closeModal()
    store.invalidateCache()
    await doFetch()
  } catch (err) {
    modalError.value = userMessage(err)
  } finally {
    saving.value = false
  }
}

const deletingProduct = ref<Product | null>(null)
const deleting = ref(false)
const deleteError = ref('')

function confirmDelete(p: Product) {
  deletingProduct.value = p;
  deleteError.value = ''
}

async function handleDelete() {
  if (!deletingProduct.value) return
  deleting.value = true;
  deleteError.value = ''
  try {
    await deleteProduct(deletingProduct.value.id)
    deletingProduct.value = null
    store.invalidateCache()
    await doFetch()
  } catch (err) {
    deleteError.value = userMessage(err)
  } finally {
    deleting.value = false
  }
}
</script>
