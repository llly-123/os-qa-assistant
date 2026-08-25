import request from './request'

// 教师端：获取全部系统设置
export function getSettings() {
  return request({ url: '/admin/settings', method: 'get' })
}

// 教师端：更新系统设置
export function updateSettings(data) {
  return request({ url: '/admin/settings', method: 'put', data })
}

// 教师端：测试 AI 接口配置是否有效
export function testAi(data) {
  return request({ url: '/admin/settings/test-ai', method: 'post', data })
}
