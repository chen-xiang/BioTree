<script setup lang="ts">
/**
 * 管理后台首页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 引导进入分类管理
 */
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import BtButton from '@/components/ui/BtButton.vue'

const { t } = useI18n()
</script>

<template>
  <section class="panel">
    <h1>{{ t('admin.title') }}</h1>
    <p>{{ t('admin.placeholder') }}</p>
    <RouterLink to="/admin/taxa">
      <BtButton>{{ t('admin.taxaNav') }}</BtButton>
    </RouterLink>
  </section>
</template>

<style scoped>
.panel {
  animation: rise var(--duration-normal) var(--ease-out);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  display: grid;
  gap: var(--space-4);
  justify-items: start;
}

h1 {
  margin: 0;
  font-family: var(--font-display);
}

p {
  margin: 0;
  color: var(--color-text-muted);
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
