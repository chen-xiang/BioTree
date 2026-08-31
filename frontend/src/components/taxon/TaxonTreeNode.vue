/**
 * 懒加载分类树节点行（支持子节点分页与虚拟滚动）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 子节点分页与加载更多
 * Updated: 2026-08-31 子节点较多时使用虚拟列表
 */
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TaxonListItem } from '@/api/taxon'
import { fetchChildren } from '@/api/taxon'
import BtButton from '@/components/ui/BtButton.vue'
import BtVirtualList from '@/components/ui/BtVirtualList.vue'
import { rankLabel } from '@/utils/apiError'

const props = defineProps<{
  node: TaxonListItem
  depth: number
  locale: string
  selectedId: number | null
}>()

const emit = defineEmits<{
  select: [id: number]
}>()

const { t } = useI18n()
const PAGE_SIZE = 40
const VIRTUAL_THRESHOLD = 40

const expanded = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const children = ref<TaxonListItem[]>([])
const page = ref(0)
const total = ref(0)
const loaded = ref(false)

const hasMore = computed(() => children.value.length < total.value)
const useVirtual = computed(() => children.value.length >= VIRTUAL_THRESHOLD)

async function loadPage(nextPage: number, append: boolean) {
  const result = await fetchChildren(props.node.id, props.locale, nextPage, PAGE_SIZE)
  total.value = result.total
  page.value = result.page
  children.value = append ? [...children.value, ...result.items] : result.items
  loaded.value = true
}

async function toggle() {
  if (!props.node.hasChildren) {
    emit('select', props.node.id)
    return
  }
  expanded.value = !expanded.value
  if (expanded.value && !loaded.value) {
    loading.value = true
    try {
      await loadPage(0, false)
    } finally {
      loading.value = false
    }
  }
  emit('select', props.node.id)
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    await loadPage(page.value + 1, true)
  } finally {
    loadingMore.value = false
  }
}

watch(
  () => props.locale,
  () => {
    loaded.value = false
    children.value = []
    page.value = 0
    total.value = 0
    if (expanded.value) {
      expanded.value = false
    }
  },
)
</script>

<template>
  <div class="node">
    <button
      class="row"
      :class="{ active: selectedId === node.id }"
      :style="{ paddingLeft: `${0.75 + depth * 0.9}rem` }"
      type="button"
      @click="toggle"
    >
      <span class="chevron" :class="{ open: expanded, hidden: !node.hasChildren }">▸</span>
      <span class="names">
        <strong>{{ node.scientificName }}</strong>
        <em v-if="node.commonName">{{ node.commonName }}</em>
      </span>
      <span class="rank">{{ rankLabel(node.rank) }}</span>
    </button>
    <p v-if="loading" class="hint">…</p>
    <div v-if="expanded" class="kids">
      <BtVirtualList
        v-if="useVirtual"
        :items="children"
        :item-height="48"
        :height="Math.min(360, children.length * 48)"
      >
        <template #default="{ item }">
          <TaxonTreeNode
            :node="item"
            :depth="depth + 1"
            :locale="locale"
            :selected-id="selectedId"
            @select="emit('select', $event)"
          />
        </template>
      </BtVirtualList>
      <template v-else>
        <TaxonTreeNode
          v-for="child in children"
          :key="child.id"
          :node="child"
          :depth="depth + 1"
          :locale="locale"
          :selected-id="selectedId"
          @select="emit('select', $event)"
        />
      </template>
      <div v-if="hasMore" class="more" :style="{ paddingLeft: `${1.2 + depth * 0.9}rem` }">
        <BtButton variant="ghost" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? t('common.loading') : t('browse.loadMore') }}
        </BtButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.row {
  width: 100%;
  display: grid;
  grid-template-columns: 1.2rem 1fr auto;
  gap: var(--space-2);
  align-items: center;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
  padding: 0.45rem 0.75rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.row:hover,
.row.active {
  background: var(--color-bg-muted);
}

.chevron {
  transition: transform var(--duration-fast) var(--ease-out);
  color: var(--color-text-muted);
}

.chevron.open {
  transform: rotate(90deg);
}

.chevron.hidden {
  visibility: hidden;
}

.names {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}

.names strong {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.names em {
  font-style: normal;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.rank {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-family: var(--font-mono);
}

.hint {
  margin: 0;
  padding-left: 2rem;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.more {
  margin-top: var(--space-1);
}
</style>
