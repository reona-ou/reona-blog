import http from './http'

export const login = data => http.post('/user/login', data)
export const register = data => http.post('/user/register', data)
export const getUserProfile = username => http.get(`/user/personify/${encodeURIComponent(username)}`)
export const changePassword = data => http.post('/user/password', data)
