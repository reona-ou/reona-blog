<template>
  <div class="blog-page">
    <section class="blog-head container">
      <div class="blog-head-top">
        <h1 class="display">{{ t('nav.blog') }}</h1>
        <RouterLink v-if="userStore.isAdmin" to="/admin/articles?new=1" class="btn btn-primary btn-small">
          {{ t('blog.write') }}
        </RouterLink>
      </div>
      <div class="blog-toolbar">
        <div class="label-list">
          <button
            v-for="label in labels"
            :key="label"
            class="chip"
            :class="{ 'is-active': activeLabel === label }"
            type="button"
            @click="activeLabel = label"
          >
            {{ labelText(label) }}
          </button>
        </div>
        <label class="blog-search">
          <el-icon><Search /></el-icon>
          <input v-model="keyword" type="search" :placeholder="t('blog.search')" />
        </label>
      </div>
    </section>

    <section class="container blog-content">
      <div class="blog-count">
        <span>{{ t('blog.count', { n: filteredArticles.length }) }}</span>
        <span class="count-line"></span>
      </div>

      <div v-if="loading" class="blog-grid">
        <div v-for="n in 4" :key="n" class="blog-skeleton"></div>
      </div>

      <div v-else-if="filteredArticles.length" class="blog-grid">
        <ArticleCard
          v-for="(article, index) in filteredArticles"
          :key="article.id"
          :article="article"
          :index="index"
        />
      </div>

      <div v-else class="empty-state">
        <strong>{{ t('blog.emptyTitle') }}</strong>
        {{ t('blog.emptyNote') }}
        <div class="empty-action">
          <button class="btn btn-outline btn-small" type="button" @click="resetFilter">{{ t('blog.clear') }}</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import ArticleCard from '@/components/ArticleCard.vue'
import { listArticles } from '@/api/blog'
import { stripMarkdown } from '@/utils/format'
import { articleLabel } from '@/utils/articleLabel'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()
const articles = ref([])
const loading = ref(true)
const activeLabel = ref('全部')
const keyword = ref('')

const labels = computed(() => {
  const values = ['全部', ...new Set(articles.value.map(article => article.label).filter(Boolean))]
  return values
})

function labelText(label) {
  return label === '全部' ? t('labels.all') : articleLabel(label, t)
}

const filteredArticles = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return articles.value.filter(article => {
    const matchLabel = activeLabel.value === '全部' || article.label === activeLabel.value
    if (!matchLabel) return false
    if (!query) return true
    const haystack = `${article.title || ''} ${stripMarkdown(article.summary || article.content, 3000)}`.toLowerCase()
    return haystack.includes(query)
  })
})

function resetFilter() {
  activeLabel.value = '全部'
  keyword.value = ''
}

async function load() {
  loading.value = true
  try {
    articles.value = (await listArticles()) || []
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.blog-head {
  padding-top: 58px;
  padding-bottom: 8px;
}

.blog-head-top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.blog-head h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.15;
}

.blog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.label-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.blog-search {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 240px;
  padding: 9px 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--muted);
  background: rgba(255, 253, 248, 0.5);
}

.blog-search input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--ink);
  font-size: 12px;
  outline: none;
}

.blog-search input::placeholder {
  color: #a89e91;
}

.blog-content {
  padding-bottom: 32px;
}

.blog-count {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 22px;
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.count-line {
  flex: 1;
  height: 1px;
  background: var(--line);
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 56px;
  padding-bottom: 40px;
  border-bottom: 1px solid var(--line-strong);
}

.blog-skeleton {
  height: 260px;
  margin-bottom: 24px;
  border-radius: var(--radius-md);
  background: var(--paper-deep);
}

.empty-action {
  margin-top: 22px;
}

@media (max-width: 800px) {
  .blog-head {
    padding-top: 44px;
  }

  .blog-head-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .blog-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .blog-search {
    width: 100%;
  }

  .blog-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
