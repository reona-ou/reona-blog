<template>
  <div class="home-page">
    <section class="container home-section">
      <div class="home-head">
        <h1 class="display">{{ t('home.latestTitle') }}</h1>
        <div class="home-head-actions">
          <RouterLink v-if="userStore.isAdmin" to="/admin/articles?new=1" class="btn btn-primary btn-small">
            {{ t('blog.write') }}
          </RouterLink>
          <RouterLink to="/blog" class="text-link">{{ t('home.allArticles') }} <span>→</span></RouterLink>
        </div>
      </div>

      <div v-if="loading" class="home-loading">
        <div v-for="n in 3" :key="n" class="skeleton"></div>
      </div>

      <div v-else-if="articles.length" class="home-list">
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
      </div>

      <div v-else class="empty-state">
        <strong>{{ t('home.emptyTitle') }}</strong>
        {{ t('home.emptyNote') }}
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import ArticleCard from '@/components/ArticleCard.vue'
import { listArticles } from '@/api/blog'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(true)
const articles = ref([])

async function load() {
  loading.value = true
  try {
    const list = (await listArticles()) || []
    articles.value = list.slice(0, 3)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 76px);
  padding: 56px 0 30px;
}

.home-section {
  max-width: 820px;
}

.home-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 10px;
}

.home-head h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 44px);
  line-height: 1.2;
}

.home-head-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.home-list {
  border-bottom: 1px solid var(--line-strong);
}

.home-list :deep(.article-card) {
  min-height: 0;
  padding: 26px 0 24px;
}

.home-list :deep(.article-card h3) {
  margin-top: 18px;
}

.home-list :deep(.article-card p) {
  max-width: 620px;
}

.home-list :deep(.article-card .article-more) {
  padding-top: 18px;
}

.home-loading {
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid var(--line-strong);
}

.skeleton {
  height: 132px;
  border-top: 1px solid var(--line-strong);
  background: linear-gradient(90deg, var(--paper-deep), rgba(255, 253, 248, 0.5), var(--paper-deep));
  background-size: 200% 100%;
  opacity: 0.6;
  animation: home-shimmer 1.4s infinite linear;
}

@keyframes home-shimmer {
  to {
    background-position: -200% 0;
  }
}

@media (max-width: 640px) {
  .home-page {
    padding-top: 36px;
  }

  .home-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .home-head h1 {
    font-size: 32px;
  }

  .home-list :deep(.article-card) {
    padding: 22px 0 20px;
  }

  .home-list :deep(.article-card h3) {
    margin-top: 14px;
    font-size: 21px;
  }

  .home-list :deep(.article-card .article-more) {
    padding-top: 14px;
  }
}
</style>
