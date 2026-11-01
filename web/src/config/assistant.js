import { reactive } from 'vue'
import { getAssistantPublic } from '@/api/assistant'

export const assistant = reactive({
  name: 'Default Assistant',
  enabled: true
})

export function applyAssistant(data) {
  if (!data) return
  if (data.name) assistant.name = data.name
  if (typeof data.enabled === 'boolean') assistant.enabled = data.enabled
}

export async function loadAssistant() {
  try {
    applyAssistant(await getAssistantPublic())
  } catch {
    // 助手配置不可用时保留默认名称，不阻塞页面
  }
}
