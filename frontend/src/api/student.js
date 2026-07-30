import request from './request'

export function getStudentList(params) {
  return request({
    url: '/admin/students',
    method: 'get',
    params
  })
}

// 获取所有学生（不分页，用于班级管理勾选）
export function getAllStudents() {
  return request({
    url: '/admin/students/all',
    method: 'get'
  })
}

export function batchImportStudents(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/students/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function createStudent(data) {
  return request({
    url: '/admin/students',
    method: 'post',
    data
  })
}

export function updateStudent(id, data) {
  return request({
    url: `/admin/students/${id}`,
    method: 'put',
    data
  })
}

export function deleteStudent(id) {
  return request({
    url: `/admin/students/${id}`,
    method: 'delete'
  })
}

export function resetStudentPassword(id) {
  return request({
    url: `/admin/students/${id}/reset-password`,
    method: 'post'
  })
}

export function toggleStudentStatus(id, status) {
  return request({
    url: `/admin/students/${id}/status`,
    method: 'put',
    data: { status }
  })
}
