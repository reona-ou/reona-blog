<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import ja from 'element-plus/es/locale/lang/ja'
import { site } from '@/config/site'

const { t, locale } = useI18n()
const elementLocale = computed(() => ({ zh: zhCn, en, ja }[locale.value] || zhCn))

watch(
  [() => site.name, locale],
  () => {
    const suffix = t('site.titleSuffix')
    document.title = site.name ? `${site.name} · ${suffix}` : suffix
    document.documentElement.lang = locale.value === 'zh' ? 'zh-CN' : locale.value
    const meta = document.querySelector('meta[name="description"]')
    if (meta) {
      meta.setAttribute('content', t('site.description', { name: site.name }))
    }
  },
  { immediate: true }
)
</script>
