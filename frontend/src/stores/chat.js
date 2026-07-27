import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getChatSessions,
  getChatMessages,
  createChatSession,
  deleteChatSession,
  updateChatSessionTitle
} from '@/api/chat'
import { getMyClasses } from '@/api/clazz'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(localStorage.getItem('currentSessionId') ? Number(localStorage.getItem('currentSessionId')) : null)
  const messages = ref([])
  const loading = ref(false)

  // 借鉴 DeepSeek：每个会话独立维护生成状态，支持多会话并行
  // typingSessions 记录正在生成回答的 sessionId 集合
  const typingSessions = ref(new Set())
  // 每个 sessionId 对应的 AbortController，用于取消该会话的请求
  const abortControllers = new Map()

  // 当前会话是否正在生成（替代旧的全局 isTyping）
  const isTyping = computed(() =>
    currentSessionId.value ? typingSessions.value.has(currentSessionId.value) : false
  )

  // 标记某会话为生成中
  function setTyping(sessionId) {
    typingSessions.value.add(sessionId)
    // 触发响应式：Set 的 add 不会触发更新，需重新赋值
    typingSessions.value = new Set(typingSessions.value)
  }

  // 清除某会话的生成状态
  function clearTyping(sessionId) {
    typingSessions.value.delete(sessionId)
    typingSessions.value = new Set(typingSessions.value)
  }

  // 判断指定会话是否在生成中
  function isSessionTyping(sessionId) {
    return typingSessions.value.has(sessionId)
  }

  // 注册某会话的 AbortController（传 null 表示清除该条目）
  function setAbortController(sessionId, controller) {
    if (controller) {
      abortControllers.set(sessionId, controller)
    } else {
      abortControllers.delete(sessionId)
    }
  }

  // 取消指定会话的提问请求（不影响其他会话）
  function abortSessionAsk(sessionId) {
    const controller = abortControllers.get(sessionId)
    if (controller) {
      controller.abort()
    }
    abortControllers.delete(sessionId)
    clearTyping(sessionId)
  }

  // 取消所有正在进行的提问请求（如切换班级、退出登录时）
  function abortAllAsk() {
    abortControllers.forEach(controller => {
      if (controller) controller.abort()
    })
    abortControllers.clear()
    typingSessions.value = new Set()
  }

  // 学生所在班级列表 + 当前选中的班级
  const classes = ref([])
  const currentClassId = ref(localStorage.getItem('currentClassId') ? Number(localStorage.getItem('currentClassId')) : null)

  async function fetchClasses() {
    const res = await getMyClasses()
    classes.value = res.data || []
    // 不自动选中班级，让学生在首页手动选择
    if (classes.value.length === 0) {
      currentClassId.value = null
      sessions.value = []
      currentSessionId.value = null
      messages.value = []
    }
    return classes.value
  }

  async function setCurrentClass(classId) {
    // 切换班级会清空所有会话，取消所有未完成的提问
    abortAllAsk()
    currentClassId.value = classId
    localStorage.setItem('currentClassId', classId)
    currentSessionId.value = null
    localStorage.removeItem('currentSessionId')
    messages.value = []
    await fetchSessions()
  }

  async function fetchSessions() {
    const res = await getChatSessions(currentClassId.value)
    sessions.value = res.data || []
    return sessions.value
  }

  async function fetchMessages(sessionId) {
    // 切换会话不再取消其他会话的生成（借鉴 DeepSeek：后台继续生成）
    loading.value = true
    try {
      const res = await getChatMessages(sessionId)
      messages.value = res.data || []
      currentSessionId.value = sessionId
      localStorage.setItem('currentSessionId', sessionId)
      return messages.value
    } finally {
      loading.value = false
    }
  }

  async function createSession(title = '新对话') {
    // 新建对话不再取消其他会话的生成（借鉴 DeepSeek：并行会话）
    const res = await createChatSession(title, currentClassId.value)
    const newSession = res.data
    currentSessionId.value = newSession.id
    localStorage.setItem('currentSessionId', newSession.id)
    messages.value = []
    // 刷新会话列表以同步老对话标题（autoTitleIfNeeded 可能在后端已更新），
    // 同时确保新会话出现在列表中
    await fetchSessions()
    return newSession
  }

  async function deleteSession(sessionId) {
    // 删除会话时取消该会话的生成请求
    abortSessionAsk(sessionId)
    await deleteChatSession(sessionId)
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      localStorage.removeItem('currentSessionId')
      messages.value = []
    }
  }

  async function updateSessionTitle(sessionId, title) {
    await updateChatSessionTitle(sessionId, title)
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      session.title = title
    }
  }

  function addMessage(message) {
    messages.value.push(message)
  }

  function updateLastMessage(content) {
    if (messages.value.length > 0) {
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg.role === 'assistant') {
        lastMsg.content += content
      }
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    loading,
    isTyping,
    typingSessions,
    classes,
    currentClassId,
    fetchClasses,
    setCurrentClass,
    fetchSessions,
    fetchMessages,
    createSession,
    deleteSession,
    updateSessionTitle,
    addMessage,
    updateLastMessage,
    setTyping,
    clearTyping,
    isSessionTyping,
    setAbortController,
    abortSessionAsk,
    abortAllAsk
  }
})
