<script setup lang="ts">
/**
 * 管理端分类 CRUD 页：搜索跳转、面包屑、移动目标选择、配图图注编辑。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 支持编辑态配图上传与删除
 * Updated: 2026-08-31 支持节点移动与设计系统表单控件
 * Updated: 2026-08-31 搜索导航、面包屑、任意父移动、图注更新
 * Updated: 2026-08-31 完整阶元管理（view=full）与中间级等级
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  createTaxon,
  deleteTaxon,
  deleteTaxonMedia,
  fetchChildren,
  fetchTaxonDetail,
  fetchTaxonMedia,
  moveTaxon,
  searchTaxa,
  updateTaxon,
  updateTaxonMedia,
  uploadTaxonMedia,
  type TaxonDetail,
  type TaxonListItem,
  type TaxonRank,
} from '@/api/taxon'
import BtButton from '@/components/ui/BtButton.vue'
import BtDialog from '@/components/ui/BtDialog.vue'
import BtInput from '@/components/ui/BtInput.vue'
import BtPagination from '@/components/ui/BtPagination.vue'
import BtSelect from '@/components/ui/BtSelect.vue'
import BtTextarea from '@/components/ui/BtTextarea.vue'
import { useLocaleStore } from '@/stores/locale'
import { useToastStore } from '@/stores/toast'
import { messageFromApiError, rankLabel } from '@/utils/apiError'
import { debounce } from '@/utils/debounce'

type Crumb = { id: number | null; label: string }

/** 管理端按完整阶元编辑；主级在前，中间级随后 */
const RANKS: TaxonRank[] = [
  'KINGDOM',
  'SUBKINGDOM',
  'PHYLUM',
  'SUBPHYLUM',
  'CLASS',
  'SUBCLASS',
  'ORDER',
  'SUBORDER',
  'SUPERFAMILY',
  'FAMILY',
  'SUBFAMILY',
  'TRIBE',
  'GENUS',
  'SUBGENUS',
  'SPECIES',
  'SUBSPECIES',
  'VARIETY',
  'FORM',
  'OTHER',
]
const ADMIN_VIEW_FULL = 'full' as const
const ADMIN_VIEW_SIMPLE = 'simple' as const
const PAGE_SIZE = 30
const SEARCH_SIZE = 15

const { t } = useI18n()
const localeStore = useLocaleStore()
const toast = useToastStore()
const apiLocale = computed(() => localeStore.locale)
const rankOptions = computed(() => RANKS.map((rank) => ({ value: rank, label: rankLabel(rank) })))
const previewSimple = ref(false)
const listView = computed(() => (previewSimple.value ? ADMIN_VIEW_SIMPLE : ADMIN_VIEW_FULL))

const trail = ref<Crumb[]>([{ id: null, label: t('admin.root') }])
const parentId = computed(() => trail.value[trail.value.length - 1]?.id ?? null)
const items = ref<TaxonListItem[]>([])
const listPage = ref(0)
const listTotal = ref(0)
const editing = ref<TaxonDetail | null>(null)
const error = ref('')
const message = ref('')
const uploading = ref(false)
const mediaCaption = ref('')
const captionDrafts = ref<Record<number, string>>({})
const confirmOpen = ref(false)
const confirmKind = ref<'taxon' | 'media'>('taxon')
const confirmTargetId = ref<number | null>(null)

const searchQuery = ref('')
const searchHits = ref<TaxonListItem[]>([])
const searching = ref(false)

const moveQuery = ref('')
const moveHits = ref<TaxonListItem[]>([])
const moving = ref(false)

const form = reactive({
  rank: 'KINGDOM' as TaxonRank,
  scientificName: '',
  commonName: '',
  summary: '',
  description: '',
})

async function loadList(page = 0) {
  error.value = ''
  try {
    const result = await fetchChildren(
      parentId.value,
      apiLocale.value,
      page,
      PAGE_SIZE,
      undefined,
      listView.value,
    )
    items.value = result.items
    listPage.value = result.page
    listTotal.value = result.total
  } catch (e) {
    error.value = messageFromApiError(e)
  }
}

