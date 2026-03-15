import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomePage.vue'),
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
        path: 'albums',
        redirect: '/dashboard',
      },
      {
        path: 'templates',
        name: 'Templates',
        component: () => import('@/views/TemplatesPage.vue'),
      },
      {
        path: 'shared',
        name: 'Shared',
        component: () => import('@/views/SharedPage.vue'),
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfilePage.vue'),
      },
      {
        path: 'album/:id/preview',
        name: 'AlbumPreview',
        component: () => import('@/views/AlbumPreviewPage.vue'),
      },
      {
        path: 'album/:id/edit',
        name: 'AlbumEditor',
        component: () => import('@/views/AlbumEditorPage.vue'),
      },
    ],
  },
  // Public album view (no auth, share link)
  {
    path: '/view/:id',
    name: 'ShareView',
    component: () => import('@/views/ShareViewPage.vue'),
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
      {
        path: 'complete',
        name: 'ShareComplete',
        component: () => import('@/views/ShareCompletePage.vue'),
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

  // Redirect logged-in users from home to dashboard
  if (to.path === '/' && userStore.isLoggedIn) {
    next('/dashboard')
    return
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
