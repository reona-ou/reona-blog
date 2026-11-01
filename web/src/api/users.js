import http from './http'

export const listManagedUsers = params => http.get('/user/manage/page', { params })
export const updateManagedUser = (id, data) => http.put(`/user/manage/${id}`, data)
export const updateManagedUserRole = (id, role) => http.put(`/user/manage/${id}/role`, { role })
export const deleteManagedUser = id => http.delete(`/user/manage/${id}`)
export const resetManagedUserPassword = (id, newPassword) => http.put(`/user/manage/${id}/password`, { newPassword })
export const getUserById = id => http.get(`/user/${id}`)
export const updateMyProfile = data => http.put('/user/profile', data)
