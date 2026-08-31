<script setup lang="ts">
/**
 * 管理端分类 CRUD 页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 支持编辑态配图上传与删除
 * Updated: 2026-08-31 支持节点移动与设计系统表单控件
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  createTaxon,
  deleteTaxon,
  deleteTaxonMedia,
  fetchChildren,
  fetchTaxonDetail,
  moveTaxon,
  updateTaxon,
  uploadTaxonMedia,
  type TaxonDetail,
  type TaxonListItem,
  type TaxonRank,
} from '@/api/taxon'
import BtButton from '@/components/ui/BtButton.vue'
import BtInput from '@/components/ui/BtInput.vue'
import BtSelect from '@/components/ui/BtSelect.vue'
import { useLocaleStore } from '@/stores/locale'

const RANKS: TaxonRank[] = ['KINGDOM', 'PHYLUM', 'CLASS', 'ORDER', 'FAMILY', 'GENUS', 'SPECIES']

const { t } = useI18n()
const localeStore = useLocaleStore()
const apiLocale = computed(() => localeStore.locale)
const rankOptions = computed(() => RANKS.map((rank) => ({ value: rank, label: rank })))

const parentId = ref<number | null>(null)
const parentLabel = ref(t('admin.root'))
const items = ref<TaxonListItem[]>([])
const editing = ref<TaxonDetail | null>(null)
const error = ref('')
const message = ref('')
const uploading = ref(false)
const mediaCaption = ref('')

const form = reactive({
  rank: 'KINGDOM' as TaxonRank,
  scientificName: '',
  commonName: '',
  summary: '',
  description: '',
})

async function loadList() {
  error.value = ''
  const page = await fetchChildren(parentId.value, apiLocale.value)
  items.value = page.items
}

async function openParent(id: number | null, label: string) {
  parentId.value = id
  parentLabel.value = label
  editing.value = null
  await loadList()
}

async function startEdit(id: number) {
  editing.value = await fetchTaxonDetail(id, apiLocale.value)
  form.rank = editing.value.rank
  form.scientificName = editing.value.scientificName
  form.commonName = editing.value.commonName || ''
  form.summary = editing.value.summary || ''
  form.description = editing.value.description || ''
  mediaCaption.value = ''
}

function resetForm() {
  editing.value = null
  form.scientificName = ''
  form.commonName = ''
  form.summary = ''
  form.description = ''
  form.rank = parentId.value == null ? 'KINGDOM' : 'PHYLUM'
  mediaCaption.value = ''
}

async function onSubmit() {
  error.value = ''
  message.value = ''
  try {
    if (editing.value) {
      editing.value = await updateTaxon(editing.value.id, {
        scientificName: form.scientificName,
        locale: apiLocale.value,
        commonName: form.commonName,
        summary: form.summary,
        description: form.description,
      })
      message.value = t('admin.updated')
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
      resetForm()
    }
    await loadList()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  }
}

async function onMoveHere() {
  if (!editing.value || parentId.value == null) {
    return
  }
  error.value = ''
  message.value = ''
  try {
    editing.value = await moveTaxon(editing.value.id, parentId.value, apiLocale.value)
    message.value = t('admin.moved')
    await loadList()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  }
}

async function onDelete(id: number) {
  if (!window.confirm(t('admin.confirmDelete'))) {
    return
  }
  try {
    await deleteTaxon(id)
    message.value = t('admin.deleted')
    if (editing.value?.id === id) {
      resetForm()
    }
    await loadList()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  }
}

async function onUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !editing.value) {
    return
  }
  uploading.value = true
  error.value = ''
  try {
    await uploadTaxonMedia(editing.value.id, file, {
      locale: apiLocale.value,
      caption: mediaCaption.value || undefined,
    })
    editing.value = await fetchTaxonDetail(editing.value.id, apiLocale.value)
    message.value = t('admin.mediaUploaded')
    mediaCaption.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function onDeleteMedia(mediaId: number) {
  if (!editing.value) {
    return
  }
  if (!window.confirm(t('admin.confirmDeleteMedia'))) {
    return
  }
  try {
    await deleteTaxonMedia(editing.value.id, mediaId)
    editing.value = await fetchTaxonDetail(editing.value.id, apiLocale.value)
    message.value = t('admin.mediaDeleted')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'failed'
  }
}

onMounted(async () => {
  resetForm()
  await loadList()
})

watch(apiLocale, loadList)
</script>

<template>
  <section class="admin-taxa">
    <header>
      <h1>{{ t('admin.taxaTitle') }}</h1>
      <p>{{ t('admin.taxaSubtitle') }}</p>
    </header>

    <p class="path">
      {{ t('admin.currentParent') }}:
      <button type="button" class="linkish" @click="openParent(null, t('admin.root'))">
        {{ t('admin.root') }}
      </button>
      <span v-if="parentId != null"> / {{ parentLabel }}</span>
    </p>

    <div class="grid">
      <div class="panel">
        <h2>{{ t('admin.children') }}</h2>
        <ul>
          <li v-for="item in items" :key="item.id">
            <div class="item-main">
              <button type="button" class="linkish" @click="startEdit(item.id)">
                <strong>{{ item.scientificName }}</strong>
                <span>{{ item.commonName || item.rank }}</span>
              </button>
            </div>
            <div class="item-actions">
              <BtButton
                variant="ghost"
                @click="openParent(item.id, item.commonName || item.scientificName)"
              >
                {{ t('admin.enter') }}
              </BtButton>
              <BtButton variant="danger" @click="onDelete(item.id)">{{ t('admin.delete') }}</BtButton>
            </div>
          </li>
        </ul>
        <p v-if="!items.length" class="muted">{{ t('admin.empty') }}</p>
      </div>

      <form class="panel form" @submit.prevent="onSubmit">
        <h2>{{ editing ? t('admin.edit') : t('admin.create') }}</h2>
        <label>
          <span>{{ t('admin.rank') }}</span>
          <BtSelect v-model="form.rank" :options="rankOptions" :disabled="!!editing" />
        </label>
        <label>
          <span>{{ t('admin.scientificName') }}</span>
          <BtInput v-model="form.scientificName" :required="true" />
        </label>
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
          <textarea v-model="form.description" rows="5" />
        </label>
        <div class="actions">
          <BtButton type="submit">{{ t('common.save') }}</BtButton>
          <BtButton type="button" variant="ghost" @click="resetForm">{{ t('common.cancel') }}</BtButton>
          <BtButton
            v-if="editing && parentId != null && editing.parentId !== parentId"
            type="button"
            variant="ghost"
            @click="onMoveHere"
          >
            {{ t('admin.move') }}
          </BtButton>
        </div>
        <p v-if="editing && parentId != null" class="muted">{{ t('admin.moveHint') }}</p>

        <div v-if="editing" class="media-block">
          <h3>{{ t('admin.media') }}</h3>
          <div class="media-grid">
            <figure v-for="m in editing.media" :key="m.id">
              <img :src="m.url" :alt="m.caption || editing.scientificName" loading="lazy" />
              <figcaption>
                <span>{{ m.caption || t('admin.mediaNoCaption') }}</span>
                <BtButton type="button" variant="danger" @click="onDeleteMedia(m.id)">
                  {{ t('admin.delete') }}
                </BtButton>
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

header p,
.muted,
.path {
  color: var(--color-text-muted);
}

.grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: var(--space-4);
}

.panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
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
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.item-main button {
  display: grid;
  gap: 0.15rem;
  text-align: left;
}

.item-actions {
  display: flex;
  gap: var(--space-2);
}

.linkish {
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 0;
}

.form {
  display: grid;
  gap: var(--space-3);
  align-content: start;
}

label {
  display: grid;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text);
  padding: 0.55rem 0.7rem;
  font: inherit;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.media-block {
  display: grid;
  gap: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.media-block h3 {
  margin: 0;
  font-size: var(--text-md);
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: var(--space-3);
}

figure {
  margin: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg);
}

figure img {
  display: block;
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}

figcaption {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-2);
  font-size: var(--text-xs);
}

.ok {
  color: var(--color-primary);
}

.error {
  color: var(--color-danger);
}

@media (max-width: 900px) {
  .grid {
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
