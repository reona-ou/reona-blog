<template>
  <div class="auth-page">
    <div class="auth-card container">
      <aside class="auth-aside">
        <RouterLink to="/" class="auth-brand">
          <span>{{ site.mark }}</span>
          <b>{{ site.name }}</b>
        </RouterLink>
        <div class="auth-aside-copy">
          <p class="eyebrow">{{ t('auth.welcomeBack') }}</p>
          <h1 class="display">{{ t('auth.asideTitle') }}</h1>
          <p>{{ t('site.tagline') }}</p>
        </div>
        <div class="auth-aside-footer">{{ t('auth.asideFooter', { year: site.since }) }}</div>
      </aside>

      <section class="auth-form-panel">
        <div class="auth-form-head">
          <p class="eyebrow">LOGIN</p>
          <h2 class="display">{{ t('auth.login') }}</h2>
          <p>{{ t('auth.loginIntro') }}</p>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">{{ t('auth.email') }}</span>
            <input v-model="form.email" class="text-input" type="email" autocomplete="email" maxlength="128" placeholder="you@example.com" />
          </label>
          <label class="form-field">
            <span class="field-label">{{ t('auth.password') }}</span>
            <input v-model="form.password" class="text-input" type="password" autocomplete="current-password" :placeholder="t('auth.password')" />
          </label>
          <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
            {{ loading ? t('auth.submitting') : t('auth.submit') }}
          </button>
        </form>

        <div class="auth-links">
          <span>{{ t('auth.noAccount') }}<RouterLink to="/register">{{ t('auth.register') }}</RouterLink></span>
          <RouterLink to="/">{{ t('auth.backHome') }}</RouterLink>
        </div>

      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { site } from '@/config/site'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ email: '', password: '' })

async function submit() {
  const email = form.email.trim()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || !form.password) {
    ElMessage.warning(t('auth.fillEmailPassword'))
    return
  }
  loading.value = true
  try {
    const data = await userStore.login({
      username: email,
      password: form.password
    })
    ElMessage.success(t('auth.loginSuccess'))
    const rawRedirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const redirect = rawRedirect.startsWith('/') && !rawRedirect.startsWith('//') ? rawRedirect : ''
    if (data.role === 'ROLE_ADMIN' || data.role === 'ROLE_SUPER_ADMIN') {
      if (redirect && !redirect.startsWith('/login')) {
        router.push(redirect)
      } else {
        router.push(data.role === 'ROLE_SUPER_ADMIN' ? '/' : '/admin/overview')
      }
    } else {
      router.push(redirect && !redirect.startsWith('/admin') && !redirect.startsWith('/login') ? redirect : '/')
    }
  } catch (error) {
    ElMessage.error(error.message || t('auth.loginFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 76px);
  display: grid;
  place-items: center;
  padding: 56px 0 70px;
}

.auth-card {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  min-height: 560px;
  border: 1px solid var(--line);
  border-radius: 18px;
  overflow: hidden;
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.auth-aside {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 34px;
  background: var(--green);
  color: var(--paper);
  overflow: hidden;
}

.auth-aside::after {
  content: "";
  position: absolute;
  right: -120px;
  bottom: -120px;
  width: 320px;
  height: 320px;
  border: 1px solid rgba(244, 239, 230, 0.14);
  border-radius: 50%;
}

.auth-aside::before {
  content: "";
  position: absolute;
  right: -40px;
  bottom: -40px;
  width: 180px;
  height: 180px;
  border: 1px solid rgba(244, 239, 230, 0.1);
  border-radius: 50%;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.auth-brand span {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--paper);
  color: var(--ink);
  font-family: Georgia, serif;
  font-size: 19px;
}

.auth-brand b {
  font-family: Georgia, serif;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.18em;
}

.auth-aside-copy {
  position: relative;
  z-index: 1;
  margin-top: auto;
  margin-bottom: 28px;
}

.auth-aside-copy .eyebrow {
  color: rgba(244, 239, 230, 0.4);
}

.auth-aside-copy h1 {
  margin: 16px 0 16px;
  font-size: clamp(42px, 5vw, 64px);
  line-height: 1.1;
}

.auth-aside-copy p:last-child {
  max-width: 300px;
  margin: 0;
  color: rgba(244, 239, 230, 0.62);
  font-size: 14px;
  line-height: 1.9;
}

.auth-aside-footer {
  position: relative;
  z-index: 1;
  color: rgba(244, 239, 230, 0.35);
  font-size: 9px;
  letter-spacing: 0.2em;
}

.auth-form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 54px 68px;
}

.auth-form-head h2 {
  margin: 12px 0 8px;
  font-size: 38px;
  font-weight: 500;
}

.auth-form-head > p:last-child {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 34px;
}

.auth-form .btn {
  margin-top: 8px;
}

.auth-links {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 22px;
  color: var(--muted);
  font-size: 12px;
}

.auth-links a {
  color: var(--accent);
}

@media (max-width: 820px) {
  .auth-card {
    grid-template-columns: 1fr;
  }

  .auth-aside {
    min-height: 230px;
  }

  .auth-aside-copy {
    margin-top: 40px;
  }

  .auth-aside-copy h1 {
    font-size: 44px;
  }

  .auth-form-panel {
    padding: 38px 30px 42px;
  }
}

@media (max-width: 520px) {
  .auth-page {
    padding-top: 26px;
  }

  .auth-card {
    min-height: 0;
  }

  .auth-form-panel {
    padding: 30px 22px 34px;
  }

  .auth-links {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
