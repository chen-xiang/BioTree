/**
 * 管理员登录页骨架。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
<script setup lang="ts">
/**
 * 管理员登录页骨架。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import BtButton from '@/components/ui/BtButton.vue'
import { useAuthStore } from '@/stores/auth'
import { ensureCsrfCookie } from '@/utils/csrf'

const { t } = useI18n()
const router = useRouter()
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
    const response = await fetch('/api/admin/auth/login', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    })
    const body = await response.json()
    if (!response.ok || body.code !== 0) {
      throw new Error(body.message || 'login failed')
    }
    auth.setUser(body.data.username)
    await ensureCsrfCookie()
    await router.push('/admin')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'login failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="panel">
    <h1>{{ t('login.title') }}</h1>
    <form class="form" @submit.prevent="onSubmit">
      <label>
        <span>{{ t('login.username') }}</span>
        <input v-model="username" autocomplete="username" required />
      </label>
      <label>
        <span>{{ t('login.password') }}</span>
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <BtButton type="submit" :disabled="loading">{{ t('login.submit') }}</BtButton>
    </form>
  </section>
</template>

<style scoped>
.panel {
  max-width: 26rem;
  margin: var(--space-8) auto;
  padding: var(--space-6);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  animation: rise var(--duration-normal) var(--ease-out);
}

h1 {
  margin: 0 0 var(--space-5);
  font-family: var(--font-display);
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

input {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text);
  transition: border-color var(--duration-fast) var(--ease-out);
}

input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--text-sm);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
