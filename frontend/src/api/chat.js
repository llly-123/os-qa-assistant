import request from './request'

export function getChatSessions(classId) {
  return request({
    url: '/chat/sessions',
    method: 'get',
    params: classId != null ? { classId } : {}
  })
}

export function getChatMessages(sessionId) {
  return request({
    url: `/chat/sessions/${sessionId}/messages`,
    method: 'get'
  })
}

export function createChatSession(title, classId) {
  return request({
    url: '/chat/sessions',
    method: 'post',
    data: { title, classId }
  })
}

export function deleteChatSession(sessionId) {
  return request({
    url: `/chat/sessions/${sessionId}`,
    method: 'delete'
  })
}

export function updateChatSessionTitle(sessionId, title) {
  return request({
    url: `/chat/sessions/${sessionId}`,
    method: 'put',
    data: { title }
  })
}

export function getMyStats(classId) {
  return request({
    url: '/chat/my-stats',
    method: 'get',
    params: classId != null ? { classId } : {}
  })
}

export function getMyTrend(params) {
  return request({
    url: '/chat/my-trend',
    method: 'get',
    params
  })
}

export function getMySessionRounds(params) {
  return request({
    url: '/chat/my-sessions',
    method: 'get',
    params
  })
}

export function getMySourceDistribution(params) {
  return request({
    url: '/chat/my-sources',
    method: 'get',
    params
  })
}

export function getMyActiveDays(params) {
  return request({
    url: '/chat/my-active-days',
    method: 'get',
    params
  })
}

export function getMyHours(params) {
  return request({
    url: '/chat/my-hours',
    method: 'get',
    params
  })
}

// ===== 学习时长统计 =====

export function getMyStudyTime(params) {
  return request({
    url: '/chat/my-study-time',
    method: 'get',
    params
  })
}
