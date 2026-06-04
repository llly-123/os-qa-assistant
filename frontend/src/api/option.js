import request from './request'

export function getOptions() {
  return request({
    url: '/admin/options',
    method: 'get'
  })
}

export function getOptionsByCategory(category) {
  return request({
    url: `/admin/options/${category}`,
    method: 'get'
  })
}

export function addOption(category, value) {
  return request({
    url: '/admin/options',
    method: 'post',
    data: { category, value }
  })
}

export function updateOption(id, value) {
  return request({
    url: `/admin/options/${id}`,
    method: 'put',
    data: { value }
  })
}

export function deleteOption(id) {
  return request({
    url: `/admin/options/${id}`,
    method: 'delete'
  })
}
