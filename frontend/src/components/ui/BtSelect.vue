/**
 * 下拉选择。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
export type BtSelectOption = { value: string; label: string }

withDefaults(
  defineProps<{
    modelValue?: string
    options: BtSelectOption[]
    disabled?: boolean
    id?: string
  }>(),
  {
    modelValue: '',
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <select
    class="bt-select"
    :id="id"
    :value="modelValue"
    :disabled="disabled"
    @change="emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
  >
    <option v-for="opt in options" :key="opt.value" :value="opt.value">
      {{ opt.label }}
    </option>
  </select>
</template>

<style scoped>
.bt-select {
  width: 100%;
  min-height: 2.5rem;
  padding: 0.45rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-elevated);
  color: var(--color-text);
  font: inherit;
}

.bt-select:focus {
  outline: 2px solid color-mix(in srgb, var(--color-primary) 35%, transparent);
  outline-offset: 1px;
  border-color: var(--color-primary);
}
</style>
