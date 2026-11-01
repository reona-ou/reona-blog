const CHAT_STREAM_URL = '/api/chat/stream'

export async function sendChatStream(messages, language, { signal, onDelta, onLanguage } = {}) {
  const response = await fetch(CHAT_STREAM_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream'
    },
    body: JSON.stringify({ messages, language }),
    signal,
    cache: 'no-store'
  })

  const contentType = response.headers.get('content-type') || ''
  if (!response.ok) {
    let message = '请求失败'
    if (contentType.includes('application/json')) {
      const body = await response.json().catch(() => null)
      message = body?.msg || message
    }
    throw new Error(message)
  }
  if (!contentType.includes('text/event-stream') || !response.body) {
    throw new Error('服务端未返回流式响应')
  }

  const replyLanguage = response.headers.get('X-Reply-Language')
  if (replyLanguage) onLanguage?.(replyLanguage)

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let full = ''
  let finished = false

  function handleBlock(block) {
    let event = 'message'
    const dataLines = []
    for (const rawLine of block.split('\n')) {
      const line = rawLine.replace(/\r$/, '')
      if (line.startsWith(':')) continue
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
        continue
      }
      if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^ /, ''))
      }
    }
    if (!dataLines.length) return
    const data = dataLines.join('\n')
    if (data === '[DONE]') {
      finished = true
      return
    }
    if (event === 'error') {
      let message = 'AI 助手暂时不可用，请稍后再试'
      try {
        message = JSON.parse(data)?.msg || message
      } catch {
        // 保持默认错误信息
      }
      throw new Error(message)
    }
    let payload
    try {
      payload = JSON.parse(data)
    } catch {
      return
    }
    if (payload?.error) {
      throw new Error(payload.error.message || 'AI 助手暂时不可用，请稍后再试')
    }
    const choice = payload?.choices?.[0]
    const delta = choice?.delta?.content ?? choice?.text ?? ''
    if (delta) {
      full += delta
      onDelta?.(delta, full)
    }
  }

  try {
    while (!finished) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let boundary
      while (!finished && (boundary = buffer.indexOf('\n\n')) >= 0) {
        const block = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        handleBlock(block)
      }
    }
    if (!finished) {
      buffer += decoder.decode()
      if (buffer.trim()) handleBlock(buffer)
    }
  } finally {
    try {
      await reader.cancel()
    } catch {
      // 已结束
    }
  }

  return full
}
