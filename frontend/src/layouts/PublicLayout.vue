/**
 * 公开浏览布局。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-09-01 按路由调整主栏宽度：首页铺满、浏览加宽
 * Updated: 2026-09-01 浏览页锁定视口高度，只让树/详情各自滚动
 */
<script setup lang="ts">
/**
 * 公开浏览布局。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppTopbar from '@/components/layout/AppTopbar.vue'

const route = useRoute()
const page = computed(() => {
  if (route.name === 'home') return 'home'
  if (route.name === 'browse') return 'browse'
  return 'narrow'
})
</script>

<template>
  <div class="layout">
    <AppTopbar />
    <main class="main" :data-page="page">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout:has(> .main[data-page='browse']) {
  height: 100vh;
  overflow: hidden;
}

.main {
  flex: 1;
  width: min(42rem, 100%);
  margin: 0 auto;
  padding: var(--space-6) var(--space-5);
}

.main[data-page='home'] {
  width: min(68rem, 100%);
  padding: var(--space-5) var(--space-5) var(--space-8);
}

.main[data-page='browse'] {
  width: min(86rem, 100%);
  padding: var(--space-5);
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 860px) {
  .layout:has(> .main[data-page='browse']) {
    height: auto;
    overflow: visible;
  }

  .main[data-page='browse'] {
    overflow: visible;
  }
}
</style>
