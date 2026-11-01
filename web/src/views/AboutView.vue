<template>
  <div class="about-page">
    <section class="container about-head">
      <h1 class="display">{{ t('nav.about') }}</h1>
    </section>

    <section class="container about-grid">
      <div class="about-section">
        <span class="about-number">01</span>
        <h2 class="display">{{ t('about.blogTitle') }}</h2>
        <p>{{ t('about.blogText') }}</p>
        <RouterLink to="/blog" class="text-link">{{ t('about.blogLink') }} <span>→</span></RouterLink>
      </div>

      <div class="about-section">
        <span class="about-number">02</span>
        <h2 class="display">{{ t('about.commentTitle') }}</h2>
        <p>{{ t('about.commentText') }}</p>
        <RouterLink v-if="!userStore.isLoggedIn" to="/login" class="text-link">{{ t('about.commentLink') }} <span>→</span></RouterLink>
        <span v-else class="about-tag">{{ t('comments.loggedHint', { name: userStore.displayName }) }}</span>
      </div>

      <div class="about-section">
        <span class="about-number">03</span>
        <h2 class="display">{{ t('about.likeTitle') }}</h2>
        <p>{{ t('about.likeText') }}</p>
        <RouterLink to="/blog" class="text-link">{{ t('about.likeLink') }} <span>→</span></RouterLink>
      </div>

      <div class="about-section">
        <span class="about-number">04</span>
        <h2 class="display">{{ t('about.contactTitle') }}</h2>
        <p>{{ t('about.contactText') }}</p>
        <a :href="`mailto:${site.email}`" class="text-link">{{ site.email }} <span>↗</span></a>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { site } from '@/config/site'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()
</script>

<style scoped>
.about-page {
  padding: 58px 0 30px;
}

.about-head h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.15;
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 70px;
  margin-top: 12px;
}

.about-section {
  position: relative;
  padding: 42px 0;
  border-bottom: 1px solid var(--line);
  scroll-margin-top: 100px;
}

.about-number {
  position: absolute;
  top: 44px;
  right: 0;
  color: var(--line-strong);
  font-family: Georgia, serif;
  font-size: 12px;
}

.about-section h2 {
  margin: 0 0 14px;
  font-size: 30px;
  font-weight: 500;
}

.about-section p {
  max-width: 470px;
  margin: 0 0 18px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.9;
}

.about-tag {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(47, 74, 58, 0.09);
  color: var(--green);
  font-size: 11px;
}

@media (max-width: 800px) {
  .about-page {
    padding-top: 44px;
  }

  .about-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
