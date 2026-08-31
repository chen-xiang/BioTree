/**
 * 基础按钮组件。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
/**
 * 基础按钮组件。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
withDefaults(
  defineProps<{
    variant?: 'primary' | 'ghost' | 'danger'
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
  }>(),
  {
    variant: 'primary',
    type: 'button',
    disabled: false,
  },
)
</script>

<template>
  <button class="bt-btn" :class="[`bt-btn--${variant}`]" :type="type" :disabled="disabled">
    <slot />
  </button>
</template>

<style scoped>
.bt-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  min-height: 2.5rem;
  padding: 0.55rem 1.1rem;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: transform var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.bt-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.bt-btn:active:not(:disabled) {
  transform: translateY(0);
}

.bt-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.bt-btn--primary {
  background: var(--color-primary);
  color: var(--color-primary-contrast);
  box-shadow: var(--shadow-sm);
}

.bt-btn--ghost {
  background: transparent;
  color: var(--color-text);
  border-color: var(--color-border);
}

.bt-btn--danger {
  background: var(--color-danger);
  color: #fff;
}
</style>
