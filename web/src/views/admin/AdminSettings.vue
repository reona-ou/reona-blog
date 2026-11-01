<template>
  <div>
    <div class="admin-page-head">
      <div>
        <h2>{{ t('admin.settings.title') }}</h2>
        <p>{{ t('admin.settings.subtitle') }}</p>
      </div>
      <span class="badge">SITE SETTINGS</span>
    </div>

    <div class="admin-settings-grid">
      <div class="admin-panel">
        <div class="admin-form-grid">
          <label class="admin-form-field span-2">
            <span>{{ t('admin.settings.siteName') }}</span>
            <el-input
              v-model="form.siteName"
              maxlength="32"
              show-word-limit
              :placeholder="t('admin.settings.siteNamePlaceholder')"
            />
          </label>
          <label class="admin-form-field span-2">
            <span>{{ t('admin.settings.email') }}</span>
            <el-input v-model="form.email" maxlength="128" placeholder="default@example.com" />
          </label>
        </div>

        <div class="admin-settings-actions">
          <el-button type="primary" :loading="saving" @click="save">
            <el-icon><Check /></el-icon>
            {{ t('admin.settings.save') }}
          </el-button>
          <el-button :disabled="saving" @click="load">{{ t('admin.settings.restore') }}</el-button>
        </div>
      </div>

      <div class="admin-panel admin-preview">
        <p class="eyebrow">{{ t('admin.settings.preview') }}</p>
        <div class="admin-preview-brand">
          <span>{{ previewMark }}</span>
          <b>{{ form.siteName || t('admin.settings.siteNameFallback') }}</b>
        </div>
        <p class="admin-preview-email">{{ form.email || t('admin.settings.emailFallback') }}</p>
        <p class="admin-preview-note">{{ t('admin.settings.previewNote') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getSiteSettings, updateSiteSettings } from '@/api/settings'
import { applySiteSettings, site } from '@/config/site'

const { t } = useI18n()
const saving = ref(false)
const form = reactive({
  siteName: site.name,
  email: site.email
})

const previewMark = computed(() => {
  const value = String(form.siteName || '').trim()
  return value ? value.slice(0, 1).toUpperCase() : 'R'
})

async function load() {
  try {
    const data = await getSiteSettings()
    if (data) {
      form.siteName = data.siteName || site.name
      form.email = data.email || site.email
      applySiteSettings(data)
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function save() {
  const siteName = String(form.siteName || '').trim()
  const email = String(form.email || '').trim()
  if (!siteName) {
    ElMessage.warning(t('admin.settings.nameRequired'))
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning(t('admin.settings.emailInvalid'))
    return
  }

  saving.value = true
  try {
    const data = await updateSiteSettings({ siteName, email })
    applySiteSettings(data)
    ElMessage.success(t('admin.settings.saved'))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.admin-form-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #6f6558;
  font-size: 12px;
}

.admin-settings-actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #eee7dd;
}

.admin-preview {
  background: #23201c;
  color: #f4efe6;
}

.admin-preview .eyebrow {
  color: rgba(244, 239, 230, 0.4);
}

.admin-preview-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}

.admin-preview-brand span {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #f4efe6;
  color: #23201c;
  font-family: Georgia, serif;
  font-size: 22px;
}

.admin-preview-brand b {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 500;
}

.admin-preview-email {
  margin: 10px 0 0;
  color: rgba(244, 239, 230, 0.55);
  font-size: 12px;
}

.admin-preview-note {
  margin: 26px 0 0;
  color: rgba(244, 239, 230, 0.35);
  font-size: 11px;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .admin-settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
