<template>
  <div class="not-found">
    <div class="not-found-inner container">
      <span class="not-found-mark display">404</span>
      <h1 class="display">{{ t('notFound.title') }}</h1>
      <p>{{ t('notFound.text') }}</p>
      <div class="not-found-actions">
        <RouterLink to="/" class="btn btn-primary">{{ t('notFound.home') }}</RouterLink>
        <RouterLink to="/blog" class="btn btn-outline">{{ t('notFound.blog') }}</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped>
.not-found {
  min-height: calc(100vh - 76px);
  display: grid;
  place-items: center;
  padding: 60px 0;
}

.not-found-inner {
  max-width: 620px;
  text-align: center;
}

.not-found-mark {
  display: block;
  color: var(--line-strong);
  font-size: clamp(110px, 20vw, 220px);
  line-height: 0.8;
  letter-spacing: -0.08em;
}

.not-found h1 {
  margin: 26px 0 12px;
  font-size: clamp(30px, 4vw, 48px);
}

.not-found p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}

.not-found-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 28px;
  flex-wrap: wrap;
}
</style>
