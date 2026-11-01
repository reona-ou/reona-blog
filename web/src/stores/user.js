import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import { STORAGE_KEY } from '@/api/http'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const user = ref(readUser())

  const isLoggedIn = computed(() => Boolean(user.value?.token))
  const isSuperAdmin = computed(() => user.value?.role === 'ROLE_SUPER_ADMIN')
  const isAdmin = computed(
    () => user.value?.role === 'ROLE_ADMIN' || isSuperAdmin.value
  )
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  function persist(value) {
    user.value = value
    if (value) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  async function login(credentials) {
    const data = await loginApi(credentials)
    persist(data)
    return data
  }

  function setUser(value) {
    persist(value)
  }

  function logout() {
    persist(null)
  }

  return { user, isLoggedIn, isAdmin, isSuperAdmin, displayName, login, setUser, logout }
})
