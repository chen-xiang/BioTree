/**
 * 应用根组件。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 挂载全局 Toast
 */
<script setup lang="ts">
import BtToastHost from '@/components/ui/BtToastHost.vue'
</script>

<template>
  <RouterView />
  <BtToastHost />
</template>
