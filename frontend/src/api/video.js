import request from './request'

// 获取章节列表（公共）
export function getChapters() {
  return request({
    url: '/courses/chapters',
    method: 'get'
  })
}

// 教师端：章管理
export function addChapter(title) {
  return request({
    url: '/admin/chapters',
    method: 'post',
    data: { title }
  })
}

export function updateChapter(id, title) {
  return request({
    url: `/admin/chapters/${id}`,
    method: 'put',
    data: { title }
  })
}

export function deleteChapter(id) {
  return request({
    url: `/admin/chapters/${id}`,
    method: 'delete'
  })
}

export function moveChapter(id, direction) {
  return request({
    url: `/admin/chapters/${id}/move`,
    method: 'put',
    data: { direction }
  })
}

// 教师端：节管理
export function addSection(chapterId, title) {
  return request({
    url: `/admin/chapters/${chapterId}/sections`,
    method: 'post',
    data: { title }
  })
}

export function updateSection(id, title) {
  return request({
    url: `/admin/sections/${id}`,
    method: 'put',
    data: { title }
  })
}

export function deleteSection(id) {
  return request({
    url: `/admin/sections/${id}`,
    method: 'delete'
  })
}

export function moveSection(id, direction) {
  return request({
    url: `/admin/sections/${id}/move`,
    method: 'put',
    data: { direction }
  })
}

// 教师端：视频上传
export function uploadVideo(sectionId, file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/admin/sections/${sectionId}/upload-video`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function deleteVideo(sectionId) {
  return request({
    url: `/admin/sections/${sectionId}/video`,
    method: 'delete'
  })
}

// 学生端：视频进度
export function saveVideoProgress(sectionId, currentTime, completed) {
  return request({
    url: '/students/video-progress',
    method: 'post',
    data: { sectionId, currentTime, completed }
  })
}

export function getVideoProgress() {
  return request({
    url: '/students/video-progress',
    method: 'get'
  })
}
