import http from './http'

export const listArticles = () => http.get('/article')
export const getArticle = id => http.get(`/article/${id}`)
export const pageArticles = params => http.get('/article/page', { params })
export const getArticleStats = () => http.get('/article/stats')
export const saveArticle = data => http.post('/article', data)
export const deleteArticle = id => http.delete(`/article/${id}`)
