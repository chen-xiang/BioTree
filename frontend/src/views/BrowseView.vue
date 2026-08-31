<script setup lang="ts">
/**
 * 分类浏览页：搜索 + 懒加载树 + 详情（可分享路由）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 详情区展示配图画廊
 * Updated: 2026-08-31 搜索防抖、AbortController、/browse/:id、分页
 */
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import {
  fetchChildren,
  fetchTaxonDetail,
  fetchTaxonMedia,
  searchTaxa,
  type TaxonDetail,
  type TaxonListItem,
  type TaxonMedia,
} from '@/api/taxon'
import TaxonTreeNode from '@/components/taxon/TaxonTreeNode.vue'
import BtButton from '@/components/ui/BtButton.vue'
import BtInput from '@/components/ui/BtInput.vue'
import BtPagination from '@/components/ui/BtPagination.vue'
import BtVirtualList from '@/components/ui/BtVirtualList.vue'
import { useLocaleStore } from '@/stores/locale'
import { messageFromApiError, rankLabel } from '@/utils/apiError'
import { debounce } from '@/utils/debounce'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  id?: string
}>()

const { t } = useI18n()
const localeStore = useLocaleStore()
const route = useRoute()
const router = useRouter()

const roots = ref<TaxonListItem[]>([])
const selectedId = ref<number | null>(null)
const detail = ref<TaxonDetail | null>(null)
const loadingRoots = ref(false)
const loadingDetail = ref(false)
const query = ref('')
const searchHits = ref<TaxonListItem[]>([])
const searchPage = ref(0)
const searchTotal = ref(0)
const searching = ref(false)
const error = ref('')
const gallery = ref<TaxonMedia[]>([])
const mediaTotal = ref(0)
const loadingMoreMedia = ref(false)

const apiLocale = computed(() => localeStore.locale)
const SEARCH_SIZE = 20
const MEDIA_PAGE = 12
const descriptionHtml = computed(() => renderMarkdown(detail.value?.description))
const hasMoreMedia = computed(() => gallery.value.length < mediaTotal.value)

let detailAbort: AbortController | null = null
let searchAbort: AbortController | null = null

async function loadRoots() {
  loadingRoots.value = true
  error.value = ''
  try {
    const page = await fetchChildren(null, apiLocale.value)
    roots.value = page.items
  } catch (e) {
    error.value = messageFromApiError(e)
  } finally {
    loadingRoots.value = false
  }
}

async function loadDetail(id: number, syncRoute = true) {
  selectedId.value = id
  if (syncRoute && String(route.params.id ?? '') !== String(id)) {
    await router.replace({ name: 'browse', params: { id: String(id) } })
  }
  detailAbort?.abort()
  detailAbort = new AbortController()
  loadingDetail.value = true
  try {
    detail.value = await fetchTaxonDetail(id, apiLocale.value, detailAbort.signal)
    gallery.value = [...(detail.value.media ?? [])]
    mediaTotal.value = detail.value.mediaTotal ?? gallery.value.length
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = messageFromApiError(e)
  } finally {
    loadingDetail.value = false
  }
}

async function loadMoreMedia() {
  if (!detail.value || !hasMoreMedia.value || loadingMoreMedia.value) return
  loadingMoreMedia.value = true
  try {
    const page = Math.floor(gallery.value.length / MEDIA_PAGE)
    const result = await fetchTaxonMedia(detail.value.id, page, MEDIA_PAGE)
    mediaTotal.value = result.total
    const seen = new Set(gallery.value.map((m) => m.id))
    for (const item of result.items) {
      if (!seen.has(item.id)) gallery.value.push(item)
    }
  } catch (e) {
    error.value = messageFromApiError(e)
  } finally {
    loadingMoreMedia.value = false
  }
}

async function runSearch(page = 0) {
  const q = query.value.trim()
  if (q.length < 2) {
    searchHits.value = []
    searchTotal.value = 0
    searchPage.value = 0
    return
  }
  searchAbort?.abort()
  searchAbort = new AbortController()
  searching.value = true
  try {
    const result = await searchTaxa(q, apiLocale.value, page, SEARCH_SIZE, searchAbort.signal)
    searchHits.value = result.items
    searchTotal.value = result.total
    searchPage.value = result.page
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = e instanceof Error ? e.message : 'failed'
  } finally {
    searching.value = false
  }
}

const debouncedSearch = debounce(() => {
  void runSearch(0)
}, 320)

function onSearchSubmit() {
  void runSearch(0)
}

onMounted(async () => {
  await loadRoots()
  const routeId = props.id ? Number(props.id) : NaN
  if (Number.isFinite(routeId)) {
    await loadDetail(routeId, false)
  }
})

watch(
  () => props.id,
  async (value) => {
    if (!value) {
      selectedId.value = null
      detail.value = null
      return
    }
    const id = Number(value)
    if (Number.isFinite(id) && id !== selectedId.value) {
      await loadDetail(id, false)
    }
  },
)

watch(apiLocale, async () => {
  await loadRoots()
  if (selectedId.value != null) {
    await loadDetail(selectedId.value, false)
  }
  if (query.value.trim().length >= 2) {
    await runSearch(searchPage.value)
  }
})

watch(query, () => {
  debouncedSearch()
})

onBeforeUnmount(() => {
  detailAbort?.abort()
  searchAbort?.abort()
})
</script>

