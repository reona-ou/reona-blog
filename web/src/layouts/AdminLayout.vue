<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink to="/" class="admin-brand">
        <span class="admin-brand-mark">{{ site.mark }}</span>
        <span>
          <b>{{ site.name }}</b>
          <small>{{ t('admin.shell.panel') }}</small>
        </span>
      </RouterLink>

      <nav class="admin-nav">
        <RouterLink to="/admin/overview" class="admin-nav-link">
          <el-icon><Grid /></el-icon>
          <span>{{ t('admin.shell.navOverview') }}</span>
        </RouterLink>
        <RouterLink to="/admin/articles" class="admin-nav-link">
          <el-icon><Document /></el-icon>
          <span>{{ t('admin.shell.navArticles') }}</span>
        </RouterLink>
        <RouterLink to="/admin/users" class="admin-nav-link">
          <el-icon><User /></el-icon>
          <span>{{ t('admin.shell.navUsers') }}</span>
        </RouterLink>
        <RouterLink v-if="userStore.isSuperAdmin" to="/admin/assistant" class="admin-nav-link">
          <el-icon><ChatDotRound /></el-icon>
          <span>{{ t('admin.shell.navAssistant') }}</span>
        </RouterLink>
        <RouterLink v-if="userStore.isSuperAdmin" to="/admin/settings" class="admin-nav-link">
          <el-icon><Setting /></el-icon>
          <span>{{ t('admin.shell.navSettings') }}</span>
        </RouterLink>
      </nav>

      <div class="admin-sidebar-actions">
        <RouterLink to="/account" class="admin-side-link">
          <el-icon><User /></el-icon>
          <span>{{ t('account.title') }}</span>
        </RouterLink>
        <RouterLink to="/" class="admin-side-link">
          <el-icon><Back /></el-icon>
          <span>{{ t('admin.shell.backSite') }}</span>
        </RouterLink>
        <button class="admin-side-link" type="button" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>{{ t('admin.shell.logout') }}</span>
        </button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div>
          <p class="admin-topbar-kicker">{{ site.name }} STUDIO</p>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="admin-topbar-actions">
          <el-dropdown trigger="click" @command="setLanguage">
            <button class="admin-lang" type="button" :aria-label="t('language.label')">
              {{ currentShort }}
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="lang in languages"
                  :key="lang.code"
                  :command="lang.code"
                  :class="{ 'is-current-language': locale === lang.code }"
                >
                  {{ lang.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <div class="admin-user">
            <span class="admin-avatar">{{ initial }}</span>
            <div>
              <b>{{ userStore.displayName }}</b>
              <small>{{ roleLabel }}</small>
            </div>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { site } from '@/config/site'
import { supportedLocales } from '@/i18n'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()
const languages = supportedLocales

const pageTitle = computed(() => {
  const map = {
    'admin-overview': t('admin.shell.navOverview'),
    'admin-articles': t('admin.shell.navArticles'),
    'admin-users': t('admin.shell.navUsers'),
    'admin-assistant': t('admin.shell.navAssistant'),
    'admin-settings': t('admin.shell.navSettings')
  }
  return map[route.name] || t('admin.shell.panel')
})

const roleLabel = computed(() => (
  userStore.isSuperAdmin ? t('admin.shell.roleSuper') : t('admin.shell.roleAdmin')
))
const currentShort = computed(() => {
  return languages.find(item => item.code === locale.value)?.short || '中'
})

const initial = computed(() => (userStore.displayName || 'A').slice(0, 1).toUpperCase())

function setLanguage(code) {
  locale.value = code
  localStorage.setItem('site_language', code)
}

function logout() {
  userStore.logout()
  ElMessage.success(t('admin.shell.logoutSuccess'))
  router.push('/login')
}
</script>
