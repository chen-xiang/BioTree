/**
 * 首页：阶元脊柱作为主视觉，配合库藏数量进入浏览。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 强化品牌英雄区与动效，去掉首屏 API 状态
 * Updated: 2026-09-01 用林奈七级脊柱替换抽象叶片，管理入口降为文字链
 */
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { fetchStatsSummary, type StatsSummary } from '@/api/stats'
import RankSpine from '@/components/taxon/RankSpine.vue'
import BtButton from '@/components/ui/BtButton.vue'
import { useLocaleStore } from '@/stores/locale'

const { t } = useI18n()
const localeStore = useLocaleStore()
const stats = ref<StatsSummary | null>(null)

const tally = computed(() => {
  if (!stats.value) return ''
  return stats.value.totalTaxa.toLocaleString(localeStore.locale === 'zh-CN' ? 'zh-CN' : 'en-US')
})

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
        <RouterLink class="admin-link" to="/admin/taxa">{{ t('home.ctaAdmin') }}</RouterLink>
      </div>
      <p v-if="stats" class="stats">
        <strong>{{ tally }}</strong>
        <span>{{ t('home.statsLine') }}</span>
      </p>
    </div>
    <RankSpine
      class="hero-spine"
      :counts="stats?.byRank"
      :caption="t('home.spineCaption')"
    />
  </section>
</template>

<style scoped>
.hero {
  min-height: calc(100vh - 6.5rem);
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(16rem, 0.75fr);
  gap: var(--space-6);
  align-items: center;
}

.hero-copy {
  max-width: 34rem;
}

.brand {
  margin: 0 0 var(--space-4);
  font-family: var(--font-display);
  font-size: clamp(2.6rem, 6vw, 4.25rem);
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 0.95;
}

h1 {
  margin: 0 0 var(--space-3);
  font-family: var(--font-display);
  font-size: clamp(1.45rem, 2.8vw, 2rem);
  line-height: 1.25;
  font-weight: 500;
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
  align-items: center;
  gap: var(--space-4);
}

.admin-link {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.1rem;
  transition: color var(--duration-fast) var(--ease-out);
}

.admin-link:hover {
  color: var(--color-text);
}

.stats {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2) var(--space-3);
  margin: var(--space-6) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.stats strong {
  font-family: var(--font-mono);
  font-size: var(--text-xl);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--color-text);
}

.hero-spine {
  padding: var(--space-5) var(--space-6);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
    min-height: auto;
    gap: var(--space-6);
    padding: var(--space-4) 0;
  }

  .hero-spine {
    order: -1;
    padding: var(--space-4);
  }
}
</style>
