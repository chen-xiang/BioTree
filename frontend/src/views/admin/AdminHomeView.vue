<script setup lang="ts">
/**
 * 管理后台首页：统计概览、导入状态与快捷入口。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 接入 stats 与导入状态
 */
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { fetchImportStatus, type ImportStatus } from '@/api/import'
import { fetchStatsSummary, type StatsSummary } from '@/api/stats'
import BtButton from '@/components/ui/BtButton.vue'
import { messageFromApiError, rankLabel } from '@/utils/apiError'

const { t } = useI18n()
const stats = ref<StatsSummary | null>(null)
const importStatus = ref<ImportStatus | null>(null)
const error = ref('')

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

    <div v-if="stats" class="stats">
      <article class="stat">
        <p class="label">{{ t('admin.statTotal') }}</p>
        <p class="value">{{ stats.totalTaxa }}</p>
      </article>
      <article v-for="(count, rank) in stats.byRank" :key="rank" class="stat">
        <p class="label">{{ rankLabel(String(rank)) }}</p>
        <p class="value">{{ count }}</p>
      </article>
    </div>

    <div v-if="stats && Object.keys(stats.byKingdom).length" class="kingdoms">
      <h2>{{ t('admin.statByKingdom') }}</h2>
      <ul>
        <li v-for="(count, name) in stats.byKingdom" :key="name">
          <strong>{{ name }}</strong>
          <span>{{ count }}</span>
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
      <p class="muted">
        {{ t('admin.importProgress', {
          n: importStatus.processedCount ?? 0,
          total: importStatus.totalHint ?? 0,
        }) }}
      </p>
    </div>
  </section>
</template>

<style scoped>
.admin-home {
  display: grid;
  gap: var(--space-5);
  animation: rise var(--duration-normal) var(--ease-out);
  max-width: 52rem;
}

header h1 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
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

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(7.5rem, 1fr));
  gap: var(--space-3);
}

.stat {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
}

.stat .label {
  margin: 0 0 var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.stat .value {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-xl);
}

.kingdoms,
.import {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
}

.kingdoms h2,
.import h2 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-lg);
}

.kingdoms ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--space-2);
}

.kingdoms li {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
}

.muted {
  color: var(--color-text-muted);
}

.error {
  color: var(--color-danger, #b42318);
}
</style>
