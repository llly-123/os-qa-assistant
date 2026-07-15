import { defineStore } from 'pinia'
import { ref } from 'vue'
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
  const currentSessionId = ref(null)
  const messages = ref([])
  const loading = ref(false)
  // AI 是否正在思考回答。放 store 而非组件局部，避免切换路由组件卸载后状态丢失
  // （否则切回来 isTyping=false，思考提示消失、输入框解锁，可连问导致“先问N个再答N个”）
  const isTyping = ref(false)

  // 学生所在班级列表 + 当前选中的班级
  const classes = ref([])
  const currentClassId = ref(null)

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
    currentClassId.value = classId
    currentSessionId.value = null
    messages.value = []
    await fetchSessions()
  }

  async function fetchSessions() {
    const res = await getChatSessions(currentClassId.value)
    sessions.value = res.data || []
    return sessions.value
  }

  async function fetchMessages(sessionId) {
    loading.value = true
    try {
      const res = await getChatMessages(sessionId)
      messages.value = res.data || []
      currentSessionId.value = sessionId
      return messages.value
    } finally {
      loading.value = false
    }
  }

  async function createSession(title = '新对话') {
    const res = await createChatSession(title, currentClassId.value)
    const newSession = res.data
    sessions.value.unshift(newSession)
    currentSessionId.value = newSession.id
    messages.value = []
    return newSession
  }

  async function deleteSession(sessionId) {
    await deleteChatSession(sessionId)
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
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
    updateLastMessage
  }
})
