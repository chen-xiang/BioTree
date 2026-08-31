<script setup lang="ts">
/**
 * 分类浏览页：搜索 + 懒加载树 + 详情。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 详情区展示配图画廊
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import {
  fetchChildren,
  fetchTaxonDetail,
  searchTaxa,
  type TaxonDetail,
  type TaxonListItem,
} from '@/api/taxon'
import TaxonTreeNode from '@/components/taxon/TaxonTreeNode.vue'
import BtButton from '@/components/ui/BtButton.vue'
import { useLocaleStore } from '@/stores/locale'

const { t } = useI18n()
const localeStore = useLocaleStore()

const roots = ref<TaxonListItem[]>([])
const selectedId = ref<number | null>(null)
const detail = ref<TaxonDetail | null>(null)
const loadingRoots = ref(false)
const loadingDetail = ref(false)
const query = ref('')
const searchHits = ref<TaxonListItem[]>([])
const searching = ref(false)
const error = ref('')

const apiLocale = computed(() => localeStore.locale)

async function loadRoots() {
  loadingRoots.value = true
  error.value = ''
  try {
    const page = await fetchChildren(null, apiLocale.value)
    roots.value = page.items
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  } finally {
    loadingRoots.value = false
  }
}

async function loadDetail(id: number) {
  selectedId.value = id
  loadingDetail.value = true
  try {
    detail.value = await fetchTaxonDetail(id, apiLocale.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  } finally {
    loadingDetail.value = false
  }
}

async function onSearch() {
  if (query.value.trim().length < 2) {
    searchHits.value = []
    return
  }
  searching.value = true
  try {
    const page = await searchTaxa(query.value.trim(), apiLocale.value)
    searchHits.value = page.items
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  } finally {
    searching.value = false
  }
}

onMounted(loadRoots)

watch(apiLocale, async () => {
  await loadRoots()
  if (selectedId.value != null) {
    await loadDetail(selectedId.value)
  }
  if (query.value.trim().length >= 2) {
    await onSearch()
  }
})
</script>

<template>
  <section class="browse">
    <header class="head">
      <div>
        <h1>{{ t('browse.title') }}</h1>
        <p>{{ t('browse.subtitle') }}</p>
      </div>
      <form class="search" @submit.prevent="onSearch">
        <input v-model="query" :placeholder="t('browse.searchPlaceholder')" />
        <BtButton type="submit">{{ t('browse.search') }}</BtButton>
      </form>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="searchHits.length" class="hits">
      <h2>{{ t('browse.searchResults') }}</h2>
      <button
        v-for="hit in searchHits"
        :key="hit.id"
        type="button"
        class="hit"
        @click="loadDetail(hit.id)"
      >
        <strong>{{ hit.scientificName }}</strong>
        <span>{{ hit.commonName || hit.rank }}</span>
      </button>
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
              :to="`/browse?id=${crumb.id}`"
              @click.prevent="loadDetail(crumb.id)"
            >
              {{ crumb.commonName || crumb.scientificName }}
            </RouterLink>
          </nav>
          <h2 class="sci">{{ detail.scientificName }}</h2>
          <p v-if="detail.commonName" class="common">{{ detail.commonName }}</p>
          <p class="meta">{{ detail.rank }} · {{ t('browse.childrenCount', { n: detail.childCount }) }}</p>
          <p v-if="detail.summary" class="summary">{{ detail.summary }}</p>
          <div v-if="detail.description" class="desc">{{ detail.description }}</div>
          <div v-if="detail.media.length" class="gallery">
            <figure v-for="m in detail.media" :key="m.id">
              <img :src="m.url" :alt="m.caption || detail.scientificName" loading="lazy" />
              <figcaption v-if="m.caption">{{ m.caption }}</figcaption>
            </figure>
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
}

.search input {
  min-width: min(20rem, 70vw);
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-elevated);
  color: var(--color-text);
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
  white-space: pre-wrap;
  line-height: 1.65;
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
  padding: 0.7rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
  cursor: pointer;
  text-align: left;
  color: inherit;
  transition: transform var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out);
}

.hit:hover {
  transform: translateY(-1px);
  border-color: var(--color-primary);
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
