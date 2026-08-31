/**
 * 会话 Store：管理端登录用户信息。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const username = ref<string | null>(null)
  const isAuthenticated = computed(() => Boolean(username.value))

  function setUser(name: string | null) {
    username.value = name
  }

  function clear() {
    username.value = null
  }

  return { username, isAuthenticated, setUser, clear }
})