async function jumpToCrumb(index: number) {
  trail.value = trail.value.slice(0, index + 1)
  editing.value = null
  await loadList(0)
}

async function enterChild(item: TaxonListItem) {
  trail.value = [
    ...trail.value,
    { id: item.id, label: item.commonName || item.scientificName },
  ]
  editing.value = null
  await loadList(0)
}

async function hydrateEditing(detail: TaxonDetail) {
  if (detail.mediaTotal > detail.media.length) {
    const all = await fetchTaxonMedia(detail.id, 0, Math.min(Number(detail.mediaTotal), 100))
    editing.value = { ...detail, media: all.items }
  } else {
    editing.value = detail
  }
  form.rank = editing.value.rank
  form.scientificName = editing.value.scientificName
  form.commonName = editing.value.commonName || ''
  form.summary = editing.value.summary || ''
  form.description = editing.value.description || ''
  mediaCaption.value = ''
  captionDrafts.value = Object.fromEntries(
    (editing.value.media ?? []).map((m) => [m.id, m.caption || '']),
  )
}

async function startEdit(id: number) {
  try {
    const detail = await fetchTaxonDetail(id, apiLocale.value, undefined, ADMIN_VIEW_FULL)
    await hydrateEditing(detail)
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  }
}

/** 从搜索结果跳转：定位到其父级列表并打开编辑 */
async function jumpFromSearch(item: TaxonListItem) {
  try {
    const detail = await fetchTaxonDetail(item.id, apiLocale.value, undefined, ADMIN_VIEW_FULL)
    const crumbs: Crumb[] = [{ id: null, label: t('admin.root') }]
    for (const c of detail.breadcrumbs) {
      if (c.id === detail.id) continue
      crumbs.push({ id: c.id, label: c.commonName || c.scientificName })
    }
    trail.value = crumbs
    await loadList(0)
    await hydrateEditing(detail)
    searchQuery.value = ''
    searchHits.value = []
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  }
}

function resetForm() {
  editing.value = null
  form.scientificName = ''
  form.commonName = ''
  form.summary = ''
  form.description = ''
  form.rank = parentId.value == null ? 'KINGDOM' : 'PHYLUM'
  mediaCaption.value = ''
  captionDrafts.value = {}
  moveQuery.value = ''
  moveHits.value = []
}

async function onSubmit() {
  error.value = ''
  message.value = ''
  try {
    if (editing.value) {
      const updated = await updateTaxon(editing.value.id, {
        scientificName: form.scientificName,
        locale: apiLocale.value,
        commonName: form.commonName,
        summary: form.summary,
        description: form.description,
      })
      await hydrateEditing(updated)
      message.value = t('admin.updated')
      toast.push(t('admin.updated'), 'ok')
    } else {
      await createTaxon({
        parentId: parentId.value,
        rank: form.rank,
        scientificName: form.scientificName,
        locale: apiLocale.value,
        commonName: form.commonName,
        summary: form.summary,
        description: form.description,
      })
      message.value = t('admin.created')
      toast.push(t('admin.created'), 'ok')
      resetForm()
    }
    await loadList(listPage.value)
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  }
}

async function onMoveHere() {
  if (!editing.value || parentId.value == null) return
  await performMove(parentId.value)
}

async function performMove(targetParentId: number) {
  if (!editing.value) return
  moving.value = true
  error.value = ''
  message.value = ''
  try {
    const updated = await moveTaxon(editing.value.id, targetParentId, apiLocale.value)
    message.value = t('admin.moved')
    toast.push(t('admin.moved'), 'ok')
    moveQuery.value = ''
    moveHits.value = []
    await jumpFromSearch({
      id: updated.id,
      rank: updated.rank,
      scientificName: updated.scientificName,
      commonName: updated.commonName,
      childCount: updated.childCount,
      hasChildren: (updated.directChildCount ?? updated.childCount) > 0,
      directChildCount: updated.directChildCount ?? updated.childCount,
      rankRaw: updated.rankRaw,
    })
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  } finally {
    moving.value = false
  }
}

