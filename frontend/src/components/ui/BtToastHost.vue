/**
 * Toast 容器。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
</script>

<template>
  <div class="bt-toast-host" aria-live="polite">
    <div v-for="item in toast.items" :key="item.id" class="bt-toast" :class="`bt-toast--${item.tone}`">
      {{ item.message }}
    </div>
  </div>
</template>

<style scoped>
.bt-toast-host {
  position: fixed;
  right: var(--space-4);
  bottom: var(--space-4);
  z-index: 40;
  display: grid;
  gap: var(--space-2);
  max-width: min(22rem, 90vw);
}

.bt-toast {
  padding: 0.7rem 0.9rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  animation: rise var(--duration-fast) var(--ease-out);
}

.bt-toast--ok {
  border-color: color-mix(in srgb, var(--color-primary) 45%, var(--color-border));
}

.bt-toast--error {
  border-color: color-mix(in srgb, var(--color-danger) 55%, var(--color-border));
  color: var(--color-danger);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
