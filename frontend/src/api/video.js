import request from './request'

// ========== 视频集（套）管理 ==========
export function getVideoSets() {
  return request({ url: '/admin/video-sets', method: 'get' })
}

export function createVideoSet(data) {
  return request({ url: '/admin/video-sets', method: 'post', data })
}

export function updateVideoSet(id, data) {
  return request({ url: `/admin/video-sets/${id}`, method: 'put', data })
}

export function deleteVideoSet(id) {
  return request({ url: `/admin/video-sets/${id}`, method: 'delete' })
}

// 获取某视频集下的章节（教师端）
export function getVideoSetChapters(setId) {
  return request({ url: `/admin/video-sets/${setId}/chapters`, method: 'get' })
}

// 学生端：获取某班级的视频章节
export function getClassChapters(classId) {
  return request({ url: `/students/classes/${classId}/chapters`, method: 'get' })
}

// ========== 章节管理（教师端）==========
export function getChapters(videoSetId) {
  return request({
    url: '/courses/chapters',
    method: 'get',
    params: videoSetId != null ? { videoSetId } : {}
  })
}

export function addChapter(title, videoSetId) {
  return request({
    url: '/admin/chapters',
    method: 'post',
    data: { title, videoSetId }
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

// ========== 节管理（教师端）==========
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

// ========== 视频上传 ==========
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

// ========== 学生端：视频进度 ==========
export function saveVideoProgress(sectionId, currentTime, completed, classId) {
  return request({
    url: '/students/video-progress',
    method: 'post',
    data: { sectionId, currentTime, completed, classId }
  })
}

export function getVideoProgress() {
  return request({
    url: '/students/video-progress',
    method: 'get'
  })
}
