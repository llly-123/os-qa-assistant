import request from './request'

export function getQAStatistics(params) {
  return request({
    url: '/admin/statistics/qa',
    method: 'get',
    params
  })
}

export function getHotKeywords(params) {
  return request({
    url: '/admin/statistics/keywords',
    method: 'get',
    params
  })
}

export function getQuestionTrend(params) {
  return request({
    url: '/admin/statistics/trend',
    method: 'get',
    params
  })
}

export function getRecentQuestions(params) {
  return request({
    url: '/admin/statistics/recent',
    method: 'get',
    params
  })
}

export function getUserQuestions(userId, limit = 20) {
  return request({
    url: `/admin/statistics/user/${userId}/questions`,
    method: 'get',
    params: { limit }
  })
}
