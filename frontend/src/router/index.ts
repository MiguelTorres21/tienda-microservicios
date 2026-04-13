import {createRouter, createWebHistory} from 'vue-router'
import {useAuthStore} from '@/stores/auth'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {path: '/login', name: 'login', component: () => import('@/views/LoginView.vue')},
        {path: '/', redirect: '/products'},
        {
            path: '/products',
            name: 'products',
            component: () => import('@/views/ProductListView.vue'),
            meta: {requiresAuth: true}
        },
        {
            path: '/products/:id',
            name: 'product',
            component: () => import('@/views/ProductDetailView.vue'),
            meta: {requiresAuth: true}
        },
        {
            path: '/products/:id/buy',
            name: 'purchase',
            component: () => import('@/views/PurchaseView.vue'),
            meta: {requiresAuth: true}
        },
        {path: '/:pathMatch(.*)*', redirect: '/products'},
    ]
})

router.beforeEach(to => {
    const auth = useAuthStore()
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return {name: 'login', query: {redirect: to.fullPath}}
    }
})

export default router
