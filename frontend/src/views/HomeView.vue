/**
 * 首页：品牌英雄级入口 + 轻量统计。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 强化品牌英雄区与动效，去掉首屏 API 状态
 */
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import BtButton from '@/components/ui/BtButton.vue'
import { fetchStatsSummary, type StatsSummary } from '@/api/stats'

const { t } = useI18n()
const stats = ref<StatsSummary | null>(null)

onMounted(async () => {
  try {
    stats.value = await fetchStatsSummary()
  } catch {
    stats.value = null
  }
})
</script>

<template>
  <section class="hero">
    <div class="hero-copy">
      <p class="brand">{{ t('common.brand') }}</p>
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
      <p v-if="stats" class="stats">
        {{ t('home.statsLine', { n: stats.totalTaxa }) }}
      </p>
    </div>
    <div class="hero-visual" aria-hidden="true">
      <div class="ring ring-a" />
      <div class="ring ring-b" />
      <div class="leaf" />
    </div>
  </section>
</template>

<style scoped>
.hero {
  min-height: calc(100vh - 5.5rem);
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: var(--space-6);
  align-items: center;
  position: relative;
  overflow: hidden;
}

.hero-copy {
  position: relative;
  z-index: 1;
  animation: rise var(--duration-normal) var(--ease-out);
  max-width: 34rem;
}

.brand {
  margin: 0 0 var(--space-4);
  font-family: var(--font-display);
  font-size: clamp(2.4rem, 6vw, 4rem);
  font-weight: 700;
  letter-spacing: 0.01em;
  line-height: 1;
  animation: brand-in 700ms var(--ease-out) both;
}

h1 {
  margin: 0 0 var(--space-3);
  font-family: var(--font-display);
  font-size: clamp(1.35rem, 2.6vw, 1.85rem);
  line-height: 1.25;
  font-weight: 550;
  color: var(--color-text-muted);
}

.subtitle {
  margin: 0 0 var(--space-5);
  color: var(--color-text-muted);
  font-size: var(--text-lg);
  max-width: 28rem;
}

.cta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  animation: rise 500ms var(--ease-out) 120ms both;
}

.stats {
  margin: var(--space-5) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.hero-visual {
  position: relative;
  min-height: min(52vh, 28rem);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 30% 30%, color-mix(in srgb, var(--color-primary) 28%, transparent), transparent 55%),
    linear-gradient(145deg, color-mix(in srgb, var(--color-bg-elevated) 80%, #9bb89a), var(--color-bg));
  overflow: hidden;
  animation: wash 12s ease-in-out infinite alternate;
}

.ring {
  position: absolute;
  border: 1px solid color-mix(in srgb, var(--color-primary) 35%, transparent);
  border-radius: 50%;
}

.ring-a {
  width: 70%;
  aspect-ratio: 1;
  left: 15%;
  top: 12%;
  animation: spin 28s linear infinite;
}

.ring-b {
  width: 42%;
  aspect-ratio: 1;
  right: 12%;
  bottom: 16%;
  animation: spin 18s linear infinite reverse;
}

.leaf {
  position: absolute;
  width: 38%;
  aspect-ratio: 0.72;
  left: 31%;
  top: 28%;
  border-radius: 60% 40% 55% 45%;
  background: color-mix(in srgb, var(--color-primary) 55%, #d7e8cf);
  transform: rotate(-18deg);
  animation: float 5.5s var(--ease-out) infinite alternate;
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
    min-height: auto;
    padding: var(--space-6) 0;
  }

  .hero-visual {
    min-height: 14rem;
    order: -1;
  }
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

@keyframes brand-in {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes float {
  from {
    transform: rotate(-18deg) translateY(0);
  }
  to {
    transform: rotate(-12deg) translateY(-10px);
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes wash {
  from {
    filter: saturate(1);
  }
  to {
    filter: saturate(1.08);
  }
}
</style>