async function runAdminSearch() {
  const q = searchQuery.value.trim()
  if (q.length < 2) {
    searchHits.value = []
    return
  }
  searching.value = true
  try {
    const result = await searchTaxa(q, apiLocale.value, 0, SEARCH_SIZE)
    searchHits.value = result.items
  } catch (e) {
    error.value = messageFromApiError(e)
  } finally {
    searching.value = false
  }
}

const debouncedAdminSearch = debounce(() => {
  void runAdminSearch()
}, 320)

async function runMoveSearch() {
  const q = moveQuery.value.trim()
  if (q.length < 2) {
    moveHits.value = []
    return
  }
  try {
    const result = await searchTaxa(q, apiLocale.value, 0, SEARCH_SIZE)
    moveHits.value = result.items.filter((h) => h.id !== editing.value?.id)
  } catch (e) {
    error.value = messageFromApiError(e)
  }
}

const debouncedMoveSearch = debounce(() => {
  void runMoveSearch()
}, 320)

function askDeleteTaxon(id: number) {
  confirmKind.value = 'taxon'
  confirmTargetId.value = id
  confirmOpen.value = true
}

function askDeleteMedia(mediaId: number) {
  confirmKind.value = 'media'
  confirmTargetId.value = mediaId
  confirmOpen.value = true
}

async function onConfirmDelete() {
  const id = confirmTargetId.value
  confirmOpen.value = false
  if (id == null) return
  if (confirmKind.value === 'taxon') {
    try {
      await deleteTaxon(id)
      message.value = t('admin.deleted')
      toast.push(t('admin.deleted'), 'ok')
      if (editing.value?.id === id) resetForm()
      await loadList(listPage.value)
    } catch (e) {
      error.value = messageFromApiError(e)
      toast.push(error.value, 'error')
    }
    return
  }
  if (!editing.value) return
  try {
    await deleteTaxonMedia(editing.value.id, id)
    await startEdit(editing.value.id)
    message.value = t('admin.mediaDeleted')
    toast.push(t('admin.mediaDeleted'), 'ok')
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  }
}

