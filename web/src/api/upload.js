import http from './http'

export async function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/file/upload', form, { timeout: 60000 })
}
