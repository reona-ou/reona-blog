import http from './http'

export const getSiteSettings = () => http.get('/settings')
export const updateSiteSettings = data => http.put('/settings', data)
