/**
 * 多行文本框。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue?: string
    rows?: number
    placeholder?: string
    disabled?: boolean
  }>(),
  {
    modelValue: '',
    rows: 4,
    placeholder: '',
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <textarea
    class="bt-textarea"
    :rows="rows"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
</template>

<style scoped>
.bt-textarea {
  width: 100%;
  padding: 0.55rem 0.7rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-elevated);
  color: var(--color-text);
  font: inherit;
  resize: vertical;
}

.bt-textarea:focus {
  outline: 2px solid color-mix(in srgb, var(--color-primary) 35%, transparent);
  outline-offset: 1px;
  border-color: var(--color-primary);
}
</style>
