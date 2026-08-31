/**
 * 路由：公开浏览与管理端分离；管理端未登录跳转登录页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/LoginView.vue'),
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'admin-home',
          component: () => import('@/views/admin/AdminHomeView.vue'),
        },
      ],
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) {
    return true
  }
  const auth = useAuthStore()
  if (auth.isAuthenticated) {
    return true
  }
  try {
    const response = await fetch('/api/admin/auth/me', { credentials: 'include' })
    if (response.ok) {
      const body = await response.json()
      if (body.code === 0 && body.data?.username) {
        auth.setUser(body.data.username)
        return true
      }
    }
  } catch {
    // ignore and redirect to login
  }
  return { name: 'login', query: { redirect: to.fullPath } }
})

export default router
