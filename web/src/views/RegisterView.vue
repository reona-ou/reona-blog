<template>
  <div class="register-page">
    <div class="register-card container">
      <div class="register-copy">
        <p class="eyebrow">{{ t('auth.registerEyebrow') }}</p>
        <h1 class="display">{{ t('auth.registerTitle1') }}<br />{{ t('auth.registerTitle2') }}</h1>
        <p>{{ t('auth.registerIntro') }}</p>
      </div>

      <section class="register-form-panel">
        <div class="register-form-head">
          <p class="eyebrow">REGISTER</p>
          <h2 class="display">{{ t('auth.create') }}</h2>
        </div>
        <form class="register-form" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">{{ t('auth.username') }}</span>
            <input v-model="form.username" class="text-input" type="text" autocomplete="username" maxlength="32" :placeholder="t('auth.username')" />
          </label>
          <label class="form-field">
            <span class="field-label">{{ t('auth.emailLogin') }}</span>
            <input v-model="form.email" class="text-input" type="email" autocomplete="email" maxlength="128" placeholder="you@example.com" />
          </label>
          <label class="form-field">
            <span class="field-label">{{ t('auth.nickname') }}</span>
            <input v-model="form.nickname" class="text-input" type="text" maxlength="32" :placeholder="t('auth.nickname')" />
          </label>
          <label class="form-field">
            <span class="field-label">{{ t('auth.password') }}</span>
            <input v-model="form.password" class="text-input" type="password" autocomplete="new-password" :placeholder="t('auth.password')" />
          </label>
          <label class="form-field">
            <span class="field-label">{{ t('auth.confirmPassword') }}</span>
            <input v-model="form.confirm" class="text-input" type="password" autocomplete="new-password" :placeholder="t('auth.confirmPassword')" />
          </label>
          <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
            {{ loading ? t('auth.creating') : t('auth.create') }}
          </button>
        </form>
        <div class="register-links">
          <span>{{ t('auth.haveAccount') }}<RouterLink to="/login">{{ t('auth.loginLink') }}</RouterLink></span>
          <RouterLink to="/">{{ t('auth.backHome') }}</RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  nickname: '',
  password: '',
  confirm: ''
})

async function submit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning(t('auth.fillUsernamePassword'))
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    ElMessage.warning(t('auth.invalidEmail'))
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning(t('auth.passwordShort'))
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.warning(t('auth.passwordMismatch'))
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username.trim(),
      email: form.email.trim(),
      nickname: form.nickname.trim() || form.username.trim(),
      password: form.password
    })
    await userStore.login({ username: form.email.trim(), password: form.password })
    ElMessage.success(t('auth.registerSuccess'))
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message || t('auth.registerFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 76px);
  display: grid;
  place-items: center;
  padding: 56px 0 70px;
}

.register-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.9fr);
  gap: 80px;
  align-items: center;
  padding: 60px 68px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.register-copy h1 {
  margin: 18px 0 20px;
  font-size: clamp(42px, 5vw, 70px);
  line-height: 1.1;
}

.register-copy > p:last-child {
  max-width: 420px;
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.9;
}

.register-form-panel {
  padding: 34px;
  border-radius: var(--radius-md);
  background: var(--paper);
}

.register-form-head h2 {
  margin: 10px 0 0;
  font-size: 30px;
  font-weight: 500;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 24px;
}

.register-form .btn {
  margin-top: 8px;
}

.register-links {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
  color: var(--muted);
  font-size: 12px;
}

.register-links a {
  color: var(--accent);
}

@media (max-width: 900px) {
  .register-card {
    grid-template-columns: 1fr;
    gap: 38px;
    padding: 44px 34px;
  }
}

@media (max-width: 560px) {
  .register-page {
    padding-top: 26px;
  }

  .register-card {
    padding: 30px 22px;
  }

  .register-form-panel {
    padding: 24px 18px;
  }

  .register-links {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
