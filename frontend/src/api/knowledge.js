import request from './request'

export function getKnowledgeList(params) {
  return request({
    url: '/admin/knowledge',
    method: 'get',
    params
  })
}

export function uploadKnowledge(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/knowledge/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  })
}

export function deleteKnowledge(id) {
  return request({
    url: `/admin/knowledge/${id}`,
    method: 'delete'
  })
}

export function rebuildKnowledgeIndex() {
  return request({
    url: '/admin/knowledge/rebuild',
    method: 'post'
  })
}

export function getKnowledgeStatus() {
  return request({
    url: '/admin/knowledge/status',
    method: 'get'
  })
}
