import {defineStore} from 'pinia'
import {ref, computed} from 'vue'
import {login as apiLogin} from '@/api/products'

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(sessionStorage.getItem('jwt_token'))

    const isAuthenticated = computed(() => token.value !== null)

    async function login(username: string, password: string): Promise<void> {
        const t = await apiLogin(username, password)
        token.value = t
        sessionStorage.setItem('jwt_token', t)
    }

    function logout(): void {
        token.value = null
        sessionStorage.removeItem('jwt_token')
    }

    return {token, isAuthenticated, login, logout}
})
