/**
 * 分页控件。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
import { computed } from 'vue'
import BtButton from './BtButton.vue'

const props = defineProps<{
  page: number
  size: number
  total: number
}>()

const emit = defineEmits<{
  'update:page': [page: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / Math.max(props.size, 1))))
const canPrev = computed(() => props.page > 0)
const canNext = computed(() => props.page + 1 < totalPages.value)
</script>

<template>
  <div class="bt-pagination" v-if="total > size">
    <BtButton variant="ghost" :disabled="!canPrev" @click="emit('update:page', page - 1)">‹</BtButton>
    <span class="meta">{{ page + 1 }} / {{ totalPages }}</span>
    <BtButton variant="ghost" :disabled="!canNext" @click="emit('update:page', page + 1)">›</BtButton>
  </div>
</template>

<style scoped>
.bt-pagination {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.meta {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}
</style>
