/**
 * 分类树展示视图偏好（简易七级 / 完整阶元）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { TaxonView } from '@/api/taxon'

const STORAGE_KEY = 'biotree.taxonView'

function readInitial(): TaxonView {
  const raw = localStorage.getItem(STORAGE_KEY)
  return raw === 'full' ? 'full' : 'simple'
}

export const useTaxonViewStore = defineStore('taxonView', () => {
  const view = ref<TaxonView>(readInitial())

  watch(view, (v) => {
    localStorage.setItem(STORAGE_KEY, v)
  })

  function setView(next: TaxonView) {
    view.value = next
  }

  function toggle() {
    view.value = view.value === 'simple' ? 'full' : 'simple'
  }

  return { view, setView, toggle }
})
