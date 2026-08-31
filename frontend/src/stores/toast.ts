/**
 * 简易 Toast 状态（全局）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastItem = { id: number; message: string; tone: 'ok' | 'error' }

export const useToastStore = defineStore('toast', () => {
  const items = ref<ToastItem[]>([])
  let seq = 0

  function push(message: string, tone: 'ok' | 'error' = 'ok') {
    const id = ++seq
    items.value = [...items.value, { id, message, tone }]
    window.setTimeout(() => {
      items.value = items.value.filter((t) => t.id !== id)
    }, 3200)
  }

  return { items, push }
})
