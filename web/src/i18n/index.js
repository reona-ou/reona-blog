import { createI18n } from 'vue-i18n'
import zh from './zh'
import en from './en'
import ja from './ja'

export const supportedLocales = [
  { code: 'zh', label: '中文', short: '中' },
  { code: 'en', label: 'English', short: 'EN' },
  { code: 'ja', label: '日本語', short: '日' }
]

function initialLocale() {
  const stored = localStorage.getItem('site_language')
  if (supportedLocales.some(item => item.code === stored)) return stored
  return 'zh'
}

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'zh',
  messages: { zh, en, ja }
})
