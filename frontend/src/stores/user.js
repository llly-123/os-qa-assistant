import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

function safeJsonParse(key, fallback = null) {
  try {
    const v = localStorage.getItem(key)
    return v && v !== 'undefined' ? JSON.parse(v) : fallback
  } catch {
    return fallback
  }
}

// 解码 JWT payload 并判断是否过期（参考后端 JwtUtil 的 exp 字段）
function isTokenExpired(token) {
  if (!token) return true
  try {
    const payload = token.split('.')[1]
    if (!payload) return true
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    // JWT 的 exp 是秒级时间戳
    return !decoded.exp || decoded.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

// 清理本地登录态（token 过期/失效时调用）
function clearAuthStorage() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('currentClassId')
  localStorage.removeItem('currentSessionId')
}

export const useUserStore = defineStore('user', () => {
  // 初始化时校验 token 是否过期，过期则清除，避免刷新后被误判为已登录
  const rawToken = localStorage.getItem('token') || ''
  if (rawToken && isTokenExpired(rawToken)) {
    clearAuthStorage()
  }
  const token = ref(isTokenExpired(rawToken) ? '' : rawToken)
  const userInfo = ref(safeJsonParse('userInfo'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const username = computed(() => userInfo.value?.username || '')
  const userId = computed(() => userInfo.value?.id || '')

  // 站点名称固定，品牌化设置功能已移除
  const siteName = computed(() => '智能答疑助手')
  const courseName = computed(() => '本课程')

  async function login(username, password) {
    const res = await loginApi(username, password)
    const data = res.data || res
    token.value = data.token
    userInfo.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.user))
    return res
  }

  async function logout() {
    await logoutApi()
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    // 清除班级/会话状态，避免下次登录残留旧班级导致进不去
    localStorage.removeItem('currentClassId')
    localStorage.removeItem('currentSessionId')
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    const data = res.data || res
    userInfo.value = data
    localStorage.setItem('userInfo', JSON.stringify(data))
    return res
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    role,
    username,
    userId,
    siteName,
    courseName,
    login,
    logout,
    fetchUserInfo
  }
})
