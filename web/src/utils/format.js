import { marked } from 'marked'
import DOMPurify from 'dompurify'

export function formatDate(value, withTime = false) {
  if (!value) return ''
  const normalized = String(value)
    .replace(' ', 'T')
    .replace(/(\.\d{3})\d+/, '$1')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, withTime ? 16 : 10).replace(/-/g, '.')
  const parts = new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {})
  }).formatToParts(date)
  const map = Object.fromEntries(parts.map(part => [part.type, part.value]))
  return `${map.year}.${map.month}.${map.day}${withTime ? ` ${map.hour}:${map.minute}` : ''}`
}

export function stripMarkdown(text, length = 110) {
  const plain = String(text || '')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/<[^>]+>/g, '')
    .replace(/[#>*_`~\-]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return plain.length > length ? `${plain.slice(0, length)}…` : plain
}

export function resolveAsset(url) {
  if (!url) return ''
  return String(url)
    .replace(/^https?:\/\/localhost:9090/i, '/api')
    .replace(/^https?:\/\/127\.0\.0\.1:9090/i, '/api')
}

export function storedAsset(url) {
  if (!url) return ''
  if (url.startsWith('http://localhost:9090') || url.startsWith('http://127.0.0.1:9090')) {
    return url.replace(/^https?:\/\/[^/]+/i, '/api')
  }
  if (url.startsWith('/file/')) return `/api${url}`
  return url
}

export function renderMarkdown(content) {
  const html = marked.parse(String(content || ''), { gfm: true, breaks: true })
  return DOMPurify.sanitize(html, {
    ADD_ATTR: ['target', 'rel']
  }).replace(/https?:\/\/(localhost|127\.0\.0\.1):9090/g, '/api')
}
