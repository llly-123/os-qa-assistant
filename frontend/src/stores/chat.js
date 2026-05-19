import { defineStore } from 'pinia'
import { ref } from 'vue'
import { 
  getChatSessions, 
  getChatMessages, 
  createChatSession, 
  deleteChatSession,
  updateChatSessionTitle
} from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const loading = ref(false)

  async function fetchSessions() {
    const res = await getChatSessions()
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
    const res = await createChatSession(title)
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
    fetchSessions,
    fetchMessages,
    createSession,
    deleteSession,
    updateSessionTitle,
    addMessage,
    updateLastMessage
  }
})
