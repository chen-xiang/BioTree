<script setup lang="ts">
/**
 * 分类浏览页：搜索 + 懒加载树 + 详情（可分享路由）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 详情区展示配图画廊
 * Updated: 2026-08-31 搜索防抖、AbortController、/browse/:id、分页
 * Updated: 2026-08-31 移动端树/详情切换与配图加载更多
 * Updated: 2026-09-01 搜索改为下拉、详情改为标本卡、工作台分栏
 * Updated: 2026-09-01 浏览工作台单栏滚动，树滚动根提供给续页观察
 * Updated: 2026-09-03 未分类目录详情与面包屑
 */
import { computed, onMounted, onBeforeUnmount, provide, ref, watch } from 'vue'
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
import { TAXON_TREE_SCROLL_ROOT } from '@/composables/useTaxonChildren'
import RankSpine from '@/components/taxon/RankSpine.vue'
import TaxonTreeNode from '@/components/taxon/TaxonTreeNode.vue'
import BtButton from '@/components/ui/BtButton.vue'
import BtInput from '@/components/ui/BtInput.vue'
import BtPagination from '@/components/ui/BtPagination.vue'
import BtVirtualList from '@/components/ui/BtVirtualList.vue'
import { useLocaleStore } from '@/stores/locale'
import { useTaxonViewStore } from '@/stores/taxonView'
import { useToastStore } from '@/stores/toast'
import { messageFromApiError, rankLabel } from '@/utils/apiError'
import { isUnclassifiedId } from '@/domain/unclassified'
import { debounce } from '@/utils/debounce'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  id?: string
}>()

const { t } = useI18n()
const localeStore = useLocaleStore()
const taxonViewStore = useTaxonViewStore()
const toast = useToastStore()
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
const mobilePane = ref<'tree' | 'detail'>('tree')
const crumbsExpanded = ref(false)
const searchOpen = ref(false)
const searchRoot = ref<HTMLElement | null>(null)
const treeScroll = ref<HTMLElement | null>(null)
provide(TAXON_TREE_SCROLL_ROOT, treeScroll)

const apiLocale = computed(() => localeStore.locale)
const treeView = computed(() => taxonViewStore.view)
const SEARCH_SIZE = 20
const MEDIA_PAGE = 12
const descriptionHtml = computed(() => renderMarkdown(detail.value?.description))
const hasMoreMedia = computed(() => gallery.value.length < mediaTotal.value)
const authorshipLine = computed(() => {
  const a = detail.value?.scientificNameAuthorship
  return a && a.trim() ? a.trim() : ''
})
const otherVernaculars = computed(() => {
  const list = detail.value?.vernaculars ?? []
  const current = detail.value?.locale
  return list.filter((v) => v.locale !== current && v.commonName)
})
const distributions = computed(() => detail.value?.distributions ?? [])
const namePublishedIn = computed(() => detail.value?.namePublishedIn?.trim() || '')
const nomenclaturalMeta = computed(() => {
  const parts = [
    detail.value?.nomenclaturalCode,
    detail.value?.nomenclaturalStatus,
  ].filter((p): p is string => !!p && p.trim().length > 0)
  return parts.join(' · ')
})
const crumbHead = computed(() => {
  const crumbs = detail.value?.breadcrumbs ?? []
  if (crumbsExpanded.value || crumbs.length <= 5) return crumbs
  return crumbs.slice(0, 1)
})
const crumbTail = computed(() => {
  const crumbs = detail.value?.breadcrumbs ?? []
  if (crumbsExpanded.value || crumbs.length <= 5) return []
  return crumbs.slice(-3)
})
function crumbLabel(crumb: { id: number; scientificName: string; commonName?: string | null }) {
  if (isUnclassifiedId(crumb.id)) return t('browse.unclassified')
  return crumb.commonName || crumb.scientificName
}

const crumbsCollapsed = computed(() => {
  const n = detail.value?.breadcrumbs?.length ?? 0
  return !crumbsExpanded.value && n > 5
})

let detailAbort: AbortController | null = null
let searchAbort: AbortController | null = null

async function loadRoots() {
  loadingRoots.value = true
  error.value = ''
  try {
    const page = await fetchChildren(null, apiLocale.value, 0, 30, undefined, treeView.value)
    roots.value = page.items
  } catch (e) {
    error.value = messageFromApiError(e)
  } finally {
    loadingRoots.value = false
  }
}

