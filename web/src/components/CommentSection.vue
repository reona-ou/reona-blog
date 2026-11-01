<template>
  <section class="comments-section">
    <header class="comments-head">
      <div>
        <p class="eyebrow">{{ t('comments.eyebrow') }}</p>
        <h2 class="display">{{ t('comments.title') }} <span>{{ comments.length }}</span></h2>
      </div>
      <span class="comments-hint">
        {{ userStore.isLoggedIn ? t('comments.loggedHint', { name: userStore.displayName }) : t('comments.hint') }}
      </span>
    </header>

    <div v-if="userStore.isLoggedIn" class="comment-form">
      <div class="comment-form-top">
        <span class="comment-avatar">{{ initial }}</span>
        <span class="comment-form-name">{{ userStore.displayName }}</span>
      </div>
      <textarea
        v-model="content"
        maxlength="500"
        :placeholder="t('comments.placeholder')"
        :aria-label="t('comments.placeholder')"
      ></textarea>
      <div class="comment-form-bottom">
        <span>{{ content.length }} / 500</span>
        <button class="btn btn-primary btn-small" type="button" :disabled="submitting || !content.trim()" @click="submit">
          {{ submitting ? t('comments.submitting') : t('comments.submit') }}
        </button>
      </div>
    </div>

    <div v-else class="comment-login">
      <span>{{ t('comments.loginPrompt') }}</span>
      <RouterLink :to="{ name: 'login', query: { redirect: route.fullPath } }">{{ t('comments.loginAction') }}</RouterLink>
    </div>

    <div v-if="loading" class="comments-loading">
      <div class="comment-skeleton"></div>
      <div class="comment-skeleton"></div>
    </div>

    <div v-else-if="topLevel.length" class="comment-list">
      <article v-for="comment in topLevel" :key="comment.id" class="comment-item">
        <div class="comment-item-head">
          <span class="comment-avatar">{{ avatarOf(comment) }}</span>
          <div class="comment-item-meta">
            <b>{{ comment.nickname || comment.username }}</b>
            <span>{{ formatDate(comment.createTime, true) }}</span>
          </div>
          <button
            v-if="canDelete(comment)"
            class="comment-delete"
            type="button"
            :disabled="deletingId === comment.id"
            @click="remove(comment)"
          >
            {{ t('comments.delete') }}
          </button>
        </div>
        <p class="comment-text">{{ comment.content }}</p>
        <div class="comment-actions">
          <button
            class="comment-action"
            :class="{ active: comment.liked }"
            type="button"
            @click="like(comment)"
          >
            <span>{{ comment.liked ? '♥' : '♡' }}</span>
            {{ comment.liked ? t('comments.liked', { n: comment.likeCount || 0 }) : t('comments.like', { n: comment.likeCount || 0 }) }}
          </button>
          <button v-if="userStore.isLoggedIn" class="comment-action" type="button" @click="startReply(comment)">
            {{ t('comments.reply') }}
          </button>
        </div>

        <div v-if="replyTo?.id === comment.id" class="reply-form">
          <textarea
            v-model="replyContent"
            maxlength="500"
            :placeholder="t('comments.replyPlaceholder')"
            :aria-label="t('comments.replyPlaceholder')"
          ></textarea>
          <div class="reply-form-actions">
            <button class="comment-action" type="button" @click="cancelReply">{{ t('comments.cancel') }}</button>
            <button
              class="btn btn-primary btn-small"
              type="button"
              :disabled="replySubmitting || !replyContent.trim()"
              @click="submitReply"
            >
              {{ replySubmitting ? t('comments.replySubmitting') : t('comments.replySubmit') }}
            </button>
          </div>
        </div>

        <div v-if="repliesFor(comment.id).length" class="replies">
          <article v-for="reply in repliesFor(comment.id)" :key="reply.id" class="reply-item">
            <div class="comment-item-head">
              <span class="comment-avatar small">{{ avatarOf(reply) }}</span>
              <div class="comment-item-meta">
                <b>{{ reply.nickname || reply.username }}</b>
                <span>{{ formatDate(reply.createTime, true) }}</span>
              </div>
              <button
                v-if="canDelete(reply)"
                class="comment-delete"
                type="button"
                :disabled="deletingId === reply.id"
                @click="remove(reply)"
              >
                {{ t('comments.delete') }}
              </button>
            </div>
            <p v-if="parentName(reply)" class="reply-to">{{ t('comments.replyTo', { name: parentName(reply) }) }}</p>
            <p class="comment-text reply-text">{{ reply.content }}</p>
            <div class="comment-actions">
              <button
                class="comment-action"
                :class="{ active: reply.liked }"
                type="button"
                @click="like(reply)"
              >
                <span>{{ reply.liked ? '♥' : '♡' }}</span>
                {{ reply.liked ? t('comments.liked', { n: reply.likeCount || 0 }) : t('comments.like', { n: reply.likeCount || 0 }) }}
              </button>
            </div>
          </article>
        </div>
      </article>
    </div>

    <div v-else class="comments-empty">{{ t('comments.empty') }}</div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addComment, deleteComment, listComments } from '@/api/comment'
