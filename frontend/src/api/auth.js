import request from './request'

export function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: { username, password }
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

export function changePassword(oldPassword, newPassword) {
  return request({
    url: '/auth/password',
    method: 'put',
    data: { oldPassword, newPassword }
  })
}

export function resetPassword(email) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data: { email }
  })
}

export function sendVerifyCode(email) {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data: { email }
  })
}
