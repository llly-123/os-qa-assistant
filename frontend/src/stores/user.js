import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  
  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const username = computed(() => userInfo.value?.username || '')
  const userId = computed(() => userInfo.value?.id || '')

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
    login,
    logout,
    fetchUserInfo
  }
})
