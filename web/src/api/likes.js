import http from './http'

export const getArticleLike = articleId => http.get(`/article-like/${articleId}`)
export const toggleArticleLike = articleId => http.post(`/article-like/${articleId}`)
export const toggleCommentLike = commentId => http.post(`/comment/${commentId}/like`)
