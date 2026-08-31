/**
 * 文本输入框。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue?: string
    type?: string
    placeholder?: string
    disabled?: boolean
    id?: string
    required?: boolean
  }>(),
  {
    modelValue: '',
    type: 'text',
    placeholder: '',
    disabled: false,
    required: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <input
    class="bt-input"
    :id="id"
    :type="type"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :required="required"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>

<style scoped>
.bt-input {
  width: 100%;
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-elevated);
  color: var(--color-text);
  font: inherit;
}

.bt-input:focus {
  outline: 2px solid color-mix(in srgb, var(--color-primary) 35%, transparent);
  outline-offset: 1px;
  border-color: var(--color-primary);
}

.bt-input:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
