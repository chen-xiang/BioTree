<script setup lang="ts">
/**
 * 管理后台布局骨架。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加分类管理侧栏入口
 */
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import AppTopbar from '@/components/layout/AppTopbar.vue'

const { t } = useI18n()
</script>

<template>
  <div class="layout">
    <AppTopbar />
    <div class="shell">
      <aside class="side">
        <p class="side-title">{{ t('admin.title') }}</p>
        <RouterLink class="side-link" to="/admin">{{ t('nav.admin') }}</RouterLink>
        <RouterLink class="side-link" to="/admin/taxa">{{ t('admin.taxaNav') }}</RouterLink>
      </aside>
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.shell {
  display: grid;
  grid-template-columns: 220px 1fr;
  min-height: calc(100vh - 4.5rem);
}

.side {
  border-right: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-bg-elevated) 88%, transparent);
  padding: var(--space-5);
}

.side-title {
  margin: 0 0 var(--space-4);
  font-family: var(--font-display);
  font-size: var(--text-lg);
}

.side-link {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--color-text-muted);
  transition: color var(--duration-fast) var(--ease-out);
}

.side-link.router-link-active,
.side-link:hover {
  color: var(--color-text);
}

.main {
  padding: var(--space-6) var(--space-5);
}

@media (max-width: 860px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .side {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }
}
</style>
