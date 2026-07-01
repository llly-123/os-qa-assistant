import request from './request'

// 教师端：获取班级列表
export function getClasses() {
  return request({ url: '/admin/classes', method: 'get' })
}

// 教师端：创建班级
export function createClass(name, startTime, endTime) {
  return request({ url: '/admin/classes', method: 'post', data: { name, startTime, endTime } })
}

// 教师端：删除班级
export function deleteClass(classId) {
  return request({ url: `/admin/classes/${classId}`, method: 'delete' })
}

// 教师端：解散班级
export function dissolveClass(classId) {
  return request({ url: `/admin/classes/${classId}/dissolve`, method: 'post' })
}

// 教师端：获取班级学生
export function getClassStudents(classId) {
  return request({ url: `/admin/classes/${classId}/students`, method: 'get' })
}

// 教师端：添加单个学生
export function addStudent(classId, studentId) {
  return request({ url: `/admin/classes/${classId}/students`, method: 'post', data: { studentId } })
}

// 教师端：批量添加学生（按学号）
export function addStudentsByUsernames(classId, usernames) {
  return request({ url: `/admin/classes/${classId}/students/batch`, method: 'post', data: { usernames } })
}

// 教师端：移除学生
export function removeStudent(classId, studentId) {
  return request({ url: `/admin/classes/${classId}/students/${studentId}`, method: 'delete' })
}

// 教师端：在班级内创建学生（创建用户+加入班级）
export function createStudentInClass(classId, data) {
  return request({ url: `/admin/classes/${classId}/students/create`, method: 'post', data })
}

// 教师端：批量导入学生到班级
export function importStudentsInClass(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/admin/classes/${classId}/students/import`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 学生端：查询自己的班级
export function getMyClass() {
  return request({ url: '/students/my-class', method: 'get' })
}
