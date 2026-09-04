import request from './request'

export function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: { username, password }
  })
}

// 教师自助注册（工号 + 密码 + 姓名），注册后待超管审核
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
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

export function changePhone(code, newPhone) {
  return request({
    url: '/auth/change-phone',
    method: 'post',
    data: { code, newPhone }
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

export function bindEmail(email) {
  return request({
    url: '/auth/bind-email',
    method: 'post',
    data: { email }
  })
}

export function unbindEmail() {
  return request({
    url: '/auth/unbind-email',
    method: 'post'
  })
}

export function sendEmailCode(email) {
  return request({
    url: '/auth/send-email-code',
    method: 'post',
    data: { email }
  })
}

export function resetPasswordByEmail(email, code) {
  return request({
    url: '/auth/reset-password-by-email',
    method: 'post',
    data: { email, code }
  })
}
