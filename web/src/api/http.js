import axios from 'axios'

const STORAGE_KEY = 'reona_user'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

function currentToken() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')?.token || ''
  } catch {
    return ''
  }
}

http.interceptors.request.use(config => {
  const token = currentToken()
  if (token) config.headers.token = token
  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === '200') return body.data
      if (body.code === '401') {
        localStorage.removeItem(STORAGE_KEY)
        if (!window.location.pathname.startsWith('/login')) {
          window.location.assign('/login')
        }
      }
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  error => {
    const body = error.response?.data
    const message = body?.msg || error.message || '网络异常，请稍后重试'
    if (body?.code === '401') {
      localStorage.removeItem(STORAGE_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    return Promise.reject(new Error(message))
  }
)

export { STORAGE_KEY }
export default http
