import request from './request'

export function getChatSessions() {
  return request({
    url: '/chat/sessions',
    method: 'get'
  })
}

export function getChatMessages(sessionId) {
  return request({
    url: `/chat/sessions/${sessionId}/messages`,
    method: 'get'
  })
}

export function createChatSession(title) {
  return request({
    url: '/chat/sessions',
    method: 'post',
    data: { title }
  })
}

export function deleteChatSession(sessionId) {
  return request({
    url: `/chat/sessions/${sessionId}`,
    method: 'delete'
  })
}

export function updateChatSessionTitle(sessionId, title) {
  return request({
    url: `/chat/sessions/${sessionId}`,
    method: 'put',
    data: { title }
  })
}

export function sendMessage(sessionId, content, onMessage, onError, onComplete) {
  const token = localStorage.getItem('token')
  
  return new Promise((resolve, reject) => {
    const eventSource = new EventSource(
      `/api/chat/sessions/${sessionId}/stream?content=${encodeURIComponent(content)}&token=${token}`
    )
    
    let fullContent = ''
    
    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data)
      if (data.type === 'content') {
        fullContent += data.content
        onMessage && onMessage(data.content, fullContent)
      } else if (data.type === 'citation') {
        onMessage && onMessage(data.citation, fullContent, true)
      } else if (data.type === 'done') {
        eventSource.close()
        onComplete && onComplete(fullContent)
        resolve(fullContent)
      }
    }
    
    eventSource.onerror = (error) => {
      eventSource.close()
      onError && onError(error)
      reject(error)
    }
  })
}

export function sendMessageStream(sessionId, content, webSearch = false) {
  const token = localStorage.getItem('token')
  return fetch(`/api/chat/sessions/${sessionId}/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ content, webSearch })
  })
}
