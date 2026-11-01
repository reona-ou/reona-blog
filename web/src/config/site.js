import { reactive } from 'vue'
import { getSiteSettings } from '@/api/settings'

const defaults = {
  name: 'Default Blog',
  mark: 'D',
  email: 'default@example.com',
  since: '2026'
}

export const site = reactive({ ...defaults })

function deriveMark(name) {
  const value = String(name || '').trim()
  return value ? value.slice(0, 1).toUpperCase() : 'D'
}

export function applySiteSettings(data) {
  if (!data) return
  if (data.siteName) site.name = data.siteName
  if (data.email) site.email = data.email
  site.mark = deriveMark(site.name)
}

export async function loadSiteSettings() {
  try {
    applySiteSettings(await getSiteSettings())
  } catch {
    // 后端不可用时保留默认站点信息，不阻塞页面
  }
}
