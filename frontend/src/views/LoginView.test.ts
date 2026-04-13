import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import * as productsApi from '@/api/products'
import { AppError } from '@/api/client'

vi.mock('@/api/products')

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/products', component: { template: '<div>products</div>' } },
    ]
  })
}

async function mountLoginView() {
  const router = makeRouter()
  await router.push('/login')
  await router.isReady()

  const wrapper = mount(LoginView, {
    global: { plugins: [router, createPinia()] }
  })

  return { wrapper, router }
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  it('muestra el formulario de login', async () => {
    const { wrapper } = await mountLoginView()

    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
    expect(wrapper.find('button[type="submit"]').exists()).toBe(true)
  })

  it('muestra errores de validación si se envía vacío', async () => {
    const { wrapper } = await mountLoginView()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Campo obligatorio')
    expect(productsApi.login).not.toHaveBeenCalled()
  })

  it('llama al login con las credenciales ingresadas', async () => {
    vi.mocked(productsApi.login).mockResolvedValue('fake-jwt-token')

    const { wrapper } = await mountLoginView()

    await wrapper.find('input[type="text"]').setValue('admin')
    await wrapper.find('input[type="password"]').setValue('admin123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(productsApi.login).toHaveBeenCalledWith('admin', 'admin123')
    expect(sessionStorage.getItem('jwt_token')).toBe('fake-jwt-token')
  })

  it('muestra error cuando las credenciales son incorrectas', async () => {
    vi.mocked(productsApi.login).mockRejectedValue(
        new AppError(401, 'INVALID_CREDENTIALS', 'Usuario o contraseña incorrectos.')
    )

    const { wrapper } = await mountLoginView()

    await wrapper.find('input[type="text"]').setValue('admin')
    await wrapper.find('input[type="password"]').setValue('wrong')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.alert-error').exists()).toBe(true)
    expect(wrapper.text()).toContain('incorrectos')
  })

  it('muestra error de conexión cuando el servidor no responde', async () => {
    vi.mocked(productsApi.login).mockRejectedValue(
        new AppError(0, 'NETWORK_ERROR', 'No se pudo conectar con el servidor.')
    )

    const { wrapper } = await mountLoginView()

    await wrapper.find('input[type="text"]').setValue('admin')
    await wrapper.find('input[type="password"]').setValue('admin123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.alert-error').exists()).toBe(true)
    expect(wrapper.text()).toContain('servidor')
  })

  it('deshabilita el botón mientras procesa el login', async () => {
    let resolve!: (v: string) => void
    vi.mocked(productsApi.login).mockReturnValue(new Promise(r => { resolve = r }))

    const { wrapper } = await mountLoginView()

    await wrapper.find('input[type="text"]').setValue('admin')
    await wrapper.find('input[type="password"]').setValue('admin123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const btn = wrapper.find('button[type="submit"]')
    expect(btn.attributes('disabled')).toBeDefined()

    resolve('token')
    await flushPromises()
  })
})