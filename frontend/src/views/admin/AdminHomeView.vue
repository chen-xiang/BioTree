/**
 * 管理后台占位页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
/**
 * 管理后台占位页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <section class="panel">
    <h1>{{ t('admin.title') }}</h1>
    <p>{{ t('admin.placeholder') }}</p>
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
}

h1 {
  margin: 0 0 var(--space-3);
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