async function loadDetail(id: number, syncRoute = true) {
  selectedId.value = id
  mobilePane.value = 'detail'
  crumbsExpanded.value = false
  if (syncRoute && String(route.params.id ?? '') !== String(id)) {
    await router.replace({ name: 'browse', params: { id: String(id) } })
  }
  detailAbort?.abort()
  detailAbort = new AbortController()
  loadingDetail.value = true
  try {
    detail.value = await fetchTaxonDetail(id, apiLocale.value, detailAbort.signal, treeView.value)
    gallery.value = [...(detail.value.media ?? [])]
    mediaTotal.value = detail.value.mediaTotal ?? gallery.value.length
    const anchor = detail.value.nearestSimpleAncestorId
    if (treeView.value === 'simple' && !detail.value.placeholder && anchor != null && anchor !== id) {
      toast.push(t('browse.jumpedToVisibleAncestor'), 'ok')
      await loadDetail(anchor, true)
      return
    }
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = messageFromApiError(e)
  } finally {
    loadingDetail.value = false
  }
}

async function loadMoreMedia() {
  if (!detail.value || detail.value.placeholder || !hasMoreMedia.value || loadingMoreMedia.value) return
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
    searchOpen.value = false
    return
  }
  searchAbort?.abort()
  searchAbort = new AbortController()
  searching.value = true
  searchOpen.value = true
  try {
    const result = await searchTaxa(q, apiLocale.value, page, SEARCH_SIZE, searchAbort.signal)
    searchHits.value = result.items
    searchTotal.value = result.total
    searchPage.value = result.page
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = messageFromApiError(e)
  } finally {
    searching.value = false
  }
}

const debouncedSearch = debounce(() => {
  void runSearch(0)
}, 320)

function onSearchSubmit() {
  searchOpen.value = true
  void runSearch(0)
}

function onSelectHit(id: number) {
  searchOpen.value = false
  void loadDetail(id)
}

function clearSearch() {
  query.value = ''
  searchHits.value = []
  searchTotal.value = 0
  searchPage.value = 0
  searchOpen.value = false
}

function reopenSearch() {
  if (searchHits.value.length || searching.value) {
    searchOpen.value = true
  }
}

function onDocPointerDown(event: PointerEvent) {
  if (!searchOpen.value) return
  const root = searchRoot.value
  if (root && event.target instanceof Node && !root.contains(event.target)) {
    searchOpen.value = false
  }
}

