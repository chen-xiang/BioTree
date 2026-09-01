/**
 * 分类树子节点分页：展开拉第一页，触底续页，不一次拉完整层。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
import { computed, ref } from 'vue'
import { fetchChildren, type TaxonListItem, type TaxonView } from '@/api/taxon'

export const TREE_CHILD_PAGE_SIZE = 50
export const TAXON_TREE_SCROLL_ROOT = Symbol('taxonTreeScrollRoot')

export type ChildrenLoader = typeof fetchChildren

export function useTaxonChildren(options: {
  parentId: () => number
  locale: () => string
  view: () => TaxonView
  load?: ChildrenLoader
}) {
  const load = options.load ?? fetchChildren
  const children = ref<TaxonListItem[]>([])
  const page = ref(0)
  const total = ref(0)
  const loaded = ref(false)
  const loading = ref(false)
  const loadingMore = ref(false)

  const hasMore = computed(() => loaded.value && children.value.length < total.value)

  async function fetchPage(nextPage: number, append: boolean) {
    const result = await load(
      options.parentId(),
      options.locale(),
      nextPage,
      TREE_CHILD_PAGE_SIZE,
      undefined,
      options.view(),
    )
    const items = result.items ?? []
    total.value = items.length === 0 ? (append ? children.value.length : 0) : result.total
    page.value = result.page
    children.value = append ? [...children.value, ...items] : items
    loaded.value = true
    return items.length
  }

  async function loadFirstPage() {
    if (loaded.value || loading.value) return
    loading.value = true
    try {
      await fetchPage(0, false)
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (!hasMore.value || loadingMore.value || loading.value) return 0
    loadingMore.value = true
    try {
      return await fetchPage(page.value + 1, true)
    } finally {
      loadingMore.value = false
    }
  }

  function reset() {
    loaded.value = false
    children.value = []
    page.value = 0
    total.value = 0
    loading.value = false
    loadingMore.value = false
  }

  return {
    children,
    hasMore,
    loaded,
    loading,
    loadingMore,
    loadFirstPage,
    loadMore,
    reset,
  }
}
