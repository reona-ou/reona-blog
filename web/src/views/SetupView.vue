<template>
  <div class="setup-page">
    <div class="setup-shell">
      <aside class="setup-aside">
        <div class="setup-brand">
          <span class="setup-mark">{{ brandMark }}</span>
          <b>{{ form.siteName || t('setup.title') }}</b>
        </div>

        <div class="setup-aside-copy">
          <p class="eyebrow">FIRST RUN</p>
          <h1 class="display">{{ t('setup.title') }}</h1>
          <p>{{ t('setup.intro') }}</p>
        </div>

        <ol class="setup-step-list">
          <li
            v-for="item in steps"
            :key="item.n"
            :class="{ active: step === item.n, done: step > item.n }"
          >
            <span>{{ item.n }}</span>
            <div>
              <b>{{ t(item.label) }}</b>
              <small>{{ t(item.hint) }}</small>
            </div>
          </li>
        </ol>

        <div class="setup-aside-footer">
          <span>{{ t('setup.privateOnly') }}</span>
          <select class="setup-language" :value="locale" @change="setLanguage($event.target.value)">
            <option v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.label }}
            </option>
          </select>
        </div>
      </aside>

      <main class="setup-main">
        <section v-if="step === 1">
          <p class="eyebrow">{{ t('setup.stepOf', { n: 1 }) }}</p>
          <h2 class="display">{{ t('setup.step1') }}</h2>
          <p class="setup-hint">{{ t('setup.step1Hint') }}</p>

          <div class="setup-form">
            <div class="setup-row">
              <label class="form-field">
                <span class="field-label">{{ t('admin.settings.siteName') }}</span>
                <input v-model="form.siteName" class="text-input" type="text" maxlength="32" />
              </label>
              <label class="form-field">
                <span class="field-label">{{ t('admin.settings.email') }}</span>
                <input v-model="form.siteEmail" class="text-input" type="email" maxlength="128" />
              </label>
            </div>

            <div class="setup-divider"></div>

            <div class="setup-row">
              <label class="form-field">
                <span class="field-label">{{ t('auth.username') }}</span>
                <input v-model="form.ownerUsername" class="text-input" type="text" autocomplete="username" maxlength="32" />
              </label>
              <label class="form-field">
                <span class="field-label">{{ t('auth.email') }}</span>
                <input v-model="form.ownerEmail" class="text-input" type="email" autocomplete="email" maxlength="128" />
              </label>
            </div>
            <label class="form-field">
              <span class="field-label">{{ t('auth.nickname') }}</span>
              <input v-model="form.ownerNickname" class="text-input" type="text" maxlength="32" />
            </label>
            <div class="setup-row">
              <label class="form-field">
                <span class="field-label">{{ t('auth.password') }}</span>
                <input v-model="form.password" class="text-input" type="password" autocomplete="new-password" />
              </label>
              <label class="form-field">
                <span class="field-label">{{ t('auth.confirmPassword') }}</span>
                <input v-model="form.confirm" class="text-input" type="password" autocomplete="new-password" />
              </label>
            </div>
          </div>

          <div class="setup-actions">
            <button class="btn btn-primary" type="button" @click="nextFromStep1">
              {{ t('setup.next') }}
            </button>
          </div>
        </section>

        <section v-else-if="step === 2">
          <p class="eyebrow">{{ t('setup.stepOf', { n: 2 }) }}</p>
          <h2 class="display">{{ t('setup.assistantTitle') }}</h2>
          <p class="setup-hint">{{ t('setup.assistantHint') }}</p>

          <label class="setup-check">
            <input v-model="form.configureAssistant" type="checkbox" />
            {{ t('setup.configureAssistant') }}
          </label>

          <div v-if="form.configureAssistant" class="setup-form">
            <label class="setup-check">
              <input v-model="form.assistantEnabled" type="checkbox" />
              {{ t('admin.assistant.enabled') }}
            </label>
            <label class="form-field">
              <span class="field-label">{{ t('admin.assistant.apiUrl') }}</span>
              <input v-model="form.assistantApiUrl" class="text-input" type="url" maxlength="255" />
            </label>
            <div class="setup-row">
              <label class="form-field">
                <span class="field-label">{{ t('admin.assistant.model') }}</span>
                <input v-model="form.assistantModel" class="text-input" type="text" maxlength="128" />
              </label>
              <label class="form-field">
                <span class="field-label">{{ t('admin.assistant.apiKey') }}</span>
                <input v-model="form.assistantApiKey" class="text-input" type="password" autocomplete="off" maxlength="255" />
              </label>
            </div>
          </div>

          <div class="setup-actions">
            <button class="btn btn-outline" type="button" @click="step = 1">{{ t('setup.back') }}</button>
            <button class="btn btn-outline" type="button" @click="skipAssistant">{{ t('setup.skip') }}</button>
            <button class="btn btn-primary" type="button" @click="nextFromStep2">
              {{ t('setup.next') }}
            </button>
          </div>
        </section>

        <section v-else>
          <p class="eyebrow">{{ t('setup.stepOf', { n: 3 }) }}</p>
          <h2 class="display">{{ t('setup.sampleTitle') }}</h2>
          <p class="setup-hint">{{ t('setup.sampleHint') }}</p>

          <label class="setup-check">
            <input v-model="form.createSampleContent" type="checkbox" />
            {{ t('setup.createSample') }}
          </label>

          <div class="setup-summary">
            <div>
              <span>{{ t('admin.settings.siteName') }}</span>
              <b>{{ form.siteName }}</b>
            </div>
            <div>
              <span>{{ t('account.email') }}</span>
              <b>{{ form.ownerEmail }}</b>
            </div>
            <div>
              <span>{{ t('setup.assistantTitle') }}</span>
              <b>{{ form.configureAssistant ? form.assistantModel : t('setup.skip') }}</b>
            </div>
          </div>

          <p class="setup-note">{{ t('setup.defaultAdminNote') }}</p>

          <div class="setup-actions">
            <button class="btn btn-outline" type="button" @click="step = 2">{{ t('setup.back') }}</button>
            <button class="btn btn-primary" type="button" :disabled="submitting" @click="finish">
              {{ submitting ? t('setup.initializing') : t('setup.finish') }}
            </button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { initializeSetup } from '@/api/setup'