onMounted(async () => {
  document.addEventListener('pointerdown', onDocPointerDown)
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

watch(treeView, async () => {
  await loadRoots()
  if (selectedId.value != null) {
    await loadDetail(selectedId.value, false)
  }
})

watch(query, () => {
  debouncedSearch()
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocPointerDown)
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
      <div class="head-actions">
        <div class="view-toggle" role="group" :aria-label="t('browse.viewToggle')">
          <button
            type="button"
            :class="{ active: treeView === 'simple' }"
            @click="taxonViewStore.setView('simple')"
          >
            {{ t('browse.viewSimple') }}
          </button>
          <button
            type="button"
            :class="{ active: treeView === 'full' }"
            @click="taxonViewStore.setView('full')"
          >
            {{ t('browse.viewFull') }}
          </button>
        </div>
        <form ref="searchRoot" class="search" @submit.prevent="onSearchSubmit" @focusin="reopenSearch">
          <BtInput v-model="query" :placeholder="t('browse.searchPlaceholder')" />
          <div class="search-actions">
            <BtButton v-if="query" variant="ghost" type="button" @click="clearSearch">
              {{ t('browse.clearSearch') }}
            </BtButton>
            <BtButton type="submit">{{ t('browse.search') }}</BtButton>
          </div>
          <div v-if="searchOpen && (searchHits.length || searching)" class="hits" role="listbox">
            <p class="hits-label">{{ t('browse.searchResults') }}</p>
            <p v-if="searching" class="muted">{{ t('common.loading') }}</p>
            <BtVirtualList
              v-if="searchHits.length"
              :items="searchHits"
              :item-height="56"
              :height="Math.min(280, Math.max(112, searchHits.length * 56))"
            >
              <template #default="{ item }">
                <button type="button" class="hit" @click="onSelectHit(item.id)">
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
        </form>
      </div>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <div class="mobile-tabs" role="tablist">
      <button
        type="button"
        role="tab"
        :aria-selected="mobilePane === 'tree'"
        :class="{ active: mobilePane === 'tree' }"
        @click="mobilePane = 'tree'"
      >
        {{ t('browse.tree') }}
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="mobilePane === 'detail'"
        :class="{ active: mobilePane === 'detail' }"
        @click="mobilePane = 'detail'"
      >
        {{ t('browse.detailTab') }}
      </button>
    </div>

    <div class="split" :data-pane="mobilePane">
      <aside
        ref="treeScroll"
        class="tree panel bt-scroll"
        :class="{ 'pane-hidden-mobile': mobilePane !== 'tree' }"
      >
        <h2>{{ t('browse.tree') }}</h2>
        <p v-if="loadingRoots" class="muted">{{ t('common.loading') }}</p>
        <TaxonTreeNode
          v-for="node in roots"
          :key="`${node.id}-${treeView}`"
          :node="node"
          :depth="0"
          :locale="apiLocale"
          :view="treeView"
          :selected-id="selectedId"
          @select="loadDetail"
        />
      </aside>

      <article
        class="detail panel bt-scroll"
        :class="{ 'pane-hidden-mobile': mobilePane !== 'detail' }"
      >
        <p v-if="loadingDetail" class="muted">{{ t('common.loading') }}</p>
        <template v-else-if="detail">
          <nav class="crumbs" :aria-label="t('browse.title')">
            <template v-for="(crumb, index) in crumbHead" :key="`h-${crumb.id}`">
              <span v-if="index > 0" class="sep" aria-hidden="true">›</span>
              <RouterLink :to="{ name: 'browse', params: { id: String(crumb.id) } }">
                {{ crumbLabel(crumb) }}
              </RouterLink>
            </template>
            <template v-if="crumbsCollapsed">
              <span class="sep" aria-hidden="true">›</span>
              <button type="button" class="crumb-more" @click="crumbsExpanded = true">…</button>
            </template>
            <template v-for="crumb in crumbTail" :key="`t-${crumb.id}`">
              <span class="sep" aria-hidden="true">›</span>
              <RouterLink :to="{ name: 'browse', params: { id: String(crumb.id) } }">
                {{ crumbLabel(crumb) }}
              </RouterLink>
            </template>
          </nav>
          <header class="plate">
            <p class="stamp">{{ rankLabel(detail.rank) }}</p>
            <h2 class="sci" :class="{ bucket: detail.placeholder }">
              {{ detail.placeholder ? t('browse.unclassified') : detail.scientificName }}
            </h2>
            <p v-if="authorshipLine" class="authorship">{{ authorshipLine }}</p>
            <p v-if="detail.commonName" class="common">{{ detail.commonName }}</p>
            <p class="meta">{{ t('browse.childrenCount', { n: detail.childCount }) }}</p>
          </header>
          <p v-if="namePublishedIn" class="muted pub">{{ t('browse.publishedIn') }}: {{ namePublishedIn }}</p>
          <p v-if="nomenclaturalMeta" class="muted">{{ nomenclaturalMeta }}</p>
          <p v-if="detail.summary" class="summary">{{ detail.summary }}</p>
          <div v-if="descriptionHtml" class="desc markdown" v-html="descriptionHtml" />
          <div v-if="otherVernaculars.length" class="vernaculars">
            <h3>{{ t('browse.otherVernaculars') }}</h3>
            <ul>
              <li v-for="v in otherVernaculars" :key="`${v.locale}-${v.commonName}`">
                <span class="locale">{{ v.locale }}</span>
                <span>{{ v.commonName }}</span>
              </li>
            </ul>
          </div>
          <div v-if="distributions.length" class="distributions">
            <h3>{{ t('browse.distributions') }}</h3>
            <ul>
              <li v-for="d in distributions" :key="d.id">
                <strong v-if="d.countryCode">{{ d.countryCode }}</strong>
                <span v-if="d.locality">{{ d.locality }}</span>
                <em v-if="d.establishmentMeans">{{ d.establishmentMeans }}</em>
              </li>
            </ul>
          </div>
          <div v-if="detail.synonyms?.length" class="synonyms">
            <h3>{{ t('browse.synonyms') }}</h3>
            <ul>
              <li v-for="s in detail.synonyms" :key="s.id">
                <em>{{ s.scientificName }}</em>
              </li>
            </ul>
          </div>
          <div v-if="gallery.length" class="gallery-wrap">
            <div class="gallery">
              <figure v-for="m in gallery" :key="m.id">
                <img :src="m.url" :alt="m.caption || detail.scientificName" loading="lazy" />
                <figcaption v-if="m.caption">{{ m.caption }}</figcaption>
              </figure>
            </div>
            <div v-if="hasMoreMedia" class="gallery-more">
              <BtButton variant="ghost" :disabled="loadingMoreMedia" @click="loadMoreMedia">
                {{ t('browse.loadMoreMedia') }}
              </BtButton>
            </div>
          </div>
        </template>
        <div v-else class="empty">
          <RankSpine compact :caption="t('home.spineCaption')" />
          <p>{{ t('browse.selectHint') }}</p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.browse {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  flex: 1;
  min-height: 0;
}

.head {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  justify-content: space-between;
  align-items: end;
}

.head-actions {
  display: grid;
  gap: var(--space-3);
  justify-items: end;
}

.view-toggle {
  display: inline-flex;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.view-toggle button {
  border: 0;
  background: var(--color-bg-elevated);
  color: var(--color-text-muted);
  padding: var(--space-2) var(--space-3);
  cursor: pointer;
}

.view-toggle button.active {
  color: var(--color-text);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-bg-elevated));
}

h1 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
  font-size: clamp(1.55rem, 3vw, 2rem);
  font-weight: 650;
}

.head p {
  margin: 0;
  color: var(--color-text-muted);
}

.search {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-2);
  align-items: center;
}

