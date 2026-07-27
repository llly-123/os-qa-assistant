import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import { getPublicSettings } from '@/api/setting'

function safeJsonParse(key, fallback = null) {
  try {
    const v = localStorage.getItem(key)
    return v && v !== 'undefined' ? JSON.parse(v) : fallback
  } catch {
    return fallback
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(safeJsonParse('userInfo'))
  const settings = ref(safeJsonParse('appSettings', {}))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const username = computed(() => userInfo.value?.username || '')
  const userId = computed(() => userInfo.value?.id || '')

  const siteName = computed(() => settings.value?.site_name || '智能答疑助手')
  const courseName = computed(() => settings.value?.course_name || '本课程')
  const schoolName = computed(() => settings.value?.school_name || '')

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

  // 拉取公开品牌化设置（站点名/课程名/学校名），登录页等未登录场景也可调用
  async function fetchPublicSettings() {
    try {
      const res = await getPublicSettings()
      const data = res.data || res
      if (data) {
        settings.value = data
        localStorage.setItem('appSettings', JSON.stringify(data))
      }
      return data
    } catch (e) {
      return null
    }
  }

  return {
    token,
    userInfo,
    settings,
    isLoggedIn,
    role,
    username,
    userId,
    siteName,
    courseName,
    schoolName,
    login,
    logout,
    fetchUserInfo,
    fetchPublicSettings
  }
})
