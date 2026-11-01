<template>
  <div>
    <div class="admin-page-head">
      <div>
        <h2>{{ t('admin.overview.headline') }}</h2>
        <p>{{ t('admin.overview.intro') }}</p>
      </div>
      <span class="badge">{{ site.name }} STUDIO</span>
    </div>

    <div class="admin-stat-grid">
      <div class="admin-stat">
        <span>{{ t('admin.overview.articles') }}</span>
        <strong>{{ stats.articles }}</strong>
        <small>{{ t('admin.overview.articlesHint') }}</small>
      </div>
      <div class="admin-stat">
        <span>{{ t('admin.overview.published') }}</span>
        <strong>{{ stats.published }}</strong>
        <small>{{ t('admin.overview.publishedHint') }}</small>
      </div>
      <div class="admin-stat">
        <span>{{ t('admin.overview.comments') }}</span>
        <strong>{{ stats.comments }}</strong>
        <small>{{ t('admin.overview.commentsHint') }}</small>
      </div>
    </div>

    <div class="admin-panel">
      <div class="admin-page-head" style="margin-bottom: 10px">
        <div>
          <h2 style="font-size: 20px">{{ t('admin.overview.recent') }}</h2>
          <p>{{ t('admin.overview.recentHint') }}</p>
        </div>
        <el-button text @click="$router.push('/admin/articles')">{{ t('admin.overview.manage') }}</el-button>
      </div>
      <el-table :data="recentArticles" style="width: 100%">
        <el-table-column prop="title" :label="t('admin.common.title')" min-width="280">
          <template #default="{ row }">
            <div class="admin-product-name">{{ row.title }}</div>
            <div class="admin-product-desc">{{ row.label ? articleLabel(row.label, t) : t('admin.common.uncategorized') }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.common.status')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enable ? 'success' : 'info'">
              {{ row.enable ? t('admin.common.published') : t('admin.common.draft') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.common.updatedAt')" width="160">
          <template #default="{ row }">{{ formatDate(row.updateTime, true) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.common.actions')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.enable" text type="primary" @click="view(row)">{{ t('admin.common.view') }}</el-button>
            <el-button v-else text type="primary" @click="$router.push('/admin/articles')">{{ t('admin.common.edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="admin-quick-grid">
      <button class="admin-quick" type="button" @click="$router.push('/admin/articles?new=1')">
        <span>
          <b>{{ t('admin.overview.write') }}</b>
          <span>{{ t('admin.overview.writeHint') }}</span>
        </span>
        <el-icon><EditPen /></el-icon>
      </button>
      <button class="admin-quick" type="button" @click="openSite">
        <span>
          <b>{{ t('admin.overview.viewSite') }}</b>
          <span>{{ t('admin.overview.viewSiteHint') }}</span>
        </span>
        <el-icon><View /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getArticleStats, pageArticles } from '@/api/blog'
import { getCommentCount } from '@/api/comment'
import { site } from '@/config/site'
import { articleLabel } from '@/utils/articleLabel'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const stats = reactive({ articles: 0, published: 0, comments: 0 })
const recentArticles = ref([])

function view(row) {
  window.open(`/blog/${row.id}`, '_blank')
}

function openSite() {
  window.open('/', '_blank')
}

async function load() {
  const [articleData, articleStats, commentCount] = await Promise.all([
    pageArticles({ pageNum: 1, pageSize: 5 }).catch(() => null),
    getArticleStats().catch(() => null),
    getCommentCount().catch(() => 0)
  ])
  stats.articles = Number(articleStats?.total || 0)
  stats.published = Number(articleStats?.published || 0)
  stats.comments = Number(commentCount || 0)
  recentArticles.value = (articleData?.records || []).slice(0, 5)
}

onMounted(load)
</script>
