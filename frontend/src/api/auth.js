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

export function bindPhone(phone) {
  return request({
    url: '/auth/bind-phone',
    method: 'post',
    data: { phone }
  })
}

export function unbindPhone() {
  return request({
    url: '/auth/unbind-phone',
    method: 'post'
  })
}

export function sendPhoneCode(phone) {
  return request({
    url: '/auth/send-phone-code',
    method: 'post',
    data: { phone }
  })
}

export function resetPasswordByPhone(phone, code) {
  return request({
    url: '/auth/reset-password-by-phone',
    method: 'post',
    data: { phone, code }
  })
}
