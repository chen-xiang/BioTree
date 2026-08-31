/**
 * 确认对话框。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
import BtButton from './BtButton.vue'

defineProps<{
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <div v-if="open" class="bt-dialog-backdrop" @click.self="emit('cancel')">
    <div class="bt-dialog" role="dialog" aria-modal="true">
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
      <div class="actions">
        <BtButton variant="ghost" type="button" @click="emit('cancel')">
          {{ cancelLabel || 'Cancel' }}
        </BtButton>
        <BtButton variant="danger" type="button" @click="emit('confirm')">
          {{ confirmLabel || 'OK' }}
        </BtButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bt-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 30;
  background: rgb(10 20 16 / 0.45);
  display: grid;
  place-items: center;
  padding: var(--space-4);
}

.bt-dialog {
  width: min(24rem, 100%);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-md);
  animation: rise var(--duration-normal) var(--ease-out);
}

h3 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
}

p {
  margin: 0 0 var(--space-4);
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
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
