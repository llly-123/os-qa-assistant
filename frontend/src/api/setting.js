import request from './request'

// 公开接口（登录页等未登录场景）：站点/课程/学校名
export function getPublicSettings() {
  return request({ url: '/settings/public', method: 'get' })
}

// 教师端：获取全部系统设置
export function getSettings() {
  return request({ url: '/admin/settings', method: 'get' })
}

// 教师端：更新系统设置
export function updateSettings(data) {
  return request({ url: '/admin/settings', method: 'put', data })
}
