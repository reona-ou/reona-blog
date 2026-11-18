import http from './http'

export const getCalendarSources = () => http.get('/calendar/sources')

export const createCalendarSource = payload => http.post('/calendar/sources', payload, { timeout: 45000 })

export const updateCalendarSource = (id, payload) =>
  http.put(`/calendar/sources/${id}`, payload, { timeout: 45000 })

export const deleteCalendarSource = id => http.delete(`/calendar/sources/${id}`)

export const getCalendarFeed = refresh =>
  http.get('/calendar/feed', {
    params: refresh ? { refresh: true } : {},
    timeout: 90000
  })

export const getPublicCalendarFeed = refresh =>
  http.get('/calendar/public/feed', {
    params: refresh ? { refresh: true } : {},
    timeout: 90000
  })
