<template>
  <div class="login-page">

    <div class="login-brand">
      <div class="login-brand-logo">
        <div class="login-brand-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
            <line x1="3" y1="6" x2="21" y2="6"/>
            <path d="M16 10a4 4 0 0 1-8 0"/>
          </svg>
        </div>
        <span class="login-brand-name">Tienda Admin</span>
      </div>

      <div class="login-brand-body">
        <h1 class="login-brand-headline">Gestión de<br>inventario y ventas</h1>
        <p class="login-brand-desc">
          Administra tu catálogo, controla el stock y procesa
          ventas de forma eficiente, segura y trazable.
        </p>
        <div class="login-features">
          <div class="login-feature">
            <div class="login-feature-dot">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                   stroke-linecap="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Control de inventario en tiempo real</span>
          </div>
          <div class="login-feature">
            <div class="login-feature-dot">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                   stroke-linecap="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Autenticación segura con JWT</span>
          </div>
          <div class="login-feature">
            <div class="login-feature-dot">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                   stroke-linecap="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Compras idempotentes y auditables</span>
          </div>
        </div>
      </div>

      <div class="login-brand-footer">© 2025 Prueba técnica.</div>
    </div>

    <div class="login-form-side">
      <div class="login-form-inner">

        <div class="login-logo-mobile">
          <div class="login-logo-mobile-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
              <line x1="3" y1="6" x2="21" y2="6"/>
              <path d="M16 10a4 4 0 0 1-8 0"/>
            </svg>
          </div>
          <span class="login-logo-mobile-name">Tienda Admin</span>
        </div>

        <h2 class="login-title">Bienvenido de vuelta</h2>
        <p class="login-subtitle">Ingresa tus credenciales para continuar</p>

        <div v-if="errorMsg" class="alert alert-error" role="alert">
          <svg style="flex-shrink:0;margin-top:1px" width="15" height="15" viewBox="0 0 24 24" fill="none"
               stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          {{ errorMsg }}
        </div>

        <form @submit.prevent="handleLogin" novalidate>
          <div class="form-group">
            <label for="username">Usuario</label>
            <div class="input-wrap">
              <span class="input-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                     stroke-linecap="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                </svg>
              </span>
              <input
                  id="username"
                  v-model.trim="username"
                  type="text"
                  autocomplete="username"
                  placeholder="admin"
                  :class="{ error: touched && !username }"
              />
            </div>
            <span v-if="touched && !username" class="field-error">Campo obligatorio</span>
          </div>

          <div class="form-group">
            <label for="password">Contraseña</label>
            <div class="input-wrap">
              <span class="input-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                     stroke-linecap="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
              </span>
              <input
                  id="password"
                  v-model="password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="••••••••"
                  :class="{ error: touched && !password }"
              />
            </div>
            <span v-if="touched && !password" class="field-error">Campo obligatorio</span>
          </div>

          <button type="submit" class="btn-primary btn-lg w-full" :disabled="loading" style="margin-top:4px">
            <span v-if="loading" class="spinner"></span>
            {{ loading ? 'Verificando...' : 'Iniciar sesión' }}
          </button>
        </form>

        <div class="login-hint">
          <strong>Credenciales de prueba:</strong><br>
          usuario: <code>admin</code> &nbsp; contraseña: <code>admin123</code>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {userMessage} from '@/api/client'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const touched = ref(false)

async function handleLogin() {
  touched.value = true
  errorMsg.value = ''

  if (!username.value || !password.value) return

  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const redirect = (route.query.redirect as string) || '/products'
    router.push(redirect)
  } catch (err) {
    errorMsg.value = userMessage(err)
  } finally {
    loading.value = false
  }
}
</script>
