<template>
  <div class="article-page">
    <div class="container article-shell">
      <RouterLink to="/blog" class="back-link">{{ t('article.back') }}</RouterLink>

      <div v-if="loading" class="article-loading">
        <div class="loading-line short"></div>
        <div class="loading-line title"></div>
        <div class="loading-line"></div>
        <div class="loading-line"></div>
      </div>

      <article v-else-if="article" class="article">
        <div v-if="userStore.isAdmin" class="article-admin-actions">
          <RouterLink :to="`/admin/articles?edit=${article.id}`" class="btn btn-outline btn-small">
            {{ t('article.edit') }}
          </RouterLink>
          <RouterLink to="/admin/articles?new=1" class="btn btn-outline btn-small">
            {{ t('blog.write') }}
          </RouterLink>
        </div>
        <header class="article-header">
          <div class="article-meta">
            <span class="article-label">{{ articleLabel(article.label, t) }}</span>
            <span>{{ formatDate(article.createTime) }}</span>
            <span>{{ t('article.minutes', { n: readMinutes }) }}</span>
          </div>
          <h1 class="display">{{ article.title }}</h1>
          <p class="article-lede">{{ lede }}</p>
        </header>

        <div class="article-rule">
          <span></span>
          <b>{{ site.mark }}</b>
          <span></span>
        </div>

        <MarkdownContent :content="article.content" />

        <footer class="article-footer">
          <div>
            <p class="eyebrow">{{ t('article.endNote') }}</p>
            <p>{{ t('article.thanks') }}</p>
          </div>
          <div class="article-footer-actions">
            <button
              class="like-button"
              :class="{ active: liked }"
              type="button"
              :disabled="likeLoading"
              @click="toggleLike"
            >
              <span class="like-symbol">{{ liked ? '♥' : '♡' }}</span>
              <span>{{ liked ? t('article.liked', { n: likeCount }) : t('article.like', { n: likeCount }) }}</span>
            </button>
            <RouterLink to="/blog" class="btn btn-primary btn-small">{{ t('article.continue') }}</RouterLink>
          </div>
        </footer>

        <CommentSection :article-id="article.id" />
      </article>

      <div v-else class="empty-state">
        <strong>{{ t('article.notFoundTitle') }}</strong>
        {{ t('article.notFoundNote') }}
        <div class="empty-action">
          <RouterLink to="/blog" class="btn btn-outline btn-small">{{ t('article.backToBlog') }}</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import MarkdownContent from '@/components/MarkdownContent.vue'
import CommentSection from '@/components/CommentSection.vue'
import { site } from '@/config/site'
import { getArticle } from '@/api/blog'
import { getArticleLike, toggleArticleLike } from '@/api/likes'
import { useUserStore } from '@/stores/user'
import { formatDate, stripMarkdown } from '@/utils/format'
import { articleLabel } from '@/utils/articleLabel'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const article = ref(null)
const loading = ref(true)
const liked = ref(false)
const likeCount = ref(0)
const likeLoading = ref(false)

const lede = computed(() => stripMarkdown(article.value?.content, 120))
const readMinutes = computed(() => {
  const length = stripMarkdown(article.value?.content, 10000).length
  return Math.max(1, Math.ceil(length / 420))
})

async function loadLike() {
  if (!article.value) return
  try {
    const data = await getArticleLike(article.value.id)
    liked.value = Boolean(data?.liked)
    likeCount.value = Number(data?.count || 0)
  } catch {
    liked.value = false
    likeCount.value = 0
  }
}

async function toggleLike() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (likeLoading.value) return
  likeLoading.value = true
  try {
    const data = await toggleArticleLike(article.value.id)
    liked.value = Boolean(data?.liked)
    likeCount.value = Number(data?.count || 0)
  } catch {
    ElMessage.error(t('article.likeFailed'))
  } finally {
    likeLoading.value = false
  }
}

async function load() {
  loading.value = true
  article.value = null
  liked.value = false
  likeCount.value = 0
  try {
    article.value = await getArticle(route.params.id)
    await loadLike()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })
</script>

<style scoped>
.article-page {
  padding: 48px 0 40px;
}

.article-shell {
  max-width: 860px;
}

.article-admin-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 22px;
}

.back-link {
  display: inline-flex;
  margin-bottom: 38px;
  color: var(--muted);
  font-size: 12px;
  letter-spacing: 0.06em;
  transition: color 0.18s ease;
}

.back-link:hover {
  color: var(--accent);
}

.article-header {
  text-align: center;
}

.article-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 0.06em;
}

.article-label {
  color: var(--accent);
  font-weight: 600;
}

.article-header h1 {
  max-width: 780px;
  margin: 24px auto 22px;
  font-size: clamp(36px, 5vw, 62px);
  line-height: 1.2;
}

.article-lede {
  max-width: 620px;
  margin: 0 auto;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.9;
}

.article-rule {
  display: flex;
  align-items: center;
  gap: 18px;
  margin: 44px 0;
  color: var(--gold);
}

.article-rule span {
  flex: 1;
  height: 1px;
  background: var(--line);
}

.article-rule b {
  font-family: Georgia, serif;
  font-size: 18px;
  font-weight: 400;
}

.article-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-top: 64px;
  padding-top: 26px;
  border-top: 1px solid var(--line);
}

.article-footer p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.article-footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.like-button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 15px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.like-button:hover,
.like-button.active {
  border-color: var(--accent);
  color: var(--accent);
}

.like-button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.like-symbol {
  font-size: 16px;
  line-height: 1;
}

.article-loading {
  padding: 80px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-line {
  height: 14px;
  border-radius: 4px;
  background: var(--paper-deep);
}

.loading-line.short {
  width: 120px;
}

.loading-line.title {
  width: 70%;
  height: 48px;
  margin: 18px 0;
}

.empty-action {
  margin-top: 22px;
}

@media (max-width: 640px) {
  .article-page {
    padding-top: 30px;
  }

  .article-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .article-footer-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
