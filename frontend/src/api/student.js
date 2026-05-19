import request from './request'

export function getStudentList(params) {
  return request({
    url: '/admin/students',
    method: 'get',
    params
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
