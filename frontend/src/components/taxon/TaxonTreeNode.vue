<script setup lang="ts">
/**
 * 懒加载分类树节点行。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { ref, watch } from 'vue'
import type { TaxonListItem } from '@/api/taxon'
import { fetchChildren } from '@/api/taxon'

const props = defineProps<{
  node: TaxonListItem
  depth: number
  locale: string
  selectedId: number | null
}>()

const emit = defineEmits<{
  select: [id: number]
}>()

const expanded = ref(false)
const loading = ref(false)
const children = ref<TaxonListItem[]>([])
const loaded = ref(false)

async function toggle() {
  if (!props.node.hasChildren) {
    emit('select', props.node.id)
    return
  }
  expanded.value = !expanded.value
  if (expanded.value && !loaded.value) {
    loading.value = true
    try {
      const page = await fetchChildren(props.node.id, props.locale)
      children.value = page.items
      loaded.value = true
    } finally {
      loading.value = false
    }
  }
  emit('select', props.node.id)
}

watch(
  () => props.locale,
  () => {
    loaded.value = false
    children.value = []
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
      <span class="rank">{{ node.rank }}</span>
    </button>
    <p v-if="loading" class="hint">…</p>
    <div v-if="expanded && children.length" class="kids">
      <TaxonTreeNode
        v-for="child in children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :locale="locale"
        :selected-id="selectedId"
        @select="emit('select', $event)"
      />
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
</style>
