<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="quick-questions">
        <el-tag 
          v-for="q in quickQuestions" 
          :key="q"
          class="quick-tag"
          @click="askQuickQuestion(q)"
        >
          {{ q }}
        </el-tag>
      </div>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <el-icon :size="64" color="#c0c4cc"><ChatDotRound /></el-icon>
        <h3>开始提问吧！</h3>
        <p>我是基于西电《操作系统》教材的AI答疑助手</p>
        <p>点击上方标签快速提问，或直接输入您的问题</p>
      </div>
      
      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        :class="['message-item', msg.role]"
      >
        <div class="message-avatar">
          <el-avatar 
            v-if="msg.role === 'user'" 
            :size="36" 
            icon="User"
          />
          <el-avatar 
            v-else 
            :size="36" 
            style="background: #409eff"
          >
            <el-icon><Reading /></el-icon>
          </el-avatar>
        </div>
        
        <div class="message-content">
          <div class="message-header">
            <span class="role-name">{{ msg.role === 'user' ? '我' : 'AI助手' }}</span>
            <span class="time">{{ formatTime(msg.createTime) }}</span>
          </div>
          
          <div class="message-body">
            <div 
              v-if="msg.role === 'assistant'" 
              class="markdown-content"
              v-html="renderMarkdown(msg.content)"
            ></div>
            <div v-else class="user-content">{{ msg.content }}</div>
          </div>
          
          <div v-if="msg.citation" :class="['citation-card', msg.sourceType === 'web' ? 'citation-web' : 'citation-book']">
            <el-icon v-if="msg.sourceType === 'web'"><Link /></el-icon>
            <el-icon v-else><Document /></el-icon>
            <span v-if="msg.sourceType === 'web'">🌐 网络来源，仅供参考：{{ msg.citation }}</span>
            <span v-else>📚 参考资料：{{ msg.citation }}</span>
          </div>
        </div>
      </div>
      
      <div v-if="isTyping" class="message-item assistant">
        <div class="message-avatar">
          <el-avatar :size="36" style="background: #409eff">
            <el-icon><Reading /></el-icon>
          </el-avatar>
        </div>
        <div class="message-content">
          <div class="message-header">
            <span class="role-name">AI助手</span>
          </div>
          <div class="message-body">
            <div class="markdown-content" v-html="renderMarkdown(streamingContent)"></div>
            <span class="typing-indicator"></span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="3"
        placeholder="请输入您的操作系统问题..."
        @keydown.enter.ctrl="sendMessage"
        :disabled="isTyping"
      />
      <div class="input-actions">
        <div class="left-actions">
          <el-switch
            v-model="webSearchEnabled"
            active-text="联网搜索"
            inactive-text=""
            style="--el-switch-on-color: #409eff"
          />
          <el-tooltip content="开启后，当教材知识库未覆盖时将联网搜索补充回答" placement="top">
            <el-icon class="help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
          <span class="tip">Ctrl + Enter 发送</span>
        </div>
        <el-button 
          type="primary" 
          :loading="isTyping"
          :disabled="!inputMessage.trim()"
          @click="sendMessage"
        >
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { useChatStore } from '@/stores/chat'
import { sendMessageStream } from '@/api/chat'

const chatStore = useChatStore()

const messagesContainer = ref(null)
const inputMessage = ref('')
const isTyping = ref(false)
const streamingContent = ref('')
const webSearchEnabled = ref(false)

const messages = computed(() => chatStore.messages)
const currentSessionId = computed(() => chatStore.currentSessionId)

const quickQuestions = [
  '什么是进程死锁？',
  '死锁的四个必要条件',
  'LRU算法原理',
  '页面置换算法',
  '信号量与P/V操作',
  '进程调度算法'
]

marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

