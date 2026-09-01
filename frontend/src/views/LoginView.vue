/**
 * 管理员登录页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-09-01 登录后尊重 redirect；去掉未使用的原生 input 样式
 */
<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { login } from '@/api/auth'
import BtButton from '@/components/ui/BtButton.vue'
import BtInput from '@/components/ui/BtInput.vue'
import { useAuthStore } from '@/stores/auth'
import { ensureCsrfCookie } from '@/utils/csrf'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('admin')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    await ensureCsrfCookie()
    const name = await login(username.value, password.value)
    auth.setUser(name)
    await ensureCsrfCookie()
    const raw = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
    const redirect = raw.startsWith('/') && !raw.startsWith('//') ? raw : '/admin'
    await router.push(redirect)
  } catch (e) {
    error.value = e instanceof Error ? e.message : t('common.failed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="panel">
    <p class="eyebrow">{{ t('common.brand') }}</p>
    <h1>{{ t('login.title') }}</h1>
    <form class="form" @submit.prevent="onSubmit">
      <label>
        <span>{{ t('login.username') }}</span>
        <BtInput v-model="username" autocomplete="username" :required="true" />
      </label>
      <label>
        <span>{{ t('login.password') }}</span>
        <BtInput v-model="password" type="password" autocomplete="current-password" :required="true" />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <BtButton type="submit" :disabled="loading">{{ t('login.submit') }}</BtButton>
    </form>
  </section>
</template>

<style scoped>
.panel {
  max-width: 24rem;
  margin: var(--space-8) auto;
  padding: var(--space-6);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.eyebrow {
  margin: 0 0 var(--space-2);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-primary);
}

h1 {
  margin: 0 0 var(--space-5);
  font-family: var(--font-display);
  font-weight: 650;
}

.form {
  display: grid;
  gap: var(--space-4);
}

label {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--text-sm);
}
</style>
