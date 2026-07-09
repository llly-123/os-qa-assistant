import request from './request'

// ========== 知识库（套）管理 ==========
export function getKnowledgeBases() {
  return request({ url: '/admin/knowledge-bases', method: 'get' })
}

export function createKnowledgeBase(data) {
  return request({ url: '/admin/knowledge-bases', method: 'post', data })
}

export function updateKnowledgeBase(id, data) {
  return request({ url: `/admin/knowledge-bases/${id}`, method: 'put', data })
}

export function deleteKnowledgeBase(id) {
  return request({ url: `/admin/knowledge-bases/${id}`, method: 'delete' })
}

// ========== 知识库内文档管理 ==========
export function getKnowledgeList(params) {
  return request({
    url: '/admin/knowledge',
    method: 'get',
    params
  })
}

export function uploadKnowledge(file, kbId, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('kbId', kbId)
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

export function getKnowledgeStatus(params) {
  return request({
    url: '/admin/knowledge/status',
    method: 'get',
    params
  })
}

export function importKnowledgeText(title, content, kbId) {
  return request({
    url: '/admin/knowledge/import-text',
    method: 'post',
    data: { title, content, kbId }
  })
}
