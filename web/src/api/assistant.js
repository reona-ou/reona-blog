import http from './http'

export const getAssistantPublic = () => http.get('/assistant/public')
export const getAssistantSettings = () => http.get('/assistant/settings')
export const updateAssistantSettings = data => http.put('/assistant/settings', data)
