/**
 * 顶栏：品牌、导航、主题与语言切换。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
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
    <RouterLink class="brand" to="/">{{ t('common.brand') }}</RouterLink>
    <nav class="nav">
      <RouterLink to="/">{{ t('nav.home') }}</RouterLink>
      <RouterLink to="/admin">{{ t('nav.admin') }}</RouterLink>
    </nav>
    <div class="actions">
      <BtButton variant="ghost" @click="locale.toggleLocale()">{{ locale.label }}</BtButton>
      <BtButton variant="ghost" @click="theme.toggleTheme()">
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
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(10px);
  background: color-mix(in srgb, var(--color-bg-elevated) 82%, transparent);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 650;
  letter-spacing: 0.01em;
}

.nav {
  display: flex;
  gap: var(--space-4);
  margin-right: auto;
}

.nav a {
  color: var(--color-text-muted);
  transition: color var(--duration-fast) var(--ease-out);
}

.nav a.router-link-active,
.nav a:hover {
  color: var(--color-text);
}

.actions {
  display: flex;
  gap: var(--space-2);
}

@media (max-width: 720px) {
  .topbar {
    flex-wrap: wrap;
  }

  .nav {
    order: 3;
    width: 100%;
    margin-right: 0;
  }
}
</style>
