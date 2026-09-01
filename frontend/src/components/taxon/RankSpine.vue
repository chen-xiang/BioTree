<script setup lang="ts">
/**
 * 林奈七级脊柱：首页签名视觉，也用于浏览空态。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
import { computed } from 'vue'
import type { RankSpineItem } from '@/domain/ranks'
import { buildRankSpine } from '@/domain/ranks'
import { rankLabel } from '@/utils/apiError'

const props = withDefaults(
  defineProps<{
    counts?: Record<string, number> | null
    compact?: boolean
    caption?: string
  }>(),
  {
    counts: null,
    compact: false,
    caption: '',
  },
)

const items = computed<RankSpineItem[]>(() => buildRankSpine(props.counts))

function formatCount(n: number) {
  return n.toLocaleString()
}
</script>

<template>
  <figure class="spine" :class="{ compact }">
    <figcaption v-if="caption" class="caption">{{ caption }}</figcaption>
    <ol>
      <li v-for="item in items" :key="item.rank">
        <span class="dot" />
        <span class="label">{{ rankLabel(item.rank) }}</span>
        <span v-if="item.count != null" class="count">{{ formatCount(item.count) }}</span>
      </li>
    </ol>
  </figure>
</template>

<style scoped>
.spine {
  margin: 0;
  color: var(--color-text);
}

.caption {
  margin: 0 0 var(--space-4);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

ol {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0;
}

li {
  display: grid;
  grid-template-columns: 0.85rem minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  position: relative;
  padding: 0.55rem 0 0.55rem 0.15rem;
  animation: draw var(--duration-normal) var(--ease-out) both;
}

li:nth-child(1) {
  animation-delay: 40ms;
}
li:nth-child(2) {
  animation-delay: 80ms;
}
li:nth-child(3) {
  animation-delay: 120ms;
}
li:nth-child(4) {
  animation-delay: 160ms;
}
li:nth-child(5) {
  animation-delay: 200ms;
}
li:nth-child(6) {
  animation-delay: 240ms;
}
li:nth-child(7) {
  animation-delay: 280ms;
}

li:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 0.42rem;
  top: calc(50% + 0.35rem);
  bottom: -0.35rem;
  width: 1px;
  background: color-mix(in srgb, var(--color-primary) 45%, var(--color-border));
}

.dot {
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 16%, transparent);
}

li:last-child .dot {
  background: var(--color-accent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-accent) 16%, transparent);
}

.label {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 650;
  letter-spacing: 0.02em;
}

.count {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-muted);
}

.compact li {
  padding: 0.28rem 0;
}

.compact .label {
  font-size: var(--text-sm);
  font-weight: 500;
}

.compact .caption {
  margin-bottom: var(--space-2);
}

@keyframes draw {
  from {
    opacity: 0;
    transform: translateX(-6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 860px) {
  .spine:not(.compact) li {
    padding: 0.38rem 0;
  }

  .spine:not(.compact) .label {
    font-size: var(--text-md);
  }

  .spine:not(.compact) .count {
    font-size: var(--text-xs);
  }
}
</style>
