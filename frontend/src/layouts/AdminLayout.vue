<script setup lang="ts">
/**
 * 管理后台布局：侧栏导航、当前用户与登出。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加分类管理侧栏入口
 * Updated: 2026-08-31 展示当前用户并支持登出
 */
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRouter } from 'vue-router'
import { fetchMe, logout } from '@/api/auth'
import AppTopbar from '@/components/layout/AppTopbar.vue'
import BtButton from '@/components/ui/BtButton.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { ensureCsrfCookie } from '@/utils/csrf'

const { t } = useI18n()
const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()
const busy = ref(false)

onMounted(async () => {
  if (!auth.username) {
    const name = await fetchMe()
    if (name) auth.setUser(name)
  }
})

async function onLogout() {
  busy.value = true
  try {
    await ensureCsrfCookie()
    await logout()
    auth.clear()
    toast.push(t('admin.loggedOut'), 'ok')
    await router.push({ name: 'login' })
  } catch (e) {
    toast.push(e instanceof Error ? e.message : t('common.failed'), 'error')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="layout">
    <AppTopbar />
    <div class="shell">
      <aside class="side">
        <p class="side-title">{{ t('admin.title') }}</p>
        <p v-if="auth.username" class="user">{{ auth.username }}</p>
        <RouterLink class="side-link" to="/admin">{{ t('nav.admin') }}</RouterLink>
        <RouterLink class="side-link" to="/admin/taxa">{{ t('admin.taxaNav') }}</RouterLink>
        <BtButton class="logout" variant="ghost" :disabled="busy" @click="onLogout">
          {{ t('admin.logout') }}
        </BtButton>
      </aside>
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.shell {
  display: grid;
  grid-template-columns: 220px 1fr;
  min-height: calc(100vh - 4.5rem);
}

.side {
  border-right: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-bg-elevated) 88%, transparent);
  padding: var(--space-5);
}

.side-title {
  margin: 0 0 var(--space-2);
  font-family: var(--font-display);
  font-size: var(--text-lg);
}

.user {
  margin: 0 0 var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.side-link {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--color-text-muted);
  transition: color var(--duration-fast) var(--ease-out);
}

.side-link.router-link-active,
.side-link:hover {
  color: var(--color-text);
}

.logout {
  margin-top: var(--space-4);
  width: 100%;
}

.main {
  padding: var(--space-6) var(--space-5);
}

@media (max-width: 860px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .side {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }
}
</style>
