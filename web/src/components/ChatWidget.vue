<template>
  <div v-if="assistant.enabled" class="chat-widget">
    <transition name="chat-pop">
      <section v-if="open" class="chat-panel">
        <header class="chat-header">
          <div class="chat-identity">
            <span class="chat-spark">✦</span>
            <div>
              <b>{{ displayName }}</b>
              <small>{{ t('chat.subtitle') }}</small>
            </div>
          </div>
          <div class="chat-header-actions">
            <button
              class="chat-clear"
              type="button"
              :title="t('chat.clearHint')"
              :aria-label="t('chat.clearHint')"
              :disabled="!canClear"
              @click="clearContext"
            >
              <el-icon><Delete /></el-icon>
              <span>{{ t('chat.clear') }}</span>
            </button>
            <button class="chat-close" type="button" :aria-label="t('chat.close')" @click="open = false">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </header>

        <div ref="listEl" class="chat-messages">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="chat-message"
            :class="[message.role, { 'is-waiting': message.streaming && !message.text }]"
          >
            <span class="chat-role">{{ message.role === 'user' ? t('chat.you') : t('chat.assistant') }}</span>
            <p>{{ message.text }}</p>
          </div>
          <div v-if="typing" class="chat-message assistant">
            <span class="chat-role">{{ t('chat.assistant') }}</span>
            <p class="typing"><i></i><i></i><i></i></p>
          </div>
        </div>

        <div class="chat-quick">
          <button v-for="prompt in prompts" :key="prompt" type="button" :disabled="typing" @click="ask(prompt)">
            {{ prompt }}
          </button>
        </div>

        <form class="chat-compose" @submit.prevent="send">
          <input
            ref="inputEl"
            v-model="input"
            type="text"
            :placeholder="t('chat.placeholder')"
            maxlength="500"
            :disabled="typing"
          />
          <button type="submit" :disabled="!input.trim() || typing" :aria-label="t('chat.send')">
            <el-icon><Promotion /></el-icon>
          </button>
        </form>
      </section>
    </transition>

    <button class="chat-fab" type="button" @click="toggle">
      <span class="chat-fab-icon">✦</span>
      <span>{{ displayName }}</span>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { sendChatStream } from '@/api/chat'
import { assistant } from '@/config/assistant'

const { t, locale } = useI18n()
const open = ref(false)
const input = ref('')
const typing = ref(false)
const replyLanguage = ref(locale.value)
const displayName = computed(() => assistant.name || t('chat.title'))

function welcomeText() {
  return t('chat.welcome', { name: displayName.value })
}

const messages = ref([
  {
    role: 'assistant',
    welcome: true,
    text: welcomeText()
  }
])
const prompts = computed(() => [
  t('chat.promptBlog'),
  t('chat.promptComments'),
  t('chat.promptLikes'),
  t('chat.promptContact')
])
const canClear = computed(() => typing.value || messages.value.some(message => !message.welcome))
const listEl = ref(null)
const inputEl = ref(null)
let requestSeq = 0
let streamAbort = null

watch(locale, () => {
  requestSeq += 1
  streamAbort?.abort()
  streamAbort = null
  typing.value = false
  input.value = ''
  replyLanguage.value = locale.value
  messages.value = [{ role: 'assistant', welcome: true, text: welcomeText() }]
  scrollToBottom()
})

watch(() => assistant.name, () => {
  const welcome = messages.value.find(message => message.welcome)
  if (welcome) welcome.text = welcomeText()
})

function scrollToBottom() {
  nextTick(() => {
    if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
  })
}

function toggle() {
  open.value = !open.value
  if (open.value) {
    nextTick(() => inputEl.value?.focus())
    scrollToBottom()
  }
}

function send() {
  const text = input.value.trim()
  if (!text || typing.value) return
  input.value = ''
  ask(text)
}

function clearContext() {
  if (!canClear.value) return
  requestSeq += 1
  streamAbort?.abort()
  streamAbort = null
  typing.value = false
  input.value = ''
  messages.value = [{ role: 'assistant', welcome: true, text: welcomeText() }]
  scrollToBottom()
  ElMessage.success(t('chat.cleared'))
}

