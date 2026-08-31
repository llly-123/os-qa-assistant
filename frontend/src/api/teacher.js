import request from './request'

// 教师列表（超管端）
export function getTeacherList(params) {
  return request({
    url: '/admin/teachers',
    method: 'get',
    params
  })
}

// 审核教师：auditStatus 1=通过 2=拒绝
export function auditTeacher(id, auditStatus) {
  return request({
    url: `/admin/teachers/${id}/audit`,
    method: 'put',
    data: { auditStatus }
  })
}

// 禁用/启用教师：status 0=禁用 1=启用
export function toggleTeacherStatus(id, status) {
  return request({
    url: `/admin/teachers/${id}/status`,
    method: 'put',
    data: { status }
  })
}

// 重置教师密码
export function resetTeacherPassword(id) {
  return request({
    url: `/admin/teachers/${id}/reset-password`,
    method: 'post'
  })
}

// 删除教师
export function deleteTeacher(id) {
  return request({
    url: `/admin/teachers/${id}`,
    method: 'delete'
  })
}
