/**
 * 公开浏览布局。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
/**
 * 公开浏览布局。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import AppTopbar from '@/components/layout/AppTopbar.vue'
</script>

<template>
  <div class="layout">
    <AppTopbar />
    <main class="main">
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

.main {
  flex: 1;
  width: min(1100px, 100%);
  margin: 0 auto;
  padding: var(--space-6) var(--space-5);
}
</style>