import { loadAssistant } from '@/config/assistant'
import { loadSiteSettings } from '@/config/site'
import { loadSetupStatus, setupState } from '@/config/setup'
import { supportedLocales } from '@/i18n'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()
const languages = supportedLocales

const step = ref(1)
const submitting = ref(false)
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const form = reactive({
  siteName: 'Default Blog',
  siteEmail: '',
  ownerUsername: '',
  ownerEmail: '',
  ownerNickname: '',
  password: '',
  confirm: '',
  configureAssistant: true,
  assistantEnabled: true,
  assistantApiUrl: 'http://127.0.0.1:8080/v1/chat/completions',
  assistantModel: 'qwen3.5-4b',
  assistantApiKey: '',
  createSampleContent: true
})

const steps = [
  { n: 1, label: 'setup.step1', hint: 'setup.step1Hint' },
  { n: 2, label: 'setup.step2', hint: 'setup.step2Hint' },
  { n: 3, label: 'setup.step3', hint: 'setup.step3Hint' }
]

const brandMark = computed(() => {
  const value = String(form.siteName || '').trim()
  return value ? value.slice(0, 1).toUpperCase() : 'D'
})

function setLanguage(code) {
  locale.value = code
  localStorage.setItem('site_language', code)
}

function nextFromStep1() {
  form.siteName = form.siteName.trim()
  form.siteEmail = form.siteEmail.trim()
  form.ownerUsername = form.ownerUsername.trim()
  form.ownerEmail = form.ownerEmail.trim()
  form.ownerNickname = form.ownerNickname.trim()

  if (!form.siteName) {
    ElMessage.warning(t('setup.siteNameRequired'))
    return
  }
  if (!emailPattern.test(form.siteEmail)) {
    ElMessage.warning(t('setup.emailInvalid'))
    return
  }
  if (!form.ownerUsername || form.ownerUsername.length > 32 || form.ownerUsername.includes('@')) {
    ElMessage.warning(t('setup.usernameRequired'))
    return
  }
  if (!emailPattern.test(form.ownerEmail)) {
    ElMessage.warning(t('setup.emailInvalid'))
    return
  }
  if (form.ownerNickname.length > 32) {
    ElMessage.warning(t('setup.nicknameTooLong'))
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning(t('setup.passwordShort'))
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.warning(t('setup.passwordMismatch'))
    return
  }
  step.value = 2
}

