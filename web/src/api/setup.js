import http from './http'

export const getSetupStatus = () => http.get('/setup/status')
export const initializeSetup = data => http.post('/setup/initialize', data)
