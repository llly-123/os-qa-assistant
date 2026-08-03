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

export function getUserQuestions(userId, limit = 20, classId) {
  return request({
    url: `/admin/statistics/user/${userId}/questions`,
    method: 'get',
    params: { limit, classId }
  })
}

export function getClassList() {
  return request({
    url: '/admin/statistics/classes',
    method: 'get'
  })
}

export function getClassOverview(classId, params) {
  return request({
    url: `/admin/statistics/classes/${classId}/overview`,
    method: 'get',
    params
  })
}

export function getClassHotKeywords(classId, params) {
  return request({
    url: `/admin/statistics/classes/${classId}/keywords`,
    method: 'get',
    params
  })
}

export function getClassQuestionTrend(classId, params) {
  return request({
    url: `/admin/statistics/classes/${classId}/trend`,
    method: 'get',
    params
  })
}

export function getSessionRounds(params) {
  return request({
    url: '/admin/statistics/sessions',
    method: 'get',
    params
  })
}

export function getClassSessionRounds(classId, params) {
  return request({
    url: `/admin/statistics/classes/${classId}/sessions`,
    method: 'get',
    params
  })
}

export function getSourceDistribution(params) {
  return request({
    url: '/admin/statistics/sources',
    method: 'get',
    params
  })
}

export function getClassSourceDistribution(classId, params) {
  return request({
    url: `/admin/statistics/classes/${classId}/sources`,
    method: 'get',
    params
  })
}

export function getActiveDaysStats(params) {
  return request({
    url: '/admin/statistics/active-days',
    method: 'get',
    params
  })
}

export function getClassActiveDaysStats(classId, params) {
  return request({
    url: `/admin/statistics/classes/${classId}/active-days`,
    method: 'get',
    params
  })
}
