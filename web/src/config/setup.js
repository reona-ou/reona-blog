import { reactive } from 'vue'
import { getSetupStatus } from '@/api/setup'

export const setupState = reactive({
  checked: false,
  required: false,
  siteName: 'Default Blog',
  email: ''
})

let pending = null

export async function loadSetupStatus(force = false) {
  if (setupState.checked && !force) {
    return setupState
  }
  if (pending) {
    return pending
  }
  pending = (async () => {
    try {
      const data = await getSetupStatus()
      setupState.required = Boolean(data?.required)
      setupState.siteName = data?.siteName || 'Default Blog'
      setupState.email = data?.email || ''
    } catch {
      setupState.required = false
    } finally {
      setupState.checked = true
      pending = null
    }
    return setupState
  })()
  return pending
}
