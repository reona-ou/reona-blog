<template>
  <div class="account-page">
    <div class="container account-shell">
      <header class="account-head">
        <p class="eyebrow">ACCOUNT</p>
        <h1 class="display">{{ t('account.title') }}</h1>
        <p>{{ t('account.intro') }}</p>
      </header>

      <section class="account-card">
        <div class="account-card-head">
          <h2 class="display">{{ t('account.profileTitle') }}</h2>
          <span class="account-role">{{ roleLabel }}</span>
        </div>
        <form class="account-form" @submit.prevent="saveProfile">
          <label class="form-field">
            <span class="field-label">{{ t('account.username') }}</span>
            <input :value="profile.username" class="text-input" type="text" disabled />
          </label>
          <div class="account-form-row">
            <label class="form-field">
              <span class="field-label">{{ t('account.nickname') }}</span>
              <input v-model="profile.nickname" class="text-input" type="text" maxlength="32" />
            </label>
            <label class="form-field">
              <span class="field-label">{{ t('account.email') }}</span>
              <input v-model="profile.email" class="text-input" type="email" maxlength="128" />
            </label>
          </div>
          <div class="account-form-row">
            <label class="form-field">
              <span class="field-label">{{ t('account.phone') }}</span>
              <input v-model="profile.phone" class="text-input" type="text" maxlength="11" />
            </label>
            <label class="form-field">
              <span class="field-label">{{ t('account.address') }}</span>
              <input v-model="profile.address" class="text-input" type="text" maxlength="255" />
            </label>
          </div>
          <button class="btn btn-primary" type="submit" :disabled="profileSaving || loading">
            {{ profileSaving ? t('account.saving') : t('account.saveProfile') }}
          </button>
        </form>
      </section>

      <section class="account-card">
        <div class="account-card-head">
          <h2 class="display">{{ t('account.passwordTitle') }}</h2>
        </div>
        <p class="account-hint">{{ t('account.passwordHint') }}</p>
        <form class="account-form" @submit.prevent="savePassword">
          <label class="form-field">
            <span class="field-label">{{ t('account.oldPassword') }}</span>
            <input v-model="password.old" class="text-input" type="password" autocomplete="current-password" />
          </label>
          <div class="account-form-row">
            <label class="form-field">
              <span class="field-label">{{ t('account.newPassword') }}</span>
              <input v-model="password.next" class="text-input" type="password" autocomplete="new-password" />
            </label>
            <label class="form-field">
              <span class="field-label">{{ t('account.confirmPassword') }}</span>
              <input v-model="password.confirm" class="text-input" type="password" autocomplete="new-password" />
            </label>
          </div>
          <button class="btn btn-primary" type="submit" :disabled="passwordSaving">
            {{ passwordSaving ? t('account.updating') : t('account.savePassword') }}
          </button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'
import { getUserById, updateMyProfile } from '@/api/users'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profile = reactive({
  username: userStore.user?.username || '',
  nickname: userStore.user?.nickname || '',
  email: userStore.user?.email || '',
  phone: '',
  address: ''
})

const password = reactive({
  old: '',
  next: '',
  confirm: ''
})

const roleLabel = computed(() => {
  const role = userStore.user?.role
  if (role === 'ROLE_SUPER_ADMIN') return t('admin.users.roles.super')
  if (role === 'ROLE_ADMIN') return t('admin.users.roles.admin')
  return t('admin.users.roles.user')
})

async function load() {
  if (!userStore.user?.id) return
  loading.value = true
  try {
    const data = await getUserById(userStore.user.id)
    if (data) {
      profile.username = data.username || ''
      profile.nickname = data.nickname || ''
      profile.email = data.email || ''
      profile.phone = data.phone || ''
      profile.address = data.address || ''
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  const nickname = profile.nickname.trim()
  const email = profile.email.trim()
  if (!nickname) {
    ElMessage.warning(t('account.nicknameRequired'))
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning(t('account.emailInvalid'))
    return
  }
  profileSaving.value = true
  try {
    const data = await updateMyProfile({
      nickname,
      email,
      phone: profile.phone.trim(),
      address: profile.address.trim()
    })
    profile.nickname = data.nickname || nickname
    profile.email = data.email || email
    profile.phone = data.phone || ''
    profile.address = data.address || ''
    userStore.setUser({
      ...userStore.user,
      nickname: profile.nickname,
      email: profile.email
    })
    ElMessage.success(t('account.profileSaved'))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  if (!password.old) {
    ElMessage.warning(t('account.oldPasswordRequired'))
    return
  }
  if (password.next.length < 6) {
    ElMessage.warning(t('account.passwordShort'))
    return
  }
  if (password.next !== password.confirm) {
    ElMessage.warning(t('account.passwordMismatch'))
    return
  }
  passwordSaving.value = true
  try {
    await changePassword({
      username: userStore.user.username,
      password: password.old,
      newPassword: password.next
    })
    password.old = ''
    password.next = ''
    password.confirm = ''
    ElMessage.success(t('account.passwordChanged'))
    userStore.logout()
    router.push({ name: 'login' })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    passwordSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.account-page {
  padding: 56px 0 40px;
}

.account-shell {
  max-width: 820px;
}

.account-head h1 {
  margin: 14px 0 10px;
  font-size: clamp(34px, 4vw, 48px);
  line-height: 1.15;
}

.account-head > p:last-child {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}

.account-card {
  margin-top: 26px;
  padding: 26px 28px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.account-card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.account-card-head h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
}

.account-role {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(47, 74, 58, 0.09);
  color: var(--green);
  font-size: 11px;
}

.account-hint {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.account-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 20px;
}

.account-form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px;
}

.account-form .btn {
  align-self: flex-start;
  margin-top: 4px;
}

.account-form .text-input:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .account-page {
    padding-top: 34px;
  }

  .account-card {
    padding: 22px 18px;
  }

  .account-form-row {
    grid-template-columns: 1fr;
  }
}
</style>
