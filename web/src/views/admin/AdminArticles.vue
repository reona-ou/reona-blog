<template>
  <div>
    <div class="admin-page-head">
      <div>
        <h2>{{ t('admin.articles.title') }}</h2>
        <p>{{ t('admin.articles.subtitle') }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><EditPen /></el-icon>
        {{ t('admin.articles.new') }}
      </el-button>
    </div>

    <div class="admin-panel">
      <el-table v-loading="loading" :data="articles" style="width: 100%">
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
        <el-table-column :label="t('admin.common.createdAt')" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime, true) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.common.updatedAt')" width="160">
          <template #default="{ row }">{{ formatDate(row.updateTime, true) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.common.actions')" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.enable" text type="primary" @click="view(row)">{{ t('admin.common.view') }}</el-button>
            <el-button text type="primary" @click="openEdit(row)">{{ t('admin.common.edit') }}</el-button>
            <el-button text type="danger" @click="remove(row)">{{ t('admin.common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="pageCount > 1" class="admin-pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          @current-change="goPage"
        />
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="form.id ? t('admin.articles.editTitle') : t('admin.articles.createTitle')"
      size="88%"
      destroy-on-close
    >
      <div class="article-meta-form">
        <label class="admin-form-field article-title-field">
          <span>{{ t('admin.articles.titleLabel') }}</span>
          <el-input v-model="form.title" maxlength="32" show-word-limit :placeholder="t('admin.articles.titlePlaceholder')" />
        </label>
        <label class="admin-form-field">
          <span>{{ t('admin.articles.labelLabel') }}</span>
          <el-select v-model="form.label" filterable allow-create default-first-option :placeholder="t('admin.articles.labelPlaceholder')">
            <el-option v-for="label in labelOptions" :key="label" :label="articleLabel(label, t)" :value="label" />
          </el-select>
        </label>
        <label class="admin-form-field article-publish-field">
          <span>{{ t('admin.articles.publishStatus') }}</span>
          <el-switch v-model="form.enable" :active-text="t('admin.articles.publish')" :inactive-text="t('admin.articles.draft')" />
        </label>
      </div>

      <div class="editor-layout">
        <div class="editor-pane">
          <div class="editor-toolbar">
            <button type="button" @click="insert('## ', '', t('admin.articles.insertHeading'))">H2</button>
            <button type="button" @click="insert('**', '**', t('admin.articles.insertBold'))">{{ t('admin.articles.bold') }}</button>
            <button type="button" @click="insert('> ', '', t('admin.articles.insertQuote'))">{{ t('admin.articles.quote') }}</button>
            <button type="button" @click="insert('[', '](https://)', t('admin.articles.insertLink'))">{{ t('admin.articles.link') }}</button>
            <button type="button" @click="fileInput?.click()">{{ t('admin.articles.image') }}</button>
            <span class="editor-toolbar-note">{{ uploading ? t('admin.articles.uploading') : t('admin.articles.markdown') }}</span>
            <input ref="fileInput" type="file" accept="image/*" hidden @change="handleFile" />
          </div>
          <textarea
            ref="textareaRef"
            v-model="form.content"
            class="editor-textarea"
            :placeholder="t('admin.articles.contentPlaceholder')"
          ></textarea>
        </div>
        <div class="editor-pane">
          <div class="editor-toolbar">
            <span class="editor-toolbar-note">{{ t('admin.articles.preview') }}</span>
          </div>
          <div class="editor-preview">
            <MarkdownContent v-if="form.content" :content="form.content" />
            <p v-else class="admin-empty">{{ t('admin.articles.previewEmpty') }}</p>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="drawerVisible = false">{{ t('admin.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          <el-icon><Check /></el-icon>
          {{ t('admin.articles.saveArticle') }}
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { deleteArticle, getArticle, pageArticles, saveArticle } from '@/api/blog'
import { uploadImage } from '@/api/upload'
import { articleLabel } from '@/utils/articleLabel'
import { formatDate, storedAsset } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const articles = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const drawerVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const textareaRef = ref(null)
const fileInput = ref(null)

const labelOptions = ['随笔', '日常', '关于', '记录']

const form = reactive({
  id: null,
  title: '',
  label: '随笔',
  content: '',
  enable: true
})

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function load() {
  loading.value = true
  try {
    const data = await pageArticles({ pageNum: pageNum.value, pageSize })
    articles.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function goPage(page) {
  pageNum.value = page
  load()
}

function view(row) {
  window.open(`/blog/${row.id}`, '_blank')
}

function resetForm() {
  Object.assign(form, {
    id: null,
    title: '',
    label: '随笔',
    content: '',
    enable: true
  })
}

function openCreate() {
  resetForm()
  drawerVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    title: row.title || '',
    label: row.label || '随笔',
    content: row.content || '',
    enable: Boolean(row.enable)
  })
  drawerVisible.value = true
}

function insertText(text) {
  const el = textareaRef.value
  if (!el) {
    form.content += text
    return
  }
  const start = el.selectionStart
  const end = el.selectionEnd
  form.content = form.content.slice(0, start) + text + form.content.slice(end)
  nextTick(() => {
    el.focus()
    el.selectionStart = el.selectionEnd = start + text.length
  })
}

function insert(prefix, suffix, placeholder) {
  const el = textareaRef.value
  if (!el) {
    form.content += `${prefix}${placeholder}${suffix}`
    return
  }
  const start = el.selectionStart
  const end = el.selectionEnd
  const selected = form.content.slice(start, end) || placeholder
  insertText(`${prefix}${selected}${suffix}`)
}

async function handleFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const url = await uploadImage(file)
    if (!url) throw new Error(t('admin.articles.uploadFailed'))
    insertText(`\n\n![${file.name.replace(/[[\]]/g, '')}](${storedAsset(url)})\n\n`)
    ElMessage.success(t('admin.articles.imageInserted'))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    uploading.value = false
  }
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning(t('admin.articles.titleRequired'))
    return
  }
  if ((form.label || '').length > 32) {
    ElMessage.warning(t('admin.articles.labelTooLong'))
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning(t('admin.articles.contentRequired'))
    return
  }
  if (new TextEncoder().encode(form.content).length > 60000) {
    ElMessage.warning(t('admin.articles.contentTooLong'))
    return
  }
  saving.value = true
  const creating = !form.id
  try {
    await saveArticle({
      id: form.id || undefined,
      title: form.title.trim(),
      label: form.label || '随笔',
      content: form.content,
      enable: form.enable
    })
    ElMessage.success(creating ? t('admin.articles.saved') : t('admin.articles.updated'))
    drawerVisible.value = false
    if (creating) pageNum.value = 1
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(t('admin.articles.deleteConfirm', { title: row.title }), t('admin.articles.deleteTitle'), {
      confirmButtonText: t('admin.common.delete'),
      cancelButtonText: t('admin.common.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteArticle(row.id)
    ElMessage.success(t('admin.articles.deleted'))
    if (articles.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function openFromQuery() {
  if (route.query.new === '1') {
    openCreate()
    router.replace({ query: {} })
    return
  }
  const id = Number(route.query.edit)
  if (!Number.isInteger(id) || id <= 0) {
    if (route.query.edit !== undefined) router.replace({ query: {} })
    return
  }
  try {
    const article = await getArticle(id)
    if (article) {
      openEdit(article)
    } else {
      ElMessage.error(t('article.notFoundTitle'))
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    router.replace({ query: {} })
  }
}

watch([() => route.query.new, () => route.query.edit], openFromQuery)
onMounted(async () => {
  await load()
  await nextTick()
  await openFromQuery()
})
</script>

<style scoped>
.article-meta-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px 150px;
  gap: 14px;
  margin-bottom: 16px;
}

.article-title-field {
  min-width: 0;
}

.article-publish-field {
  justify-content: flex-end;
}

.editor-toolbar-note {
  margin-left: auto;
  color: #9a8f80;
  font-size: 11px;
}

.admin-form-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  font-size: 12px;
  color: #6f6558;
}

@media (max-width: 900px) {
  .article-meta-form {
    grid-template-columns: 1fr;
  }
}
</style>