async function onUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !editing.value) return
  uploading.value = true
  error.value = ''
  try {
    await uploadTaxonMedia(editing.value.id, file, {
      locale: apiLocale.value,
      caption: mediaCaption.value || undefined,
    })
    await startEdit(editing.value.id)
    message.value = t('admin.mediaUploaded')
    toast.push(t('admin.mediaUploaded'), 'ok')
    mediaCaption.value = ''
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function saveCaption(mediaId: number) {
  if (!editing.value) return
  try {
    const caption = captionDrafts.value[mediaId] ?? ''
    const updated = await updateTaxonMedia(editing.value.id, mediaId, { caption })
    editing.value = {
      ...editing.value,
      media: editing.value.media.map((m) => (m.id === mediaId ? updated : m)),
    }
    toast.push(t('admin.mediaUpdated'), 'ok')
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  }
}

async function bumpSort(mediaId: number, delta: number) {
  if (!editing.value) return
  const media = editing.value.media.find((m) => m.id === mediaId)
  if (!media) return
  const next = (media.sortOrder ?? 0) + delta
  try {
    const updated = await updateTaxonMedia(editing.value.id, mediaId, { sortOrder: next })
    const list = editing.value.media
      .map((m) => (m.id === mediaId ? updated : m))
      .slice()
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    editing.value = { ...editing.value, media: list }
    toast.push(t('admin.mediaUpdated'), 'ok')
  } catch (e) {
    error.value = messageFromApiError(e)
    toast.push(error.value, 'error')
  }
}

onMounted(async () => {
  resetForm()
  await loadList(0)
})

watch(apiLocale, () => loadList(listPage.value))
watch(previewSimple, () => loadList(0))
watch(searchQuery, () => debouncedAdminSearch())
watch(moveQuery, () => debouncedMoveSearch())
</script>

<template>
  <section class="admin-taxa">
    <header>
      <h1>{{ t('admin.taxaTitle') }}</h1>
      <p>{{ t('admin.taxaSubtitle') }}</p>
    </header>

    <div class="search-bar panel">
      <label>
        <span>{{ t('admin.searchTaxa') }}</span>
        <BtInput v-model="searchQuery" :placeholder="t('admin.searchPlaceholder')" />
      </label>
      <p v-if="searching" class="muted">{{ t('common.loading') }}</p>
      <ul v-if="searchHits.length" class="hit-list">
        <li v-for="hit in searchHits" :key="hit.id">
          <button type="button" class="linkish" @click="jumpFromSearch(hit)">
            <strong>{{ hit.scientificName }}</strong>
            <span>{{ hit.commonName || rankLabel(hit.rank) }}</span>
          </button>
        </li>
      </ul>
    </div>

    <nav class="path" aria-label="breadcrumb">
      <template v-for="(crumb, index) in trail" :key="`${crumb.id}-${index}`">
        <span v-if="index > 0"> / </span>
        <button type="button" class="linkish" @click="jumpToCrumb(index)">
          {{ crumb.label }}
        </button>
      </template>
    </nav>

    <label class="preview-toggle">
      <input v-model="previewSimple" type="checkbox" />
      <span>{{ t('admin.previewSimple') }}</span>
      <em>{{ t('admin.previewSimpleHint') }}</em>
    </label>

    <div class="grid">
      <div class="panel">
        <h2>{{ t('admin.children') }}</h2>
        <p class="muted">
          {{ t('admin.pageInfo', { page: listPage + 1, total: listTotal }) }}
        </p>
        <ul>
          <li v-for="item in items" :key="item.id">
            <div class="item-main">
              <button type="button" class="linkish" @click="startEdit(item.id)">
                <strong>{{ item.scientificName }}</strong>
                <span>{{ item.commonName || rankLabel(item.rank) }}</span>
              </button>
            </div>
            <div class="item-actions">
              <BtButton variant="ghost" @click="enterChild(item)">
                {{ t('admin.enter') }}
              </BtButton>
              <BtButton variant="danger" @click="askDeleteTaxon(item.id)">{{ t('admin.delete') }}</BtButton>
            </div>
          </li>
        </ul>
        <p v-if="!items.length" class="muted">{{ t('admin.empty') }}</p>
        <BtPagination
          :page="listPage"
          :size="PAGE_SIZE"
          :total="listTotal"
          @update:page="loadList"
        />
      </div>

      <form class="panel form" @submit.prevent="onSubmit">
        <h2>{{ editing ? t('admin.edit') : t('admin.create') }}</h2>
        <p v-if="previewSimple" class="muted">{{ t('admin.previewSimpleHint') }}</p>
        <fieldset :disabled="previewSimple && !editing">
        <label>
          <span>{{ t('admin.rank') }}</span>
          <BtSelect v-model="form.rank" :options="rankOptions" :disabled="!!editing" />
        </label>
        <label>
          <span>{{ t('admin.scientificName') }}</span>
          <BtInput v-model="form.scientificName" :required="true" />
        </label>
        <p v-if="editing?.scientificNameAuthorship" class="muted authorship">
          {{ t('admin.authorship') }}: {{ editing.scientificNameAuthorship }}
        </p>
        <p v-if="editing?.rankRaw" class="muted">
          {{ t('admin.rankRaw') }}: {{ editing.rankRaw }}
        </p>
        <label>
          <span>{{ t('admin.commonName') }}</span>
          <BtInput v-model="form.commonName" />
        </label>
        <label>
          <span>{{ t('admin.summary') }}</span>
          <BtInput v-model="form.summary" />
        </label>
        <label>
          <span>{{ t('admin.description') }}</span>
          <BtTextarea v-model="form.description" :rows="5" />
        </label>
        </fieldset>
        <div class="actions">
          <BtButton type="submit" :disabled="previewSimple && !editing">{{ t('common.save') }}</BtButton>
          <BtButton type="button" variant="ghost" @click="resetForm">{{ t('common.cancel') }}</BtButton>
          <BtButton
            v-if="editing && parentId != null && editing.parentId !== parentId"
            type="button"
            variant="ghost"
            :disabled="moving"
            @click="onMoveHere"
          >
            {{ t('admin.move') }}
          </BtButton>
        </div>
        <p v-if="editing && parentId != null" class="muted">{{ t('admin.moveHint') }}</p>

        <div v-if="editing" class="move-picker">
          <h3>{{ t('admin.moveToSearch') }}</h3>
          <BtInput v-model="moveQuery" :placeholder="t('admin.searchPlaceholder')" />
          <ul v-if="moveHits.length" class="hit-list">
            <li v-for="hit in moveHits" :key="hit.id">
              <button type="button" class="linkish" :disabled="moving" @click="performMove(hit.id)">
                <strong>{{ hit.scientificName }}</strong>
                <span>{{ hit.commonName || rankLabel(hit.rank) }}</span>
              </button>
            </li>
          </ul>
        </div>

        <div v-if="editing" class="media-block">
          <h3>{{ t('admin.media') }}</h3>
          <div class="media-grid">
            <figure v-for="m in editing.media" :key="m.id">
              <img :src="m.url" :alt="m.caption || editing.scientificName" loading="lazy" />
              <figcaption>
                <BtInput v-model="captionDrafts[m.id]" :placeholder="t('admin.mediaCaption')" />
                <div class="media-actions">
                  <BtButton type="button" variant="ghost" @click="saveCaption(m.id)">
                    {{ t('admin.saveCaption') }}
                  </BtButton>
                  <BtButton type="button" variant="ghost" @click="bumpSort(m.id, -1)">↑</BtButton>
                  <BtButton type="button" variant="ghost" @click="bumpSort(m.id, 1)">↓</BtButton>
                  <BtButton type="button" variant="danger" @click="askDeleteMedia(m.id)">
                    {{ t('admin.delete') }}
                  </BtButton>
                </div>
              </figcaption>
            </figure>
          </div>
          <label>
            <span>{{ t('admin.mediaCaption') }}</span>
            <BtInput v-model="mediaCaption" />
          </label>
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            @change="onUpload"
          />
          <p class="muted">{{ t('admin.mediaHint') }}</p>
          <p v-if="uploading" class="muted">{{ t('common.loading') }}</p>
        </div>

        <p v-if="message" class="ok">{{ message }}</p>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>

    <BtDialog
      :open="confirmOpen"
      :title="t('admin.delete')"
      :message="confirmKind === 'taxon' ? t('admin.confirmDelete') : t('admin.confirmDeleteMedia')"
      :confirm-label="t('common.confirm')"
      :cancel-label="t('common.cancel')"
      @confirm="onConfirmDelete"
      @cancel="confirmOpen = false"
    />
  </section>
</template>

<style scoped>
.admin-taxa {
  display: grid;
  gap: var(--space-4);
  animation: rise var(--duration-normal) var(--ease-out);
}

header h1 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
}