function skipAssistant() {
  form.configureAssistant = false
  step.value = 3
}

function nextFromStep2() {
  if (form.configureAssistant) {
    form.assistantApiUrl = form.assistantApiUrl.trim()
    form.assistantModel = form.assistantModel.trim()
    if (!/^https?:\/\/\S+$/.test(form.assistantApiUrl)) {
      ElMessage.warning(t('setup.apiUrlInvalid'))
      return
    }
    if (!form.assistantModel) {
      ElMessage.warning(t('setup.modelRequired'))
      return
    }
  }
  step.value = 3
}

async function finish() {
  if (submitting.value) {
    return
  }
  submitting.value = true
  try {
    const data = await initializeSetup({
      siteName: form.siteName.trim(),
      siteEmail: form.siteEmail.trim(),
      ownerUsername: form.ownerUsername.trim(),
      ownerEmail: form.ownerEmail.trim(),
      ownerNickname: form.ownerNickname.trim() || form.ownerUsername.trim(),
      ownerPassword: form.password,
      configureAssistant: form.configureAssistant,
      assistantEnabled: form.assistantEnabled,
      assistantApiUrl: form.assistantApiUrl.trim(),
      assistantModel: form.assistantModel.trim(),
      assistantApiKey: form.assistantApiKey.trim(),
      createSampleContent: form.createSampleContent
    })
    setupState.checked = true
    setupState.required = false
    setupState.siteName = form.siteName.trim()
    setupState.email = form.siteEmail.trim()
    if (data) {
      userStore.setUser(data)
    }
    await Promise.allSettled([loadSiteSettings(), loadAssistant()])
    ElMessage.success(t('setup.completed'))
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message || t('setup.failed'))
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const status = await loadSetupStatus(true)
  if (!status.required) {
    router.replace('/')
    return
  }
  if (status.siteName) {
    form.siteName = status.siteName
  }
  if (status.email) {
    form.siteEmail = status.email
  }
})
</script>

<style scoped>
.setup-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 24px;
  background:
    radial-gradient(circle at 12% 16%, rgba(181, 80, 46, 0.06), transparent 30%),
    radial-gradient(circle at 88% 2%, rgba(47, 74, 58, 0.08), transparent 28%),
    var(--paper);
}

.setup-shell {
  width: min(1080px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 0.88fr) minmax(0, 1.12fr);
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.setup-aside {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 34px;
  overflow: hidden;
  background: var(--green);
  color: var(--paper);
}

.setup-aside::after {
  content: "";
  position: absolute;
  right: -110px;
  bottom: -130px;
  width: 300px;
  height: 300px;
  border: 1px solid rgba(244, 239, 230, 0.14);
  border-radius: 50%;
}

.setup-aside::before {
  content: "";
  position: absolute;
  right: -30px;
  bottom: -50px;
  width: 180px;
  height: 180px;
  border: 1px solid rgba(244, 239, 230, 0.1);
  border-radius: 50%;
}

.setup-brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 11px;
}

