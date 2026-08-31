/**
 * 首页：品牌优先的浏览入口。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
/**
 * 首页：品牌优先的浏览入口。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import BtButton from '@/components/ui/BtButton.vue'
import { fetchHealth } from '@/api/client'

const { t } = useI18n()
const apiStatus = ref<string>('…')

onMounted(async () => {
  try {
    const result = await fetchHealth()
    apiStatus.value = result.data.status
  } catch {
    apiStatus.value = 'DOWN'
  }
})
</script>

<template>
  <section class="hero">
    <p class="eyebrow">{{ t('common.brand') }}</p>
    <h1>{{ t('home.headline') }}</h1>
    <p class="subtitle">{{ t('home.subtitle') }}</p>
    <div class="cta">
      <RouterLink to="/browse">
        <BtButton>{{ t('home.ctaBrowse') }}</BtButton>
      </RouterLink>
      <RouterLink to="/admin/taxa">
        <BtButton variant="ghost">{{ t('home.ctaAdmin') }}</BtButton>
      </RouterLink>
    </div>
    <p class="status">API: {{ apiStatus }}</p>
  </section>
</template>

<style scoped>
.hero {
  animation: rise var(--duration-normal) var(--ease-out);
  padding: var(--space-8) 0;
  max-width: 40rem;
}

.eyebrow {
  margin: 0 0 var(--space-3);
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 700;
  letter-spacing: 0.02em;
}

h1 {
  margin: 0 0 var(--space-3);
  font-family: var(--font-display);
  font-size: clamp(1.8rem, 4vw, 2.6rem);
  line-height: 1.15;
  font-weight: 600;
}

.subtitle {
  margin: 0 0 var(--space-5);
  color: var(--color-text-muted);
  font-size: var(--text-lg);
}

.cta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.status {
  margin-top: var(--space-5);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  font-family: var(--font-mono);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