function renderMarkdown(content) {
  if (!content) return ''
  const rawHtml = marked.parse(content)
  return DOMPurify.sanitize(rawHtml)
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(messages, () => {
  scrollToBottom()
}, { deep: true })

watch(streamingContent, () => {
  scrollToBottom()
})

onMounted(() => {
  scrollToBottom()
})

function askQuickQuestion(question) {
  inputMessage.value = question
  sendMessage()
}

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || isTyping.value) return
  
  if (!currentSessionId.value) {
    await chatStore.createSession()
  }
  
  const userMessage = {
    role: 'user',
    content: content,
    createTime: new Date().toISOString()
  }
  chatStore.addMessage(userMessage)
  inputMessage.value = ''
  
  isTyping.value = true
  streamingContent.value = ''
  
  const assistantMessage = {
    role: 'assistant',
    content: '',
    createTime: new Date().toISOString(),
    citation: null,
    sourceType: null
  }
  chatStore.addMessage(assistantMessage)
  
  try {
    const response = await sendMessageStream(currentSessionId.value, content, webSearchEnabled.value)
    
    if (!response.ok) {
      throw new Error('请求失败')
    }
    
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')
      
      for (const line of lines) {
        if (line.startsWith('data: ')) {
          try {
            const data = JSON.parse(line.slice(6))
            
            if (data.type === 'content') {
              streamingContent.value += data.content
              const lastMsg = chatStore.messages[chatStore.messages.length - 1]
              lastMsg.content = streamingContent.value
            } else if (data.type === 'citation') {
              const lastMsg = chatStore.messages[chatStore.messages.length - 1]
              lastMsg.citation = data.citation
              lastMsg.sourceType = data.sourceType || 'book'
            } else if (data.type === 'error') {
              ElMessage.error(data.message || '回答出错')
            }
          } catch (e) {
            console.error('解析数据失败:', e)
          }
        }
      }
    }
    
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请重试')
    chatStore.messages.pop()
  } finally {
    isTyping.value = false
    streamingContent.value = ''
  }
}
</script>

<style scoped lang="scss">
.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f8f9fc 0%, #eef1f8 100%);
}

.chat-header {
  padding: 14px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  
  .quick-questions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    
    .quick-tag {
      cursor: pointer;
      transition: all 0.25s ease;
      border-radius: 16px;
      border: 1px solid #d9e2f0;
      background: #fff;
      
      &:hover {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        border-color: transparent;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #909399;
    
    h3 {
      margin: 16px 0 8px;
      color: #606266;
      font-size: 20px;
    }
    
    p {
      margin: 4px 0;
      font-size: 14px;
    }
  }
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: msgAppear 0.3s ease-out;
  
  &.user {
    .message-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-radius: 16px 16px 4px 16px;
    }
    
    .user-content {
      color: #fff !important;
    }
    
    .role-name {
      color: rgba(255, 255, 255, 0.9) !important;
    }
    
    .time {
      color: rgba(255, 255, 255, 0.6) !important;
    }
  }
  
  &.assistant {
    .message-content {
      background: #fff;
      border-radius: 16px 16px 16px 4px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    }
  }
}

@keyframes msgAppear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  max-width: 80%;
  border-radius: 12px;
  padding: 14px 18px;
  
  .message-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    
    .role-name {
      font-weight: 600;
      color: #303133;
      font-size: 13px;
    }
    
    .time {
      font-size: 12px;
      color: #909399;
    }
  }
  
  .message-body {
    .user-content {
      color: #303133;
      line-height: 1.7;
      white-space: pre-wrap;
    }
  }
}

.markdown-content {
  line-height: 1.8;
  color: #303133;
  
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 16px 0 8px;
    color: #303133;
  }
  
  :deep(p) {
    margin: 8px 0;
  }
  
  :deep(code) {
    background: #282c34;
    color: #abb2bf;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Fira Code', monospace;
  }
  
  :deep(pre) {
    background: #282c34;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 12px 0;
    
    code {
      background: transparent;
      padding: 0;
    }
  }
  
  :deep(ul), :deep(ol) {
    padding-left: 24px;
    margin: 8px 0;
  }
  
  :deep(blockquote) {
    border-left: 4px solid #409eff;
    padding-left: 16px;
    margin: 12px 0;
    color: #606266;
  }
  
  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0;
    
    th, td {
      border: 1px solid #dcdfe6;
      padding: 8px 12px;
    }
    
    th {
      background: #f5f7fa;
    }
  }
}

.citation-card {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
  
  &.citation-book {
    background: linear-gradient(135deg, #fef9e7 0%, #fdf2d1 100%);
    color: #b8860b;
    border-left: 3px solid #e6a23c;
  }
  
  &.citation-web {
    background: linear-gradient(135deg, #e8f4fd 0%, #d6ecfa 100%);
    color: #2b7ec1;
    border-left: 3px solid #409eff;
  }
}

.typing-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  animation: typing 1s infinite;
  margin-left: 4px;
}

@keyframes typing {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  
  :deep(.el-textarea__inner) {
    border-radius: 12px;
    border: 1px solid #e4e8f0;
    transition: all 0.25s ease;
    
    &:focus {
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
  }
  
  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    
    .left-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .help-icon {
        color: #909399;
        cursor: pointer;
        font-size: 16px;
        
        &:hover {
          color: #667eea;
        }
      }
      
      .tip {
        font-size: 12px;
        color: #909399;
        margin-left: 4px;
      }
    }
    
    .el-button--primary {
      border-radius: 10px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      padding: 8px 20px;
      
      &:hover {
        opacity: 0.9;
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
      }
    }
  }
}
</style>