async function ask(text) {
  if (typing.value) return
  const value = String(text || '').trim()
  if (!value) return

  const seq = ++requestSeq
  messages.value.push({ role: 'user', text: value })
  const assistantIndex = messages.value.length
  messages.value.push({ role: 'assistant', text: '', streaming: true })
  typing.value = true
  scrollToBottom()

  const controller = new AbortController()
  streamAbort = controller
  const timeout = window.setTimeout(() => controller.abort(), 150000)

  try {
    const history = messages.value
      .filter(message => !message.welcome && !message.streaming && message.text
        && (message.role === 'user' || message.role === 'assistant'))
      .slice(-8)
      .map(message => ({ role: message.role, content: message.text }))
    const reply = await sendChatStream(history, replyLanguage.value, {
      signal: controller.signal,
      onLanguage: lang => {
        if (lang && seq === requestSeq) replyLanguage.value = lang
      },
      onDelta: (_, full) => {
        if (seq !== requestSeq) return
        const message = messages.value[assistantIndex]
        if (message) {
          message.text = cleanReply(full)
        }
        scrollToBottom()
      }
    })
    if (seq !== requestSeq) return
    const message = messages.value[assistantIndex]
    if (message) {
      message.text = cleanReply(reply) || t('chat.error')
      message.streaming = false
    }
  } catch (error) {
    if (seq !== requestSeq) return
    const message = messages.value[assistantIndex]
    if (message) {
      const raw = error.name === 'AbortError' ? '' : error.message || ''
      if (!message.text) {
        message.text = /问得有点快|正在想别的|太多/.test(raw)
          ? t('chat.busy')
          : t('chat.offline')
      }
      message.streaming = false
    }
  } finally {
    window.clearTimeout(timeout)
    if (streamAbort === controller) {
      streamAbort = null
    }
    if (seq === requestSeq) {
      typing.value = false
      scrollToBottom()
    }
  }
}

function cleanReply(text) {
  return String(text || '')
    .replace(/<think>[\s\S]*?<\/think>/gi, '')
    .replace(/<think>[\s\S]*$/i, '')
    .replace(/<\/?think>/gi, '')
    .trim()
}

function onKeydown(event) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  streamAbort?.abort()
})
</script>

<style scoped>
.chat-widget {
  position: fixed;
  right: 26px;
  bottom: 26px;
  z-index: 120;
}

.chat-fab {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 48px;
  padding: 0 20px;
  border: 1px solid rgba(244, 239, 230, 0.25);
  border-radius: 999px;
  background: var(--ink);
  color: var(--paper);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  box-shadow: 0 12px 30px rgba(35, 32, 28, 0.22);
  transition: transform 0.2s ease, background 0.2s ease, opacity 0.2s ease;
}

.chat-fab:hover {
  transform: translateY(-2px);
  background: var(--accent);
}

.chat-fab-icon {
  font-size: 16px;
  color: var(--gold);
}

.chat-fab span:last-child {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  position: absolute;
  right: 0;
  bottom: 62px;
  width: 370px;
  max-height: min(620px, calc(100vh - 190px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
  box-shadow: 0 24px 70px rgba(35, 32, 28, 0.2);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
  background: var(--paper);
}

.chat-identity {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}

.chat-identity > div {
  min-width: 0;
}

.chat-header-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
}

.chat-clear {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.18s ease;
}

.chat-clear:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.chat-clear:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.chat-clear .el-icon {
  font-size: 12px;
}

.chat-spark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--green);
  color: var(--paper);
  font-size: 15px;
}

.chat-identity b {
  display: block;
  overflow: hidden;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-identity small {
  display: block;
  margin-top: 2px;
  overflow: hidden;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-close {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}

.chat-close:hover {
  background: rgba(35, 32, 28, 0.06);
  color: var(--ink);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-message {
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-width: 88%;
}

.chat-message.is-waiting {
  display: none;
}

.chat-message.user {
  align-self: flex-end;
  align-items: flex-end;
}

.chat-role {
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.chat-message p {
  margin: 0;
  padding: 10px 13px;
  border-radius: 2px 12px 12px 12px;
  background: var(--paper);
  color: var(--ink-soft);
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-message.user p {
  border-radius: 12px 2px 12px 12px;
  background: var(--green);
  color: #fff;
}

.chat-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  padding: 0 16px 12px;
}

.chat-quick button {
  padding: 6px 11px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.chat-quick button:hover:not(:disabled) {
  border-color: var(--ink);
  color: var(--ink);
}

.chat-quick button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chat-compose {
  display: flex;
  gap: 8px;
  padding: 12px 14px 14px;
  border-top: 1px solid var(--line);
  background: var(--paper);
}

.chat-compose input {
  flex: 1;
  min-width: 0;
  height: 40px;
  padding: 0 13px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink);
  font-size: 12px;
  outline: none;
}

.chat-compose input:focus {
  border-color: var(--green);
}

.chat-compose input:disabled {
  opacity: 0.6;
}

.chat-compose button {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: var(--ink);
  color: var(--paper);
  cursor: pointer;
}

.chat-compose button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  min-height: 34px;
}

.typing i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--muted);
  animation: blink 1s infinite ease-in-out;
}

.typing i:nth-child(2) {
  animation-delay: 0.15s;
}

.typing i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes blink {
  0%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.chat-pop-enter-active,
.chat-pop-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
  transform-origin: bottom right;
}

.chat-pop-enter-from,
.chat-pop-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.97);
}

@media (max-width: 560px) {
  .chat-widget {
    right: 14px;
    bottom: 14px;
  }

  .chat-fab {
    width: 48px;
    padding: 0;
    justify-content: center;
  }

  .chat-fab span:last-child {
    display: none;
  }

  .chat-panel {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 74px;
    width: auto;
    max-height: calc(100vh - 166px);
  }
}
</style>
