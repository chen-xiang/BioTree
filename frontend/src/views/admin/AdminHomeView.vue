<script setup lang="ts">
/**
 * 管理后台首页：统计台账、导入进度与快捷入口。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 接入 stats 与导入状态
 * Updated: 2026-09-01 统计改为阶元台账，导入进度条可视化
 */
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { fetchImportStatus, type ImportStatus } from '@/api/import'
import { fetchStatsSummary, type StatsSummary } from '@/api/stats'
import BtButton from '@/components/ui/BtButton.vue'
import { ledgerRanks } from '@/domain/ranks'
import { useLocaleStore } from '@/stores/locale'
import { messageFromApiError, rankLabel } from '@/utils/apiError'

const { t } = useI18n()
const localeStore = useLocaleStore()
const stats = ref<StatsSummary | null>(null)
const importStatus = ref<ImportStatus | null>(null)
const error = ref('')

const numberLocale = computed(() => (localeStore.locale === 'zh-CN' ? 'zh-CN' : 'en-US'))

const rankLedger = computed(() => (stats.value ? ledgerRanks(stats.value.byRank) : []))

const kingdoms = computed(() => {
  if (!stats.value) return []
  return Object.entries(stats.value.byKingdom).sort((a, b) => b[1] - a[1])
})

const importPct = computed(() => {
  const total = importStatus.value?.totalHint ?? 0
  const n = importStatus.value?.processedCount ?? 0
  if (total <= 0) return 0
  return Math.min(100, Math.round((n / total) * 100))
})

function formatCount(n: number) {
  return n.toLocaleString(numberLocale.value)
}

onMounted(async () => {
  try {
    const [s, imp] = await Promise.all([fetchStatsSummary(), fetchImportStatus()])
    stats.value = s
    importStatus.value = imp
  } catch (e) {
    error.value = messageFromApiError(e)
  }
})
</script>

<template>
  <section class="admin-home">
    <header>
      <h1>{{ t('admin.title') }}</h1>
      <p>{{ t('admin.homeSubtitle') }}</p>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <div class="actions">
      <RouterLink to="/admin/taxa">
        <BtButton>{{ t('admin.taxaNav') }}</BtButton>
      </RouterLink>
      <RouterLink to="/browse">
        <BtButton variant="ghost">{{ t('nav.browse') }}</BtButton>
      </RouterLink>
    </div>

    <div v-if="stats" class="tally">
      <p class="tally-label">{{ t('admin.statTotal') }}</p>
      <p class="tally-value">{{ formatCount(stats.totalTaxa) }}</p>
    </div>

    <div v-if="rankLedger.length" class="ledger">
      <h2>{{ t('admin.statByRank') }}</h2>
      <ul>
        <li v-for="row in rankLedger" :key="row.rank">
          <span>{{ rankLabel(row.rank) }}</span>
          <span class="num">{{ formatCount(row.count) }}</span>
        </li>
      </ul>
    </div>

    <div v-if="kingdoms.length" class="kingdoms">
      <h2>{{ t('admin.statByKingdom') }}</h2>
      <ul>
        <li v-for="[name, count] in kingdoms" :key="name">
          <strong>{{ name }}</strong>
          <span class="num">{{ formatCount(count) }}</span>
        </li>
      </ul>
    </div>

    <div v-if="stats?.dataset" class="dataset">
      <h2>{{ t('admin.datasetSource') }}</h2>
      <p>
        <strong>{{ stats.dataset.title || 'Catalogue of Life' }}</strong>
        <span v-if="stats.dataset.version"> · {{ stats.dataset.version }}</span>
      </p>
      <p v-if="stats.dataset.sourceUrl" class="muted">
        <a :href="stats.dataset.sourceUrl" target="_blank" rel="noopener noreferrer">
          {{ stats.dataset.sourceUrl }}
        </a>
      </p>
    </div>

    <div v-if="importStatus" class="import">
      <h2>{{ t('admin.importStatus') }}</h2>
      <p>
        {{ t('admin.importPhase') }}:
        <strong>{{ importStatus.phase }}</strong>
      </p>
      <div
        class="bar"
        role="progressbar"
        :aria-valuemin="0"
        :aria-valuemax="100"
        :aria-valuenow="importPct"
      >
        <span :style="{ width: `${importPct}%` }" />
      </div>
      <p class="muted">
        {{ t('admin.importProgress', {
          n: formatCount(importStatus.processedCount ?? 0),
          total: formatCount(importStatus.totalHint ?? 0),
        }) }}
      </p>
    </div>
  </section>
</template>

<style scoped>
.admin-home {
  display: grid;
  gap: var(--space-5);
  max-width: 46rem;
}

header h1 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
  font-weight: 650;
}

header p {
  margin: 0;
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.tally {
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-elevated);
}

.tally-label {
  margin: 0 0 var(--space-2);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.tally-value {
  margin: 0;
  font-family: var(--font-mono);
  font-size: clamp(2rem, 4vw, 2.75rem);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
}

.ledger,
.kingdoms,
.dataset,
.import {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
}

.ledger h2,
.kingdoms h2,
.dataset h2,
.import h2 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-family: var(--font-mono);
  font-weight: 500;
  color: var(--color-text-muted);
}

.ledger ul,
.kingdoms ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.35rem;
}

.ledger li,
.kingdoms li {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 0.35rem 0;
  border-bottom: 1px solid var(--color-border);
}

.ledger li:last-child,
.kingdoms li:last-child {
  border-bottom: 0;
}

.num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-muted);
}

.dataset a {
  color: var(--color-primary);
}

.bar {
  height: 0.35rem;
  margin: var(--space-3) 0;
  border-radius: 99px;
  background: var(--color-bg-muted);
  overflow: hidden;
}

.bar span {
  display: block;
  height: 100%;
  background: var(--color-primary);
}

.muted {
  color: var(--color-text-muted);
}

.error {
  color: var(--color-danger);
}
</style>
