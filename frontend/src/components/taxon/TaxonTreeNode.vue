/**
 * 懒加载分类树节点行：展开拉子节点，触底自动续页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 子节点分页与加载更多
 * Updated: 2026-08-31 子节点较多时使用虚拟列表
 * Updated: 2026-08-31 支持 view=simple|full
 * Updated: 2026-09-01 选中态左边线与子级分支轨
 * Updated: 2026-09-01 去掉树内嵌套虚拟列表与「加载更多」，改为触底续页
 * Updated: 2026-09-01 选中/悬停背景随层级缩进，不再铺满整行
 * Updated: 2026-09-01 高亮限制在父级竖线内侧
 * Updated: 2026-09-01 收紧展开箭头两侧空隙
 * Updated: 2026-09-01 节点行之间增加细间距
 */
<script setup lang="ts">
import { inject, nextTick, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TaxonListItem, TaxonView } from '@/api/taxon'
import { TAXON_TREE_SCROLL_ROOT, useTaxonChildren } from '@/composables/useTaxonChildren'
import { rankLabel } from '@/utils/apiError'

const props = defineProps<{
  node: TaxonListItem
  depth: number
  locale: string
  selectedId: number | null
  view?: TaxonView
}>()

const emit = defineEmits<{
  select: [id: number]
}>()

const { t } = useI18n()
const scrollRoot = inject<Ref<HTMLElement | null>>(TAXON_TREE_SCROLL_ROOT, ref(null))
const expanded = ref(false)
const sentinel = ref<HTMLElement | null>(null)
const activeView = () => props.view ?? 'simple'

const {
  children,
  hasMore,
  loading,
  loadingMore,
  loadFirstPage,
  loadMore,
  reset,
} = useTaxonChildren({
  parentId: () => props.node.id,
  locale: () => props.locale,
  view: activeView,
})

let observer: IntersectionObserver | null = null

async function toggle() {
  if (!props.node.hasChildren) {
    emit('select', props.node.id)
    return
  }
  expanded.value = !expanded.value
  if (expanded.value) {
    await loadFirstPage()
    await nextTick()
    observeSentinel()
    await continueIfSentinelVisible()
  }
  emit('select', props.node.id)
}

function observeSentinel() {
  observer?.disconnect()
  observer = null
  const target = sentinel.value
  if (!target || !expanded.value || !hasMore.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        void continueIfSentinelVisible()
      }
    },
    {
      root: scrollRoot.value,
      rootMargin: '120px 0px',
      threshold: 0,
    },
  )
  observer.observe(target)
}

async function continueIfSentinelVisible() {
  if (!expanded.value || !hasMore.value) return
  const root = scrollRoot.value
  const target = sentinel.value
  if (!target) return
  const visible = isNearScrollRoot(target, root)
  if (!visible) return
  const added = await loadMore()
  if (added > 0) {
    await nextTick()
    observeSentinel()
    await continueIfSentinelVisible()
  }
}

function isNearScrollRoot(target: HTMLElement, root: HTMLElement | null) {
  const targetBox = target.getBoundingClientRect()
  if (root) {
    const rootBox = root.getBoundingClientRect()
    return targetBox.top <= rootBox.bottom + 120
  }
  return targetBox.top <= window.innerHeight + 120
}

function resetChildrenCache() {
  observer?.disconnect()
  observer = null
  reset()
  expanded.value = false
}

watch(() => props.locale, resetChildrenCache)
watch(() => props.view, resetChildrenCache)
watch(hasMore, async (more) => {
  if (!more) {
    observer?.disconnect()
    observer = null
    return
  }
  await nextTick()
  observeSentinel()
})

onBeforeUnmount(() => {
  observer?.disconnect()
})
</script>

<template>
  <div class="node">
    <button
      class="row"
      :class="{ active: selectedId === node.id }"
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
    <p v-if="loading" class="hint">{{ t('common.loading') }}</p>
    <div v-if="expanded" class="kids">
      <TaxonTreeNode
        v-for="child in children"
        :key="`${child.id}-${activeView()}`"
        :node="child"
        :depth="depth + 1"
        :locale="locale"
        :view="activeView()"
        :selected-id="selectedId"
        @select="emit('select', $event)"
      />
      <div v-if="hasMore" ref="sentinel" class="sentinel">
        {{ loadingMore ? t('common.loading') : t('browse.loadingMore') }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.node {
  --rail: 0.72rem;
  --rail-gap: 0.28rem;
}

.row {
  display: grid;
  grid-template-columns: 0.7rem minmax(0, 1fr) auto;
  gap: 0.28rem;
  align-items: center;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
  width: 100%;
  margin-bottom: 0.18rem;
  padding: 0.38rem 0.5rem 0.38rem 0.38rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  position: relative;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.row:hover {
  background: var(--color-bg-muted);
}

.row.active {
  background: color-mix(in srgb, var(--color-primary) 10%, var(--color-bg-elevated));
}

.row.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.3rem;
  bottom: 0.3rem;
  width: 2px;
  background: var(--color-primary);
  border-radius: 1px;
}

.chevron {
  display: grid;
  place-items: center;
  width: 0.7rem;
  line-height: 1;
  font-size: 0.7rem;
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
  font-family: var(--font-display);
  font-style: italic;
  font-weight: 650;
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
  letter-spacing: 0.02em;
}

.kids {
  position: relative;
  margin-left: var(--rail);
  padding-left: var(--rail-gap);
}

.kids::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0.35rem;
  width: 1px;
  background: color-mix(in srgb, var(--color-primary) 28%, var(--color-border));
  pointer-events: none;
}

.hint,
.sentinel {
  margin: 0;
  padding: 0.35rem 0.7rem;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
</style>
