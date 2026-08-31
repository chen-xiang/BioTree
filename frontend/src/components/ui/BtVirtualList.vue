/**
 * 简易虚拟列表：仅渲染可视窗口附近的行。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts" generic="T">
import { computed, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    items: T[]
    itemHeight?: number
    height?: number
    overscan?: number
  }>(),
  {
    itemHeight: 44,
    height: 320,
    overscan: 6,
  },
)

const root = ref<HTMLElement | null>(null)
const scrollTop = ref(0)

const totalHeight = computed(() => props.items.length * props.itemHeight)
const startIndex = computed(() =>
  Math.max(0, Math.floor(scrollTop.value / props.itemHeight) - props.overscan),
)
const endIndex = computed(() =>
  Math.min(
    props.items.length,
    Math.ceil((scrollTop.value + props.height) / props.itemHeight) + props.overscan,
  ),
)
const visible = computed(() =>
  props.items.slice(startIndex.value, endIndex.value).map((item, i) => ({
    item,
    index: startIndex.value + i,
    top: (startIndex.value + i) * props.itemHeight,
  })),
)

function onScroll(e: Event) {
  scrollTop.value = (e.target as HTMLElement).scrollTop
}

onMounted(() => {
  if (root.value) scrollTop.value = root.value.scrollTop
})
</script>

<template>
  <div
    ref="root"
    class="bt-virtual"
    :style="{ height: `${height}px` }"
    @scroll.passive="onScroll"
  >
    <div class="bt-virtual__spacer" :style="{ height: `${totalHeight}px` }">
      <div
        v-for="row in visible"
        :key="row.index"
        class="bt-virtual__row"
        :style="{ top: `${row.top}px`, height: `${itemHeight}px` }"
      >
        <slot :item="row.item" :index="row.index" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.bt-virtual {
  overflow: auto;
  position: relative;
  width: 100%;
}

.bt-virtual__spacer {
  position: relative;
  width: 100%;
}

.bt-virtual__row {
  position: absolute;
  left: 0;
  right: 0;
}
</style>
