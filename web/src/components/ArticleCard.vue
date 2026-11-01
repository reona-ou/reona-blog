<template>
  <RouterLink :to="`/blog/${article.id}`" class="article-card" :class="{ featured }">
    <div class="article-topline">
      <span class="article-label">
        <span v-if="featured" class="article-index">{{ String(index + 1).padStart(2, '0') }}</span>
        {{ articleLabel(article.label, t) }}
      </span>
      <span class="article-date">{{ formatDate(article.createTime) }}</span>
    </div>
    <h3>{{ article.title }}</h3>
    <p>{{ stripMarkdown(article.summary || article.content, featured ? 150 : 96) }}</p>
    <span class="article-more">
      {{ t('common.readMore') }}
      <span>→</span>
    </span>
  </RouterLink>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatDate, stripMarkdown } from '@/utils/format'
import { articleLabel } from '@/utils/articleLabel'

const { t } = useI18n()

defineProps({
  article: { type: Object, required: true },
  index: { type: Number, default: 0 },
  featured: { type: Boolean, default: false }
})
</script>

<style scoped>
.article-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 250px;
  padding: 22px 0 20px;
  border-top: 1px solid var(--line-strong);
  transition: color 0.2s ease;
}

.article-card:hover h3 {
  color: var(--accent);
}

.article-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 0.06em;
}

.article-label {
  color: var(--accent);
  font-weight: 600;
}

.article-index {
  margin-right: 8px;
  color: var(--gold);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 12px;
  letter-spacing: 0.06em;
}

.article-card h3 {
  position: relative;
  margin: 34px 0 12px;
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 500;
  line-height: 1.35;
  letter-spacing: -0.02em;
  transition: color 0.2s ease;
}

.article-card p {
  position: relative;
  margin: 0;
  max-width: 440px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}

.article-more {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 26px;
  color: var(--ink-soft);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.article-more span {
  transition: transform 0.2s ease;
}

.article-card:hover .article-more span {
  transform: translateX(4px);
}

.article-card.featured {
  min-height: 100%;
  padding: 28px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--green);
  color: var(--paper);
  overflow: hidden;
}

.article-card.featured .article-topline,
.article-card.featured .article-label,
.article-card.featured .article-date,
.article-card.featured .article-more {
  color: rgba(244, 239, 230, 0.66);
}

.article-card.featured .article-label {
  color: #e5b987;
}

.article-card.featured h3 {
  margin-top: 58px;
  max-width: 440px;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.25;
  color: var(--paper);
}

.article-card.featured p {
  color: rgba(244, 239, 230, 0.66);
  font-size: 14px;
}

.article-card.featured .article-more {
  color: var(--paper);
}

.article-card.featured:hover h3 {
  color: #fff;
}
</style>
