/**
 * 主题 Store：深浅色切换并持久化。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'biotree.theme'

function readInitialTheme(): ThemeMode {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') {
    return saved
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readInitialTheme())
  const isDark = computed(() => mode.value === 'dark')

  function applyDom(theme: ThemeMode) {
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }

  function setTheme(theme: ThemeMode) {
    mode.value = theme
  }

  function toggleTheme() {
    mode.value = mode.value === 'dark' ? 'light' : 'dark'
  }

  watch(
    mode,
    (theme) => {
      applyDom(theme)
      localStorage.setItem(STORAGE_KEY, theme)
    },
    { immediate: true },
  )

  return { mode, isDark, setTheme, toggleTheme }
})