.setup-mark {
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

.setup-brand b {
  overflow: hidden;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.16em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.setup-aside-copy {
  position: relative;
  z-index: 1;
  margin-top: auto;
  margin-bottom: 26px;
  padding-top: 56px;
}

.setup-aside-copy .eyebrow {
  color: rgba(244, 239, 230, 0.4);
}

.setup-aside-copy h1 {
  margin: 16px 0 14px;
  font-size: clamp(38px, 4vw, 54px);
  line-height: 1.12;
}

.setup-aside-copy p:last-child {
  max-width: 320px;
  margin: 0;
  color: rgba(244, 239, 230, 0.64);
  font-size: 13px;
  line-height: 1.9;
}

.setup-step-list {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 0 0 30px;
  padding: 0;
  list-style: none;
}

.setup-step-list li {
  display: flex;
  gap: 11px;
  color: rgba(244, 239, 230, 0.48);
}

.setup-step-list li > span {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  flex: 0 0 24px;
  border: 1px solid rgba(244, 239, 230, 0.24);
  border-radius: 50%;
  font-size: 10px;
}

.setup-step-list li b {
  display: block;
  font-size: 12px;
  font-weight: 600;
}

.setup-step-list li small {
  display: block;
  margin-top: 2px;
  font-size: 10px;
  line-height: 1.6;
}

.setup-step-list li.active {
  color: var(--paper);
}

.setup-step-list li.active > span {
  border-color: var(--gold);
  background: var(--gold);
  color: var(--ink);
}

.setup-step-list li.done {
  color: rgba(244, 239, 230, 0.78);
}

.setup-step-list li.done > span {
  border-color: rgba(244, 239, 230, 0.5);
}

.setup-aside-footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(244, 239, 230, 0.14);
  color: rgba(244, 239, 230, 0.42);
  font-size: 10px;
  line-height: 1.7;
}

.setup-aside-footer > span {
  max-width: 330px;
}

.setup-language {
  flex: 0 0 auto;
  height: 30px;
  padding: 0 8px;
  border: 1px solid rgba(244, 239, 230, 0.22);
  border-radius: 999px;
  background: transparent;
  color: var(--paper);
  font-size: 11px;
  outline: none;
}

.setup-language option {
  color: var(--ink);
}

.setup-main {
  padding: 42px 48px;
}

.setup-main .eyebrow {
  color: var(--accent);
}

.setup-main h2 {
  margin: 12px 0 8px;
  font-size: 32px;
  font-weight: 500;
}

.setup-hint {
  margin: 0 0 22px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}

.setup-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.setup-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.setup-divider {
  height: 1px;
  margin: 6px 0;
  background: var(--line);
}

.setup-check {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 14px 0;
  color: var(--ink-soft);
  font-size: 13px;
  cursor: pointer;
}

.setup-check input {
  width: 15px;
  height: 15px;
  accent-color: var(--green);
}

.setup-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 28px;
}

.setup-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 20px 0 0;
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--paper);
}

.setup-summary > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  font-size: 12px;
}

.setup-summary span {
  color: var(--muted);
}

.setup-summary b {
  overflow: hidden;
  color: var(--ink-soft);
  font-weight: 600;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.setup-note {
  margin: 18px 0 0;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .setup-shell {
    grid-template-columns: 1fr;
  }

  .setup-aside {
    min-height: 280px;
  }

  .setup-aside-copy {
    padding-top: 36px;
  }

  .setup-main {
    padding: 34px 30px 38px;
  }
}

@media (max-width: 560px) {
  .setup-page {
    padding: 14px;
  }

  .setup-aside {
    padding: 26px 22px;
  }

  .setup-main {
    padding: 28px 20px 32px;
  }

  .setup-main h2 {
    font-size: 27px;
  }

  .setup-row {
    grid-template-columns: 1fr;
  }

  .setup-actions {
    flex-wrap: wrap;
  }
}
</style>
