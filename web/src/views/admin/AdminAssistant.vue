<template>
  <div>
    <div class="admin-page-head">
      <div>
        <h2>{{ t('admin.assistant.title') }}</h2>
        <p>{{ t('admin.assistant.subtitle') }}</p>
      </div>
      <div class="assistant-head-actions">
        <el-tag :type="form.enabled ? 'success' : 'info'" effect="plain">
          {{ form.enabled ? t('admin.assistant.statusOn') : t('admin.assistant.statusOff') }}
        </el-tag>
        <el-button type="primary" :loading="saving" @click="save">
          <el-icon><Check /></el-icon>
          {{ t('admin.assistant.save') }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="assistant-layout">
      <div class="assistant-main">
        <div class="admin-panel">
          <div class="assistant-panel-head">
            <div>
              <h3>{{ t('admin.assistant.basicTitle') }}</h3>
              <p class="assistant-hint">{{ t('admin.assistant.enabledHint') }}</p>
            </div>
            <div class="assistant-enable">
              <span>{{ t('admin.assistant.enabled') }}</span>
              <el-switch v-model="form.enabled" />
            </div>
          </div>

          <div class="admin-form-grid assistant-form">
            <label class="admin-form-field">
              <span>{{ t('admin.assistant.name') }}</span>
              <el-input v-model="form.name" maxlength="32" show-word-limit :placeholder="t('admin.assistant.namePlaceholder')" />
            </label>
            <label class="admin-form-field">
              <span>{{ t('admin.assistant.model') }}</span>
              <el-input v-model="form.model" maxlength="128" :placeholder="t('admin.assistant.modelPlaceholder')" />
            </label>
            <label class="admin-form-field span-2">
              <span>{{ t('admin.assistant.apiUrl') }}</span>
              <el-input v-model="form.apiUrl" maxlength="255" :placeholder="t('admin.assistant.apiUrlPlaceholder')" />
              <small class="assistant-field-hint">{{ t('admin.assistant.apiUrlHint') }}</small>
            </label>
            <label class="admin-form-field span-2">
              <span class="assistant-field-title">
                {{ t('admin.assistant.apiKey') }}
                <el-tag size="small" :type="hasApiKey ? 'success' : 'info'" effect="plain">
                  {{ hasApiKey ? t('admin.assistant.apiKeyStored') : t('admin.assistant.apiKeyNotSet') }}
                </el-tag>
              </span>
              <el-input
                v-model="form.apiKey"
                type="password"
                show-password
                maxlength="255"
                :disabled="form.clearApiKey"
                :placeholder="hasApiKey ? t('admin.assistant.apiKeySavedPlaceholder') : t('admin.assistant.apiKeyEmptyPlaceholder')"
              />
              <small class="assistant-field-hint">{{ t('admin.assistant.apiKeyHint') }}</small>
              <el-checkbox v-model="form.clearApiKey" class="assistant-clear-key">
                {{ t('admin.assistant.clearApiKey') }}
              </el-checkbox>
            </label>
            <label class="admin-form-field">
              <span>{{ t('admin.assistant.temperature') }}</span>
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="2" />
              <small class="assistant-field-hint">{{ t('admin.assistant.temperatureHint') }}</small>
            </label>
            <label class="admin-form-field">
              <span>{{ t('admin.assistant.maxTokens') }}</span>
              <el-input-number v-model="form.maxTokens" :min="32" :max="4096" :step="16" />
              <small class="assistant-field-hint">{{ t('admin.assistant.maxTokensHint') }}</small>
            </label>
          </div>
        </div>

        <div class="admin-panel">
          <div class="assistant-panel-head">
            <div>
              <h3>{{ t('admin.assistant.languageTitle') }}</h3>
              <p class="assistant-hint">{{ t('admin.assistant.languageHint') }}</p>
            </div>
          </div>
          <el-alert
            v-if="toneDivergent"
            :title="t('admin.assistant.toneDivergent')"
            type="warning"
            :closable="false"
            show-icon
            class="assistant-tone-alert"
          />
          <div class="assistant-language-form">
            <label class="admin-form-field">
              <span>{{ t('admin.assistant.voiceLabel') }}</span>
              <el-input
                v-model="form.voice"
                type="textarea"
                :rows="4"
                :placeholder="t('admin.assistant.voicePlaceholder')"
              />
              <small class="assistant-field-hint">{{ t('admin.assistant.voiceHint') }}</small>
            </label>
            <label class="admin-form-field">
              <span>{{ t('admin.assistant.exampleLabel') }}</span>
              <el-input
                v-model="form.example"
                type="textarea"
                :rows="3"
                :placeholder="t('admin.assistant.examplePlaceholder')"
              />
              <small class="assistant-field-hint">{{ t('admin.assistant.exampleHint') }}</small>
            </label>
            <div>
              <el-button text @click="resetTone">
                {{ t('admin.assistant.resetTone') }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="admin-panel">
          <div class="assistant-panel-head">
            <div>
              <h3>{{ t('admin.assistant.promptTitle') }}</h3>
              <p class="assistant-hint">{{ t('admin.assistant.promptHint') }}</p>
            </div>
            <div class="assistant-prompt-actions">
              <el-tag size="small" effect="plain" :type="usingDefaultPrompt ? 'info' : 'success'">
                {{ usingDefaultPrompt ? t('admin.assistant.promptDefault') : t('admin.assistant.promptCustom') }}
              </el-tag>
              <el-button text @click="restoreDefaultPrompt">{{ t('admin.assistant.restoreDefault') }}</el-button>
            </div>
          </div>
          <el-input
            v-model="form.personaPrompt"
            type="textarea"
            :rows="18"
            class="assistant-prompt"
            :placeholder="t('admin.assistant.promptPlaceholder')"
            @input="usingDefaultPrompt = false"
          />
          <div class="assistant-save-row">
            <el-button type="primary" :loading="saving" @click="save">
              <el-icon><Check /></el-icon>
              {{ t('admin.assistant.save') }}
            </el-button>
          </div>
        </div>
      </div>

      <aside class="assistant-guide">
        <div class="admin-panel assistant-guide-panel">
          <h3>{{ t('admin.assistant.guideTitle') }}</h3>
          <p>{{ t('admin.assistant.guideIntro') }}</p>
          <div class="assistant-guide-steps">
            <p v-for="step in guideSteps" :key="step">{{ step }}</p>
          </div>

          <h4>{{ t('admin.assistant.placeholdersTitle') }}</h4>
          <ul class="assistant-placeholders">
            <li v-for="item in placeholders" :key="item.token">
              <code>{{ item.token }}</code>
              <span>{{ item.desc }}</span>
            </li>
          </ul>
          <p class="assistant-language-hint">{{ t('admin.assistant.languageRuleHint') }}</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getAssistantSettings, updateAssistantSettings } from '@/api/assistant'
import { applyAssistant } from '@/config/assistant'

const { t } = useI18n()
const saving = ref(false)
const loading = ref(false)
const hasApiKey = ref(false)
const usingDefaultPrompt = ref(true)
const defaultPrompt = ref('')
const toneDivergent = ref(false)

const form = reactive({
  name: '',
  apiUrl: '',
  apiKey: '',
  clearApiKey: false,
  model: '',
  temperature: 0.6,
  maxTokens: 420,
  enabled: true,
  personaPrompt: '',
  voice: '',
  example: ''
})

const guideSteps = computed(() => [
  t('admin.assistant.guideStep1'),
  t('admin.assistant.guideStep2'),
  t('admin.assistant.guideStep3'),
  t('admin.assistant.guideStep4'),
  t('admin.assistant.guideStep5'),
  t('admin.assistant.guideStep6')
])

const placeholders = computed(() => [
  { token: '{{assistantName}}', desc: t('admin.assistant.phName') },
  { token: '{{siteName}}', desc: t('admin.assistant.phSite') },
  { token: '{{contact}}', desc: t('admin.assistant.phContact') },
  { token: '{{languageName}}', desc: t('admin.assistant.phLanguageName') },
  { token: '{{languageVoice}}', desc: t('admin.assistant.phLanguageVoice') },
  { token: '{{languageExample}}', desc: t('admin.assistant.phLanguageExample') }
])

watch(() => form.clearApiKey, value => {
  if (value) form.apiKey = ''
})

function unifiedTone(...values) {
  return values.map(value => (value || '').trim()).find(Boolean) || ''
}

function hasDivergentTones(...values) {
  const filled = values.map(value => (value || '').trim()).filter(Boolean)
  return new Set(filled).size > 1
}

function applyData(data) {
  if (!data) return
  form.name = data.name || ''
  form.apiUrl = data.apiUrl || ''
  form.model = data.model || ''
  form.temperature = data.temperature ?? 0.6
  form.maxTokens = data.maxTokens ?? 420
  form.enabled = data.enabled !== false
  form.apiKey = ''
  form.clearApiKey = false
  hasApiKey.value = Boolean(data.hasApiKey)
  usingDefaultPrompt.value = data.usingDefaultPrompt !== false
  defaultPrompt.value = data.defaultPersonaPrompt || ''
  form.personaPrompt = data.personaPrompt || defaultPrompt.value
  form.voice = unifiedTone(data.voiceZh, data.voiceEn, data.voiceJa)
  form.example = unifiedTone(data.exampleZh, data.exampleEn, data.exampleJa)
  toneDivergent.value =
    hasDivergentTones(data.voiceZh, data.voiceEn, data.voiceJa)
    || hasDivergentTones(data.exampleZh, data.exampleEn, data.exampleJa)
}

function resetTone() {
  form.voice = ''
  form.example = ''
  toneDivergent.value = false
}

async function load() {
  loading.value = true
  try {
    applyData(await getAssistantSettings())
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function restoreDefaultPrompt() {
  form.personaPrompt = defaultPrompt.value
  usingDefaultPrompt.value = true
}

async function save() {
  const name = form.name.trim()
  const apiUrl = form.apiUrl.trim()
  const model = form.model.trim()
  if (!name) {
    ElMessage.warning(t('admin.assistant.validationName'))
    return
  }
  if (!/^https?:\/\/\S+$/.test(apiUrl)) {
    ElMessage.warning(t('admin.assistant.validationUrl'))
    return
  }
  if (!model) {
    ElMessage.warning(t('admin.assistant.validationModel'))
    return
  }
  if (form.temperature < 0 || form.temperature > 2) {
    ElMessage.warning(t('admin.assistant.validationTemp'))
    return
  }
  if (form.maxTokens < 32 || form.maxTokens > 4096) {
    ElMessage.warning(t('admin.assistant.validationTokens'))
    return
  }

  saving.value = true
  try {
    const useDefaultPrompt = usingDefaultPrompt.value && form.personaPrompt === defaultPrompt.value
    const data = await updateAssistantSettings({
      name,
      apiUrl,
      apiKey: form.apiKey,
      clearApiKey: form.clearApiKey,
      model,
      temperature: form.temperature,
      maxTokens: form.maxTokens,
      enabled: form.enabled,
      personaPrompt: useDefaultPrompt ? '' : form.personaPrompt,
      voiceZh: form.voice,
      voiceEn: form.voice,
      voiceJa: form.voice,
      exampleZh: form.example,
      exampleEn: form.example,
      exampleJa: form.example
    })
    applyData(data)
    applyAssistant({ name: data.name, enabled: data.enabled })
    ElMessage.success(t('admin.assistant.saved'))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.assistant-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

.assistant-head-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.assistant-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.assistant-guide {
  position: sticky;
  top: 20px;
}

.assistant-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
}

.assistant-panel-head h3,
.assistant-guide-panel h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 500;
}

.admin-form-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #6f6558;
  font-size: 12px;
}

.assistant-hint,
.assistant-field-hint,
.assistant-language-hint {
  color: #8d8274;
  font-size: 12px;
  line-height: 1.7;
}

.assistant-hint {
  margin: 6px 0 0;
}

.assistant-field-hint {
  display: block;
}

.assistant-field-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.assistant-enable {
  display: flex;
  align-items: center;
  gap: 9px;
  flex: 0 0 auto;
  color: #6f6558;
  font-size: 12px;
}

.assistant-form {
  margin-top: 4px;
}

.assistant-tone-alert {
  margin-bottom: 14px;
}

.assistant-language-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.assistant-language-form :deep(.el-textarea__inner) {
  font-size: 12px;
  line-height: 1.8;
  background: #fbf8f3;
}

.assistant-clear-key {
  margin-top: 2px;
}

.assistant-prompt-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.assistant-prompt :deep(.el-textarea__inner) {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.8;
  color: #2b2722;
  background: #fbf8f3;
}

.assistant-save-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.assistant-guide-panel {
  background: #fbf8f3;
}

.assistant-guide-panel > p {
  margin: 8px 0 14px;
  color: #7d7264;
  font-size: 12px;
  line-height: 1.8;
}

.assistant-guide-steps {
  color: #6f6558;
  font-size: 12px;
  line-height: 1.8;
}

.assistant-guide-steps p {
  margin: 0;
}

.assistant-guide-steps p + p {
  margin-top: 6px;
}

.assistant-guide-panel h4 {
  margin: 22px 0 10px;
  font-size: 13px;
  color: #3f3a33;
}

.assistant-placeholders {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.assistant-placeholders li {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  color: #7d7264;
  font-size: 11px;
}

.assistant-placeholders code {
  padding: 2px 6px;
  border-radius: 4px;
  background: #eee7dd;
  color: #2f4a3a;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
}

.assistant-language-hint {
  margin: 16px 0 0;
  padding-top: 14px;
  border-top: 1px solid #eee7dd;
}

@media (max-width: 1100px) {
  .assistant-layout {
    grid-template-columns: 1fr;
  }

  .assistant-guide {
    position: static;
  }
}

@media (max-width: 980px) {
  .assistant-panel-head {
    flex-direction: column;
  }

  .assistant-prompt-actions {
    flex-wrap: wrap;
  }
}
</style>