.search :deep(.bt-input) {
  min-width: min(18rem, 100%);
}

.search-actions {
  display: flex;
  gap: var(--space-2);
}

.hits {
  position: absolute;
  top: calc(100% + 0.4rem);
  right: 0;
  z-index: 8;
  width: min(28rem, 86vw);
  display: grid;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

.hits-label {
  margin: 0;
  font-size: var(--text-xs);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
}

.hit {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
  height: 100%;
  padding: 0.5rem 0.7rem;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  text-align: left;
  color: inherit;
}

.hit:hover,
.hit:focus-visible {
  background: var(--color-bg-muted);
}

.hit strong {
  font-family: var(--font-display);
  font-style: italic;
  font-weight: 500;
}

.hit span {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.split {
  display: grid;
  grid-template-columns: minmax(240px, 0.85fr) minmax(280px, 1.25fr);
  gap: var(--space-4);
  align-items: stretch;
  flex: 1;
  min-height: 0;
}

.panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
  min-height: 0;
}

.tree h2 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-weight: 500;
}

.plate {
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.stamp {
  display: inline-block;
  margin: 0 0 var(--space-3);
  padding: 0.15rem 0.5rem;
  border: 1px solid color-mix(in srgb, var(--color-accent) 55%, var(--color-border));
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sci {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 3vw, 2.1rem);
  color: var(--color-text);
  font-style: italic;
  font-weight: 650;
  line-height: 1.2;
}

.sci.bucket {
  font-style: normal;
  font-family: var(--font-sans);
  color: var(--color-text-muted);
}

.authorship {
  margin: 0 0 var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
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

.summary {
  margin: var(--space-3) 0 0;
}

.desc {
  margin-top: var(--space-4);
  line-height: 1.7;
}

.desc.markdown :deep(p) {
  margin: 0 0 var(--space-3);
}

.desc.markdown :deep(ul) {
  margin: 0 0 var(--space-3);
  padding-left: 1.2rem;
}

.synonyms,
.vernaculars,
.distributions {
  margin-top: var(--space-5);
}

.synonyms h3,
.vernaculars h3,
.distributions h3 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xs);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-weight: 500;
}

.synonyms ul,
.vernaculars ul,
.distributions ul {
  margin: 0;
  padding-left: 1.1rem;
}

.synonyms em {
  font-style: italic;
}

.vernaculars .locale {
  display: inline-block;
  min-width: 3.5rem;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  margin-right: var(--space-2);
}

.distributions li {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.pub {
  margin: 0.25rem 0 0;
  font-size: var(--text-sm);
}

.gallery-wrap {
  margin-top: var(--space-5);
}

.gallery {
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

.gallery-more {
  margin-top: var(--space-3);
}

.crumbs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.45rem;
  margin-bottom: var(--space-4);
  font-size: var(--text-sm);
}

.crumbs a {
  color: var(--color-primary);
}

.sep,
.crumb-more {
  color: var(--color-text-muted);
}

.crumb-more {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0 var(--space-1);
}

.empty {
  display: grid;
  justify-items: start;
  gap: var(--space-4);
  padding: var(--space-4) 0;
  color: var(--color-text-muted);
}

.empty p {
  margin: 0;
  max-width: 22rem;
}

.error {
  color: var(--color-danger);
}

.mobile-tabs {
  display: none;
}

@media (max-width: 860px) {
  .head-actions {
    justify-items: stretch;
    width: 100%;
  }

  .search,
  .view-toggle {
    width: 100%;
  }

  .search {
    grid-template-columns: 1fr;
  }

  .search :deep(.bt-input) {
    min-width: 0;
  }

  .search-actions {
    justify-content: flex-end;
  }

  .mobile-tabs {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-2);
  }

  .mobile-tabs button {
    border: 1px solid var(--color-border);
    background: var(--color-bg-elevated);
    color: var(--color-text-muted);
    border-radius: var(--radius-md);
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
  }

  .mobile-tabs button.active {
    color: var(--color-text);
    background: color-mix(in srgb, var(--color-primary) 10%, var(--color-bg-elevated));
    border-color: color-mix(in srgb, var(--color-primary) 35%, var(--color-border));
  }

  .browse {
    flex: none;
    height: auto;
  }

  .split {
    grid-template-columns: 1fr;
    flex: none;
  }

  .pane-hidden-mobile {
    display: none;
  }
}
</style>
