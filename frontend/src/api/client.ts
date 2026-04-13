import axios, {type AxiosInstance, type AxiosError} from 'axios'
import type {ApiErrorEnvelope} from '@/types/api'

const PRODUCTS_URL = import.meta.env.VITE_PRODUCTS_API_URL ?? 'http://localhost:5173'
const INVENTORY_URL = import.meta.env.VITE_INVENTORY_API_URL ?? 'http://localhost:5173'

export class AppError extends Error {
    constructor(
        public readonly httpStatus: number,
        public readonly code: string,
        public readonly detail: string,
        public readonly correlationId: string = ''
    ) {
        super(detail)
        this.name = 'AppError'
    }
}

export function userMessage(err: unknown): string {
    if (err instanceof AppError) {
        switch (err.code) {
            case 'INSUFFICIENT_STOCK':
                return 'No hay suficiente stock disponible.'
            case 'PRODUCT_NOT_FOUND':
                return 'El producto no existe.'
            case 'SKU_ALREADY_EXISTS':
                return 'Ya existe un producto con ese SKU.'
            case 'INVALID_CREDENTIALS':
                return 'Usuario o contraseña incorrectos.'
            case 'UNAUTHORIZED':
                return 'Sesión expirada. Vuelve a iniciar sesión.'
            case 'MISSING_IDEMPOTENCY_KEY':
                return 'Error interno: falta clave de idempotencia.'
            case 'PURCHASE_IN_PROGRESS':
                return 'Esta compra ya está en procesamiento.'
            case 'CONCURRENT_CONFLICT':
                return 'Conflicto de concurrencia. Intenta de nuevo.'
            case 'SERVICE_UNAVAILABLE':
            case 'CIRCUIT_BREAKER_OPEN':
                return 'El servicio no está disponible ahora mismo. Intenta en unos segundos.'
            case 'NETWORK_ERROR':
                return 'No se pudo conectar con el servidor. Verifica que el backend esté corriendo.'
            case 'VALIDATION_ERROR':
                return err.detail
            default:
                return err.detail || 'Error inesperado.'
        }
    }
    if (err instanceof Error) {
        if (err.message.includes('Network Error') || err.message.includes('timeout')) {
            return 'No se pudo conectar con el servidor. Verifica que el backend esté corriendo en localhost:8081 / 8082.'
        }
    }
    return 'Error inesperado. Intenta de nuevo.'
}

function makeClient(baseURL: string): AxiosInstance {
    const client = axios.create({baseURL, timeout: 8000})

    client.interceptors.request.use(config => {
        const token = sessionStorage.getItem('jwt_token')
        if (token) config.headers.Authorization = `Bearer ${token}`
        return config
    })

    client.interceptors.response.use(
        res => res,
        (error: AxiosError<ApiErrorEnvelope>) => {
            if (!error.response) {
                throw new AppError(
                    0,
                    'NETWORK_ERROR',
                    error.message || 'Sin respuesta del servidor.'
                )
            }

            const status = error.response.status
            const envelope = error.response.data
            const firstErr = envelope?.errors?.[0]

            if (status === 401 && firstErr) {
                throw new AppError(status, firstErr.code, firstErr.detail,
                    firstErr.meta?.correlationId ?? '')
            }

            if (status === 401) {
                sessionStorage.removeItem('jwt_token')
                window.location.href = '/login'
                throw new AppError(401, 'UNAUTHORIZED', 'Sesión expirada.')
            }

            if (firstErr) {
                throw new AppError(status, firstErr.code, firstErr.detail,
                    firstErr.meta?.correlationId ?? '')
            }

            throw new AppError(status, 'UNKNOWN_ERROR', `Error HTTP ${status}`)
        }
    )

    return client
}

export const productsClient = makeClient(PRODUCTS_URL)
export const inventoryClient = makeClient(INVENTORY_URL)