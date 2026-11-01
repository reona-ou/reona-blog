<template>
  <header class="site-header">
    <div class="header-inner container">
      <RouterLink to="/" class="brand" @click="menuOpen = false">
        <span class="brand-mark">{{ site.mark }}</span>
        <span class="brand-text">
          <b>{{ site.name }}</b>
          <small>{{ t('site.titleSuffix') }}</small>
        </span>
      </RouterLink>

      <nav class="desktop-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ active: isActive(item) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="header-actions">
        <el-dropdown class="lang-dropdown" trigger="click" @command="setLanguage">
          <button class="lang-trigger" type="button" :aria-label="t('language.label')">
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

        <RouterLink v-if="userStore.isAdmin" to="/admin/overview" class="admin-entry">
          {{ t('header.adminEntry') }}
        </RouterLink>

        <el-dropdown v-if="userStore.isLoggedIn" trigger="click" @command="handleCommand">
          <button class="user-trigger" type="button">
            <span class="user-avatar">{{ initial }}</span>
            <span class="user-name">{{ userStore.displayName }}</span>
            <el-icon class="user-caret"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="account">
                <el-icon><User /></el-icon>{{ t('account.title') }}
              </el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="admin">
                <el-icon><Setting /></el-icon>{{ t('common.admin') }}
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>{{ t('common.logout') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <RouterLink v-else to="/login" class="login-link">{{ t('common.login') }}</RouterLink>

        <button class="menu-toggle" type="button" :aria-label="t('header.menu')" @click="menuOpen = !menuOpen">
          <el-icon><component :is="menuOpen ? Close : Menu" /></el-icon>
        </button>
      </div>
    </div>

    <transition name="mobile-menu">
      <div v-if="menuOpen" class="mobile-nav">
        <div class="container mobile-nav-inner">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="mobile-link"
            :class="{ active: isActive(item) }"
            @click="menuOpen = false"
          >
            {{ item.label }}
          </RouterLink>
          <RouterLink v-if="userStore.isAdmin" to="/admin/overview" class="mobile-link" @click="menuOpen = false">
            {{ t('common.admin') }}
          </RouterLink>
          <RouterLink v-if="userStore.isLoggedIn" to="/account" class="mobile-link" @click="menuOpen = false">
            {{ t('account.title') }}
          </RouterLink>
          <RouterLink v-if="!userStore.isLoggedIn" to="/login" class="mobile-link" @click="menuOpen = false">
            {{ t('header.loginRegister') }}
          </RouterLink>
          <button v-else class="mobile-link mobile-logout" type="button" @click="handleCommand('logout')">
            {{ t('common.logout') }}
          </button>
          <div class="mobile-langs">
            <button
              v-for="lang in languages"
              :key="lang.code"
              type="button"
              :class="{ active: locale === lang.code }"
              @click="setLanguage(lang.code)"
            >
              {{ lang.label }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Close, Menu } from '@element-plus/icons-vue'
import { site } from '@/config/site'
import { supportedLocales } from '@/i18n'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()
const menuOpen = ref(false)
const languages = supportedLocales

const navItems = computed(() => [
  { label: t('nav.home'), to: '/' },
  { label: t('nav.blog'), to: '/blog' },
  { label: t('nav.about'), to: '/about' }
])

const initial = computed(() => (userStore.displayName || 'R').slice(0, 1).toUpperCase())
const currentShort = computed(() => {
  return languages.find(item => item.code === locale.value)?.short || '中'
})

function isActive(item) {
  if (item.to === '/') return route.path === '/'
  return route.path.startsWith(item.to)
}

function setLanguage(code) {
  locale.value = code
  localStorage.setItem('site_language', code)
  menuOpen.value = false
}

function handleCommand(command) {
  menuOpen.value = false
  if (command === 'account') router.push('/account')
  if (command === 'admin') router.push('/admin/overview')
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success(t('common.logout'))
    router.push('/')
  }
}

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  }
)
</script>

<style scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(244, 239, 230, 0.88);
  border-bottom: 1px solid rgba(203, 191, 174, 0.55);
  backdrop-filter: blur(18px);
}

.header-inner {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  flex: 0 1 auto;
  min-width: 0;
}

.brand-text {
  min-width: 0;
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--ink);
  color: var(--paper);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 19px;
}

.brand-text b {
  display: block;
  max-width: 230px;
  overflow: hidden;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.18em;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-text small {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.16em;
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: 30px;
  flex: 1;
}

.nav-link {
  position: relative;
  padding: 6px 0;
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.04em;
  transition: color 0.18s ease;
}

.nav-link::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 1px;
  background: var(--accent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.2s ease;
}

.nav-link:hover,
.nav-link.active {
  color: var(--ink);
}

.nav-link.active::after {
  transform: scaleX(1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.lang-trigger {
  min-width: 34px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.04em;
  cursor: pointer;
}

.lang-trigger:hover {
  border-color: var(--ink);
  color: var(--ink);
}

:deep(.is-current-language) {
  color: var(--accent) !important;
  font-weight: 600;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 0;
  background: transparent;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 999px;
}

.user-avatar {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--green);
  color: #fff;
  font-size: 12px;
}

.user-name {
  max-width: 90px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 12px;
}

.user-caret {
  font-size: 11px;
  color: var(--muted);
}

.login-link {
  padding: 8px 18px;
  border: 1px solid var(--line-strong);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.18s ease;
}

.login-link:hover {
  border-color: var(--ink);
  background: var(--ink);
  color: var(--paper);
}

.admin-entry {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(181, 80, 46, 0.1);
  color: var(--accent);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.admin-entry:hover {
  background: var(--accent);
  color: #fff;
}

.menu-toggle {
  display: none;
  width: 36px;
  height: 36px;
  border: 0;
  background: transparent;
  place-items: center;
  cursor: pointer;
}

.mobile-nav {
  display: none;
  border-top: 1px solid var(--line);
  background: var(--paper);
}

.mobile-nav-inner {
  display: flex;
  flex-direction: column;
  padding: 10px 0 18px;
}

.mobile-link {
  padding: 12px 4px;
  border-bottom: 1px solid rgba(221, 211, 196, 0.6);
  font-size: 14px;
  color: var(--ink-soft);
}

.mobile-link.active {
  color: var(--accent);
}

.mobile-logout {
  border: 0;
  background: transparent;
  text-align: left;
}

.mobile-langs {
  display: flex;
  gap: 8px;
  padding-top: 14px;
}

.mobile-langs button {
  padding: 6px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 12px;
  cursor: pointer;
}

.mobile-langs button.active {
  border-color: var(--ink);
  background: var(--ink);
  color: var(--paper);
}

.mobile-menu-enter-active,
.mobile-menu-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.mobile-menu-enter-from,
.mobile-menu-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 900px) {
  .desktop-nav {
    display: none;
  }

  .header-inner {
    justify-content: space-between;
  }

  .menu-toggle {
    display: grid;
  }

  .mobile-nav {
    display: block;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 560px) {
  .header-inner {
    height: 66px;
    gap: 12px;
  }

  .brand-text b {
    max-width: 200px;
  }

  .brand-text small {
    display: none;
  }

  .admin-entry {
    display: none;
  }

  .login-link {
    padding: 7px 14px;
  }
}

@media (max-width: 400px) {
  .lang-dropdown {
    display: none;
  }

  .brand-text b {
    max-width: 190px;
    font-size: 15px;
    letter-spacing: 0.12em;
  }
}
</style>
