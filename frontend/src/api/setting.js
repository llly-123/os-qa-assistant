import request from './request'

// 教师端：获取全部系统设置
export function getSettings() {
  return request({ url: '/admin/settings', method: 'get' })
}

// 教师端：更新系统设置
export function updateSettings(data) {
  return request({ url: '/admin/settings', method: 'put', data })
}

// 管理员端：清空敏感密钥（如 ai_api_key / sms_access_key_secret）
export function clearSetting(key) {
  return request({ url: `/admin/settings/${key}`, method: 'delete' })
}

// 教师端：测试 AI 接口配置是否有效
export function testAi(data) {
  return request({ url: '/admin/settings/test-ai', method: 'post', data })
}

// ===== 教师级 API 配置 =====

// 获取当前教师的 API 配置及体验状态
export function getTeacherApiConfig(teacherId) {
  return request({ url: '/teacher/api-config', method: 'get', params: teacherId ? { teacherId } : {} })
}

// 保存当前教师的 API 配置
export function saveTeacherApiConfig(data) {
  return request({ url: '/teacher/api-config', method: 'put', data })
}

// 清除当前教师的 API 配置，回退到管理员默认
export function clearTeacherApiConfig() {
  return request({ url: '/teacher/api-config', method: 'delete' })
}

// 测试教师 API 配置是否有效（不落库）
export function testTeacherApiConfig(data) {
  return request({ url: '/teacher/api-config/test', method: 'post', data })
}
