/**
 * 顶栏：品牌、导航、主题与语言切换。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-09-01 阶元标记、当前页下划线、更紧凑的移动端折行
 */
<script setup lang="ts">
/**
 * 顶栏：品牌、导航、主题与语言切换。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import BtButton from '@/components/ui/BtButton.vue'
import { useLocaleStore } from '@/stores/locale'
import { useThemeStore } from '@/stores/theme'

const { t } = useI18n()
const theme = useThemeStore()
const locale = useLocaleStore()
</script>

<template>
  <header class="topbar">
    <RouterLink class="brand" to="/">
      <svg class="mark" viewBox="0 0 16 16" aria-hidden="true">
        <path d="M4 1.5v13M4 5h7M4 8.5h5.5M4 12h7" />
        <circle cx="4" cy="14.5" r="1" />
      </svg>
      {{ t('common.brand') }}
    </RouterLink>
    <nav class="nav">
      <RouterLink to="/" exact-active-class="is-current">{{ t('nav.home') }}</RouterLink>
      <RouterLink to="/browse" active-class="is-current">{{ t('nav.browse') }}</RouterLink>
      <RouterLink to="/admin" active-class="is-current">{{ t('nav.admin') }}</RouterLink>
    </nav>
    <div class="actions">
      <BtButton variant="ghost" :title="t('locale.toggle')" @click="locale.toggleLocale()">
        {{ locale.label }}
      </BtButton>
      <BtButton variant="ghost" :title="t('theme.toggle')" @click="theme.toggleTheme()">
        {{ theme.isDark ? t('theme.light') : t('theme.dark') }}
      </BtButton>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: 0.7rem var(--space-5);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(12px);
  background: color-mix(in srgb, var(--color-bg-elevated) 86%, transparent);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 650;
  letter-spacing: 0.01em;
}

.mark {
  width: 1rem;
  height: 1rem;
  stroke: var(--color-primary);
  stroke-width: 1.4;
  stroke-linecap: round;
  fill: var(--color-accent);
}

.nav {
  display: flex;
  gap: var(--space-1);
  margin-right: auto;
}

.nav a {
  color: var(--color-text-muted);
  padding: 0.35rem 0.7rem;
  border-radius: var(--radius-sm);
  transition:
    color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);
}

.nav a.is-current,
.nav a:hover {
  color: var(--color-text);
}

.nav a.is-current {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.actions {
  display: flex;
  gap: var(--space-2);
}

@media (max-width: 720px) {
  .topbar {
    flex-wrap: wrap;
    row-gap: var(--space-2);
  }

  .nav {
    order: 3;
    width: 100%;
    margin-right: 0;
    padding-top: var(--space-1);
    border-top: 1px solid var(--color-border);
  }
}
</style>
