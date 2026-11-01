import http from './http'

export const listComments = articleId => http.get(`/comment/article/${articleId}`)
export const addComment = data => http.post('/comment', data)
export const deleteComment = id => http.delete(`/comment/${id}`)
export const getCommentCount = () => http.get('/comment/count')
