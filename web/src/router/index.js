import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { loadSetupStatus, setupState } from '@/config/setup'

const routes = [
  {
    path: '/setup',
    name: 'setup',
    component: () => import('@/views/SetupView.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/StoreLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
      { path: 'blog', name: 'blog', component: () => import('@/views/BlogView.vue') },
      { path: 'blog/:id', name: 'article', component: () => import('@/views/ArticleView.vue') },
      { path: 'about', name: 'about', component: () => import('@/views/AboutView.vue') },
      {
        path: 'account',
        name: 'account',
        component: () => import('@/views/AccountView.vue'),
        meta: { requiresAuth: true }
      },
      { path: 'login', name: 'login', component: () => import('@/views/LoginView.vue') },
      { path: 'register', name: 'register', component: () => import('@/views/RegisterView.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/overview' },
      { path: 'overview', name: 'admin-overview', component: () => import('@/views/admin/AdminOverview.vue') },
      { path: 'articles', name: 'admin-articles', component: () => import('@/views/admin/AdminArticles.vue') },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/AdminUsers.vue') },
      {
        path: 'assistant',
        name: 'admin-assistant',
        component: () => import('@/views/admin/AdminAssistant.vue'),
        meta: { requiresSuperAdmin: true }
      },
      {
        path: 'settings',
        name: 'admin-settings',
        component: () => import('@/views/admin/AdminSettings.vue'),
        meta: { requiresSuperAdmin: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, top: 100, behavior: 'smooth' }
    return { top: 0 }
  }
})

router.beforeEach(async to => {
  if (!setupState.checked) {
    await loadSetupStatus()
  }
  if (setupState.required && to.name !== 'setup') {
    return { name: 'setup' }
  }
  if (!setupState.required && to.name === 'setup') {
    return { name: 'home' }
  }

  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    if (!userStore.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    return { name: 'home' }
  }
  if (to.meta.requiresSuperAdmin && !userStore.isSuperAdmin) {
    if (!userStore.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    return { name: 'admin-overview' }
  }
  return true
})

export default router
