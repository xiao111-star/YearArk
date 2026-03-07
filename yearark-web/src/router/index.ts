import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  // AuthLayout routes
  {
    path: '/',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/LoginPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/RegisterPage.vue'),
        meta: { guest: true },
      },
    ],
  },
  // AppLayout routes (require auth)
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardPage.vue'),
      },
      {
        path: 'album/create',
        name: 'AlbumCreate',
        component: () => import('@/views/AlbumCreatePage.vue'),
      },
      {
        path: 'album/:id',
        name: 'AlbumDetail',
        component: () => import('@/views/AlbumDetailPage.vue'),
      },
      {
        path: 'album/:id/preview',
        name: 'AlbumPreview',
        component: () => import('@/views/AlbumPreviewPage.vue'),
      },
    ],
  },
  // ShareLayout routes (no auth needed)
  {
    path: '/share/:inviteCode',
    component: () => import('@/layouts/ShareLayout.vue'),
    children: [
      {
        path: '',
        name: 'ShareUpload',
        component: () => import('@/views/ShareUploadPage.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // Load token from localStorage on first navigation
  if (!userStore.isLoggedIn) {
    userStore.loadFromStorage()
  }

  // Routes that require authentication
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    if (!userStore.isLoggedIn) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  // Guest-only routes (login/register): redirect to dashboard if already logged in
  if (to.matched.some((record) => record.meta.guest)) {
    if (userStore.isLoggedIn) {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