import { toggleCommentLike } from '@/api/likes'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const props = defineProps({
  articleId: { type: [Number, String], required: true }
})

const route = useRoute()
const { t } = useI18n()
const userStore = useUserStore()
const comments = ref([])
const loading = ref(true)
const submitting = ref(false)
const deletingId = ref(null)
const content = ref('')
const replyTo = ref(null)
const replyContent = ref('')
const replySubmitting = ref(false)
const likeBusy = ref(null)

const initial = computed(() => (userStore.displayName || 'R').slice(0, 1).toUpperCase())
const topLevel = computed(() => comments.value.filter(comment => !comment.parentId))
const repliesFor = id =>
  comments.value
    .filter(comment => Number(comment.parentId) === Number(id))
    .slice()
    .sort((a, b) => Number(a.id) - Number(b.id))

function avatarOf(comment) {
  return (comment.nickname || comment.username || 'R').slice(0, 1).toUpperCase()
}

function parentName(reply) {
  const parent = comments.value.find(comment => Number(comment.id) === Number(reply.parentId))
  return parent?.nickname || parent?.username || ''
}

function canDelete(comment) {
  if (!userStore.isLoggedIn) return false
  return userStore.isAdmin || comment.userId === userStore.user?.id
}

function goLogin() {
  window.location.assign(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
}

async function load() {
  loading.value = true
  try {
    comments.value = (await listComments(props.articleId)) || []
  } catch (error) {
    ElMessage.error(error.message || t('comments.actionFailed'))
  } finally {
    loading.value = false
  }
}

async function submit() {
  const text = content.value.trim()
  if (!text) return
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  submitting.value = true
  try {
    await addComment({ articleId: Number(props.articleId), content: text })
    content.value = ''
    await load()
  } catch (error) {
    ElMessage.error(error.message || t('comments.actionFailed'))
  } finally {
    submitting.value = false
  }
}

function startReply(comment) {
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  replyTo.value = comment
  replyContent.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyContent.value = ''
}

async function submitReply() {
  if (!replyTo.value || !replyContent.value.trim()) return
  replySubmitting.value = true
  try {
    await addComment({
      articleId: Number(props.articleId),
      parentId: replyTo.value.id,
      content: replyContent.value.trim()
    })
    cancelReply()
    await load()
  } catch (error) {
    ElMessage.error(error.message || t('comments.actionFailed'))
  } finally {
    replySubmitting.value = false
  }
}

async function like(comment) {
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  if (likeBusy.value === comment.id) return
  likeBusy.value = comment.id
  try {
    const data = await toggleCommentLike(comment.id)
    comment.liked = Boolean(data?.liked)
    comment.likeCount = Number(data?.count || 0)
  } catch (error) {
    ElMessage.error(error.message || t('comments.actionFailed'))
  } finally {
    likeBusy.value = null
  }
}

async function remove(comment) {
  try {
    await ElMessageBox.confirm(t('comments.deleteMessage'), t('comments.deleteTitle'), {
      confirmButtonText: t('comments.delete'),
      cancelButtonText: t('comments.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }
  deletingId.value = comment.id
  try {
    await deleteComment(comment.id)
    if (replyTo.value?.id === comment.id) cancelReply()
    await load()
  } catch (error) {
    ElMessage.error(error.message || t('comments.actionFailed'))
  } finally {
    deletingId.value = null
  }
}

watch(() => props.articleId, load, { immediate: true })
</script>

<style scoped>
.comments-section {
  max-width: 760px;
  margin: 72px auto 0;
  padding-top: 38px;
  border-top: 1px solid var(--line);
}

.comments-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.comments-head h2 {
  margin: 10px 0 0;
  font-size: 32px;
  font-weight: 500;
}

.comments-head h2 span {
  color: var(--muted);
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 400;
}

.comments-hint {
  color: var(--muted);
  font-size: 11px;
}

.comment-form {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
}

.comment-form-top {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 12px;
}

.comment-avatar {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  flex: 0 0 30px;
  border-radius: 50%;
  background: var(--green);
  color: #fff;
  font-size: 11px;
}

.comment-avatar.small {
  width: 26px;
  height: 26px;
  flex-basis: 26px;
  font-size: 10px;
  background: var(--green-soft);
}

.comment-form-name {
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 600;
}

.comment-form textarea,
.reply-form textarea {
  width: 100%;
  min-height: 92px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--paper);
  color: var(--ink);
  font-family: var(--font-body);
  font-size: 13px;
  line-height: 1.7;
  resize: vertical;
  outline: none;
}

.reply-form textarea {
  min-height: 76px;
  background: var(--surface);
}

.comment-form textarea:focus,
.reply-form textarea:focus {
  border-color: var(--green);
}

.comment-form-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 10px;
  color: var(--muted);
  font-size: 10px;
}

.comment-login {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-sm);
  color: var(--muted);
  font-size: 12px;
}

.comment-login a {
  color: var(--accent);
  font-weight: 600;
}

.comment-list {
  display: flex;
  flex-direction: column;
  margin-top: 26px;
}

.comment-item {
  padding: 20px 0;
  border-top: 1px solid var(--line);
}

.comment-item:first-child {
  border-top: 0;
}

.comment-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-item-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.comment-item-meta b {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
}

.comment-item-meta span {
  color: var(--muted);
  font-size: 10px;
}

.comment-delete {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 10px;
  cursor: pointer;
}

.comment-delete:hover {
  color: var(--accent);
}

.comment-text {
  margin: 10px 0 0 40px;
  color: var(--ink-soft);
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 10px 0 0 40px;
}

.comment-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 11px;
  cursor: pointer;
}

.comment-action:hover,
.comment-action.active {
  color: var(--accent);
}

.reply-form {
  margin: 12px 0 0 40px;
  padding: 14px;
  border-radius: var(--radius-sm);
  background: var(--paper);
}

.reply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.replies {
  margin: 12px 0 0 40px;
  padding-left: 16px;
  border-left: 2px solid var(--line);
}

.reply-item {
  padding: 12px 0;
  border-top: 1px solid rgba(221, 211, 196, 0.7);
}

.reply-item:first-child {
  border-top: 0;
}

.reply-to {
  margin: 8px 0 0 36px;
  color: var(--muted);
  font-size: 10px;
}

.reply-text {
  margin-left: 36px;
  font-size: 12px;
}

.replies .comment-actions {
  margin-left: 36px;
}

.comments-empty {
  padding: 34px 0 10px;
  color: var(--muted);
  font-size: 12px;
}

.comments-loading {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 22px;
}

.comment-skeleton {
  height: 64px;
  border-radius: var(--radius-sm);
  background: var(--paper-deep);
}

@media (max-width: 560px) {
  .comments-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .comment-text,
  .comment-actions,
  .reply-form,
  .replies {
    margin-left: 0;
  }

  .replies {
    padding-left: 10px;
  }

  .reply-to,
  .reply-text,
  .replies .comment-actions {
    margin-left: 0;
  }
}
</style>