<template>
  <section class="browse">
    <header class="head">
      <div>
        <h1>{{ t('browse.title') }}</h1>
        <p>{{ t('browse.subtitle') }}</p>
      </div>
      <form class="search" @submit.prevent="onSearchSubmit">
        <BtInput v-model="query" :placeholder="t('browse.searchPlaceholder')" />
        <BtButton type="submit">{{ t('browse.search') }}</BtButton>
      </form>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="searchHits.length || searching" class="hits">
      <h2>{{ t('browse.searchResults') }}</h2>
      <p v-if="searching" class="muted">{{ t('common.loading') }}</p>
      <BtVirtualList
        v-if="searchHits.length"
        :items="searchHits"
        :item-height="56"
        :height="Math.min(320, Math.max(120, searchHits.length * 56))"
      >
        <template #default="{ item }">
          <button type="button" class="hit" @click="loadDetail(item.id)">
            <strong>{{ item.scientificName }}</strong>
            <span>{{ item.commonName || rankLabel(item.rank) }}</span>
          </button>
        </template>
      </BtVirtualList>
      <BtPagination
        :page="searchPage"
        :size="SEARCH_SIZE"
        :total="searchTotal"
        @update:page="runSearch"
      />
    </div>

    <div class="split">
      <aside class="tree panel">
        <h2>{{ t('browse.tree') }}</h2>
        <p v-if="loadingRoots" class="muted">{{ t('common.loading') }}</p>
        <TaxonTreeNode
          v-for="node in roots"
          :key="node.id"
          :node="node"
          :depth="0"
          :locale="apiLocale"
          :selected-id="selectedId"
          @select="loadDetail"
        />
      </aside>

      <article class="detail panel">
        <p v-if="loadingDetail" class="muted">{{ t('common.loading') }}</p>
        <template v-else-if="detail">
          <nav class="crumbs">
            <RouterLink
              v-for="crumb in detail.breadcrumbs"
              :key="crumb.id"
              :to="{ name: 'browse', params: { id: String(crumb.id) } }"
            >
              {{ crumb.commonName || crumb.scientificName }}
            </RouterLink>
          </nav>
          <h2 class="sci">{{ detail.scientificName }}</h2>
          <p v-if="detail.commonName" class="common">{{ detail.commonName }}</p>
          <p class="meta">{{ rankLabel(detail.rank) }} · {{ t('browse.childrenCount', { n: detail.childCount }) }}</p>
          <p v-if="detail.summary" class="summary">{{ detail.summary }}</p>
          <div v-if="descriptionHtml" class="desc markdown" v-html="descriptionHtml" />
          <div v-if="detail.synonyms?.length" class="synonyms">
            <h3>{{ t('browse.synonyms') }}</h3>
            <ul>
              <li v-for="s in detail.synonyms" :key="s.id">
                <em>{{ s.scientificName }}</em>
              </li>
            </ul>
          </div>
          <div v-if="gallery.length" class="gallery">
            <figure v-for="m in gallery" :key="m.id">
              <img :src="m.url" :alt="m.caption || detail.scientificName" loading="lazy" />
              <figcaption v-if="m.caption">{{ m.caption }}</figcaption>
            </figure>
            <div v-if="hasMoreMedia" class="gallery-more">
              <BtButton variant="ghost" :disabled="loadingMoreMedia" @click="loadMoreMedia">
                {{ t('browse.loadMoreMedia') }}
              </BtButton>
            </div>
          </div>
        </template>
        <p v-else class="muted">{{ t('browse.selectHint') }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.browse {
  animation: rise var(--duration-normal) var(--ease-out);
  display: grid;
  gap: var(--space-5);
}

.head {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  justify-content: space-between;
  align-items: end;
}

h1 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 3vw, 2.1rem);
}

.head p {
  margin: 0;
  color: var(--color-text-muted);
}

.search {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.search :deep(.bt-input) {
  min-width: min(20rem, 70vw);
}

.split {
  display: grid;
  grid-template-columns: minmax(240px, 0.9fr) minmax(280px, 1.2fr);
  gap: var(--space-4);
}

.panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
  min-height: 22rem;
}

.tree {
  max-height: min(70vh, 40rem);
  overflow: auto;
}

.tree h2,
.detail h2,
.hits h2 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-md);
  color: var(--color-text-muted);
  font-weight: 600;
}

.sci {
  margin: 0 0 var(--space-2) !important;
  font-family: var(--font-display);
  font-size: var(--text-2xl) !important;
  color: var(--color-text) !important;
  font-style: italic;
}

.common {
  margin: 0 0 var(--space-2);
  font-size: var(--text-lg);
}

.meta,
.muted,
.summary {
  color: var(--color-text-muted);
}

.desc {
  margin-top: var(--space-4);
  line-height: 1.65;
}

.desc.markdown :deep(p) {
  margin: 0 0 var(--space-3);
}

.desc.markdown :deep(ul) {
  margin: 0 0 var(--space-3);
  padding-left: 1.2rem;
}

.synonyms {
  margin-top: var(--space-4);
}

.synonyms h3 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.synonyms ul {
  margin: 0;
  padding-left: 1.1rem;
}

.synonyms em {
  font-style: italic;
}

.gallery {
  margin-top: var(--space-5);
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--space-3);
}

.gallery figure {
  margin: 0;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  animation: rise var(--duration-normal) var(--ease-out);
}

.gallery img {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.gallery figcaption {
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.crumbs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  font-size: var(--text-sm);
}

.crumbs a {
  color: var(--color-primary);
}

.crumbs a::after {
  content: '/';
  margin-left: var(--space-2);
  color: var(--color-text-muted);
}

.crumbs a:last-child::after {
  content: '';
}

.hits {
  display: grid;
  gap: var(--space-2);
}

.hit {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
  height: 100%;
  padding: 0.55rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
  cursor: pointer;
  text-align: left;
  color: inherit;
}

.error {
  color: var(--color-danger);
}

@media (max-width: 860px) {
  .split {
    grid-template-columns: 1fr;
  }
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
