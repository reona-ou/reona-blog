<template>
  <footer class="site-footer">
    <div class="container footer-inner">
      <div class="footer-brand">
        <div class="footer-mark">{{ site.mark }}</div>
        <div>
          <b>{{ site.name }}</b>
          <p>{{ t('footer.description') }}</p>
        </div>
      </div>

      <div class="footer-columns">
        <div class="footer-column">
          <span class="footer-label">{{ t('footer.browse') }}</span>
          <RouterLink to="/blog">{{ t('footer.blog') }}</RouterLink>
          <RouterLink to="/about">{{ t('footer.about') }}</RouterLink>
        </div>
        <div class="footer-column">
          <span class="footer-label">{{ t('footer.account') }}</span>
          <RouterLink v-if="!userStore.isLoggedIn" to="/login">{{ t('footer.login') }}</RouterLink>
          <RouterLink v-if="!userStore.isLoggedIn" to="/register">{{ t('footer.register') }}</RouterLink>
          <RouterLink v-if="userStore.isAdmin" to="/admin/overview">{{ t('footer.admin') }}</RouterLink>
          <button v-if="userStore.isLoggedIn" class="footer-button" type="button" @click="logout">
            {{ t('footer.logout') }}
          </button>
        </div>
        <div class="footer-column">
          <span class="footer-label">{{ t('footer.contact') }}</span>
          <a :href="`mailto:${site.email}`">{{ site.email }}</a>
        </div>
      </div>
    </div>

    <div class="container footer-bottom">
      <span>{{ t('footer.copyright', { year: new Date().getFullYear(), name: site.name, suffix: t('site.titleSuffix') }) }}</span>
      <span>{{ t('footer.powered') }}</span>
    </div>
  </footer>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { site } from '@/config/site'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

function logout() {
  userStore.logout()
  ElMessage.success(t('common.logout'))
  router.push('/')
}
</script>

<style scoped>
.site-footer {
  margin-top: 40px;
  padding: 64px 0 26px;
  background: #23201c;
  color: #f4efe6;
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  gap: 60px;
}

.footer-brand {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.footer-mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #f4efe6;
  color: #23201c;
  font-family: Georgia, serif;
  font-size: 24px;
}

.footer-brand b {
  display: block;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0.2em;
}

.footer-brand p {
  margin: 8px 0 0;
  max-width: 280px;
  color: rgba(244, 239, 230, 0.55);
  font-size: 13px;
  line-height: 1.7;
}

.footer-columns {
  display: flex;
  gap: 74px;
}

.footer-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 120px;
}

.footer-label {
  margin-bottom: 4px;
  color: rgba(244, 239, 230, 0.35);
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.footer-column a,
.footer-column span,
.footer-button {
  color: rgba(244, 239, 230, 0.72);
  font-size: 13px;
  transition: color 0.18s ease;
}

.footer-column a:hover,
.footer-button:hover {
  color: #fff;
}

.footer-button {
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-top: 54px;
  padding-top: 20px;
  border-top: 1px solid rgba(244, 239, 230, 0.12);
  color: rgba(244, 239, 230, 0.38);
  font-size: 11px;
  letter-spacing: 0.04em;
}

@media (max-width: 800px) {
  .footer-inner {
    flex-direction: column;
    gap: 38px;
  }

  .footer-columns {
    gap: 34px;
    flex-wrap: wrap;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