header p {
  margin: 0;
  color: var(--color-text-muted);
}

.panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.search-bar label {
  display: grid;
  gap: var(--space-2);
}

.path {
  color: var(--color-text-muted);
}

.preview-toggle {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
  margin: var(--space-3) 0 var(--space-4);
  font-size: var(--text-sm);
}

.preview-toggle em {
  color: var(--color-text-muted);
  font-style: normal;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  gap: var(--space-4);
  align-items: start;
}

ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--space-2);
}

li {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
}

.item-main {
  min-width: 0;
}

.item-actions {
  display: flex;
  gap: var(--space-2);
  flex-shrink: 0;
}

.linkish {
  display: grid;
  gap: 0.15rem;
  text-align: left;
  background: none;
  border: 0;
  padding: 0;
  color: inherit;
  cursor: pointer;
}

.linkish span {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.hit-list {
  margin-top: var(--space-3);
}

.form {
  display: grid;
  gap: var(--space-3);
}

.form label {
  display: grid;
  gap: var(--space-2);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.move-picker,
.media-block {
  display: grid;
  gap: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.move-picker h3,
.media-block h3 {
  margin: 0;
  font-size: var(--text-md);
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
  gap: var(--space-3);
}

.media-grid figure {
  margin: 0;
  display: grid;
  gap: var(--space-2);
}

.media-grid img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.media-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.muted {
  color: var(--color-text-muted);
}

.ok {
  color: var(--color-success, #067647);
}

.error {
  color: var(--color-danger, #b42318);
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }

  li {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
