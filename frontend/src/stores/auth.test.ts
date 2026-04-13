import {describe, it, expect, vi, beforeEach} from 'vitest'
import {setActivePinia, createPinia} from 'pinia'
import {useAuthStore} from '@/stores/auth'
import * as productsApi from '@/api/products'
import {AppError} from '@/api/client'

vi.mock('@/api/products')

describe('useAuthStore', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        sessionStorage.clear()
        vi.clearAllMocks()
    })

    it('arranca sin autenticación', () => {
        const auth = useAuthStore()
        expect(auth.isAuthenticated).toBe(false)
        expect(auth.token).toBeNull()
    })

    it('login exitoso guarda el token', async () => {
        vi.mocked(productsApi.login).mockResolvedValue('jwt-token-test')

        const auth = useAuthStore()
        await auth.login('admin', 'admin123')

        expect(auth.isAuthenticated).toBe(true)
        expect(auth.token).toBe('jwt-token-test')
        expect(sessionStorage.getItem('jwt_token')).toBe('jwt-token-test')
    })

    it('login fallido propaga el error', async () => {
        vi.mocked(productsApi.login).mockRejectedValue(
            new AppError(401, 'INVALID_CREDENTIALS', 'Usuario o contraseña incorrectos.')
        )

        const auth = useAuthStore()
        await expect(auth.login('admin', 'wrongpass')).rejects.toThrow('Usuario o contraseña incorrectos.')
        expect(auth.isAuthenticated).toBe(false)
    })

    it('logout borra el token y sessionStorage', async () => {
        vi.mocked(productsApi.login).mockResolvedValue('jwt-token-test')
        const auth = useAuthStore()
        await auth.login('admin', 'admin123')

        auth.logout()

        expect(auth.isAuthenticated).toBe(false)
        expect(sessionStorage.getItem('jwt_token')).toBeNull()
    })
})
