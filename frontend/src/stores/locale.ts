/**
 * 语言 Store：与 vue-i18n 同步并持久化。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { i18n, type AppLocale } from '@/locales'

const STORAGE_KEY = 'biotree.locale'

function readInitialLocale(): AppLocale {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'zh-CN' || saved === 'en') {
    return saved
  }
  return 'zh-CN'
}

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<AppLocale>(readInitialLocale())
  const label = computed(() => (locale.value === 'zh-CN' ? '中文' : 'English'))

  function setLocale(next: AppLocale) {
    locale.value = next
  }

  function toggleLocale() {
    locale.value = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
  }

  watch(
    locale,
    (next) => {
      i18n.global.locale.value = next
      document.documentElement.lang = next
      localStorage.setItem(STORAGE_KEY, next)
    },
    { immediate: true },
  )

  return { locale, label, setLocale, toggleLocale }
})
