<template>
  <div class="learn-container" v-if="inClass">
    <!-- Left: Chat Panel -->
    <div class="chat-panel" :style="{ width: isVideoCollapsed ? '100%' : leftWidth + 'px' }">
      <!-- Messages Area -->
      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" width="80" height="80">
              <rect x="12" y="14" width="40" height="36" rx="6" stroke="#cbd5e1" stroke-width="2" fill="none"/>
              <path d="M22 28h20M22 36h14" stroke="#cbd5e1" stroke-width="2" stroke-linecap="round"/>
              <circle cx="44" cy="38" r="8" fill="#eef2ff" stroke="#818cf8" stroke-width="1.5"/>
              <path d="M42 38h4M44 36v4" stroke="#818cf8" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h3>开始提问吧</h3>
          <p>我是《{{ userStore.courseName }}》课程的 AI 答疑助手</p>
          <p class="hint-text">直接输入您的问题开始提问</p>
        </div>

        <div
          v-for="(msg, index) in visibleMessages"
          :key="index"
          :class="['message-row', msg.role]"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="34" icon="UserFilled" :style="{ background: '#e2e8f0', color: '#64748b' }" />
            <el-avatar v-else :size="34" :style="{ background: 'linear-gradient(135deg, #6366f1, #4f46e5)' }">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none"><path d="M4 6h16M4 12h10M4 18h16" stroke="#fff" stroke-width="2" stroke-linecap="round"/></svg>
            </el-avatar>
          </div>
          <div class="message-body">
            <div class="message-meta">
              <span class="sender-name">
                {{ msg.role === 'user' ? '我' : 'AI 助手' }}
                <el-tag v-if="msg.videoContext" size="small" type="info" style="margin-left:4px">📺 视频相关</el-tag>
              </span>
              <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
            </div>
            <div class="message-bubble" :class="msg.role">
              <div v-if="msg.role === 'assistant'" class="markdown-content" v-html="renderMarkdown(msg.content)"></div>
              <div v-else class="user-content">{{ msg.content }}</div>
            </div>
            <!-- Citation -->
            <div v-if="msg.citation" class="citation-bar" :class="msg.sourceType === 'web' ? 'web' : 'textbook'">
              <span class="citation-icon">{{ msg.sourceType === 'web' ? '🌐' : '📚' }}</span>
              <div class="citation-text" v-html="renderCitation(msg.citation)"></div>
            </div>
          </div>
        </div>

        <!-- Typing Indicator -->
        <div v-if="isTyping" class="message-row assistant">
          <div class="message-avatar">
            <el-avatar :size="34" :style="{ background: 'linear-gradient(135deg, #6366f1, #4f46e5)' }">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none"><path d="M4 6h16M4 12h10M4 18h16" stroke="#fff" stroke-width="2" stroke-linecap="round"/></svg>
            </el-avatar>
          </div>
          <div class="message-body">
            <div class="message-meta"><span class="sender-name">AI 助手</span></div>
            <div class="message-bubble assistant typing-bubble">
              <span class="dot-pulse"></span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input-area">
        <div class="input-options-bar">
          <label class="web-search-toggle" :class="{ active: webSearchEnabled }">
            <input type="checkbox" v-model="webSearchEnabled" />
            <span class="toggle-icon">🌐</span>
            <span>联网搜索</span>
          </label>
          <span class="toggle-hint" v-if="!webSearchEnabled">开启后将搜索网络资源补充答案</span>
        </div>
        <div class="input-row">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            :placeholder="currentVideoSection ? `正在学习：${currentVideoSection.title}` : '输入您的问题...'"
            @keydown.enter.ctrl="sendMessage"
            :disabled="isTyping"
            class="msg-input"
          />
          <el-button
            type="primary"
            :loading="isTyping"
            :disabled="!inputMessage.trim()"
            @click="sendMessage"
            class="send-btn"
          >
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
        <div class="input-hint">Ctrl + Enter 发送</div>
      </div>
    </div>

    <!-- Resize Handle -->
    <div v-if="!isVideoCollapsed" class="resize-bar" @mousedown="startResize"></div>

    <!-- Right: Video Panel -->
    <div class="video-panel" :class="{ collapsed: isVideoCollapsed }">
      <div class="collapse-toggle" :class="{ 'at-edge': isVideoCollapsed }" @click="toggleVideoPanel"
        :title="isVideoCollapsed ? '展开视频' : '收起视频'">
        <el-icon :size="14">
          <ArrowRight v-if="isVideoCollapsed" />
          <ArrowLeft v-else />
        </el-icon>
      </div>

      <div class="video-panel-inner">
        <div class="video-topbar">
          <span class="video-title">📺 视频学习</span>
          <el-tag v-if="currentVideoSection" type="success" size="small">
            {{ currentVideoChapter?.title }} · {{ currentVideoSection.title }}
          </el-tag>
        </div>

        <!-- Chapter selector -->
        <div class="chapter-selector">
          <el-collapse v-model="expandedChapters">
            <el-collapse-item v-for="chapter in chapters" :key="chapter.id" :title="chapter.title" :name="chapter.id">
              <div
                v-for="section in chapter.sections"
                :key="section.id"
                :class="['section-row', { active: currentVideoSection?.id === section.id, disabled: !section.videoUrl }]"
                @click="selectVideoSection(section, chapter)"
              >
                <span class="section-icon">{{ section.videoUrl ? '🎬' : '🚫' }}</span>
                <span class="section-name">{{ section.title }}</span>
                <el-tag v-if="isSectionCompleted(section.id)" size="small" type="success" class="done-tag">已学</el-tag>
              </div>
              <div v-if="!chapter.sections || chapter.sections.length === 0" class="empty-section">暂无内容</div>
            </el-collapse-item>
          </el-collapse>
          <el-empty v-if="chapters.length === 0" description="暂无课程视频" :image-size="50" />
        </div>

        <!-- Video Player -->
        <div class="player-area">
          <div v-if="!currentVideoSection" class="video-placeholder">
            <el-icon :size="40" color="#cbd5e1"><VideoCamera /></el-icon>
            <p>从上方选择章节观看视频</p>
          </div>
          <div v-else-if="!currentVideoSection.videoUrl" class="video-placeholder">
            <el-icon :size="40" color="#cbd5e1"><VideoCamera /></el-icon>
            <p>该章节暂无视频</p>
          </div>
          <div v-else class="player-wrapper">
            <video
              ref="videoPlayer"
              :src="videoSrc(currentVideoSection.videoUrl)"
              controls
              @timeupdate="onVideoTimeUpdate"
              @loadedmetadata="onVideoLoaded"
              @ended="onVideoEnded"
            ></video>
            <el-button class="ask-video-btn" size="small" type="primary" @click="askAboutVideo">
              <el-icon><ChatDotRound /></el-icon> 提问当前内容
            </el-button>
          </div>
        </div>

        <!-- Progress -->
        <div class="progress-bar">
          <div class="progress-label">学习进度：{{ completedCount }} / {{ totalSectionCount }} 节</div>
          <el-progress
            :percentage="totalSectionCount > 0 ? Math.round(completedCount / totalSectionCount * 100) : 0"
            :stroke-width="6"
            :show-text="false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Promotion, VideoCamera, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { getClassChapters, getVideoProgress, saveVideoProgress } from '@/api/video'

// <video> 标签无法携带 Authorization 头，视频走 ?token= 查询参数鉴权
function videoSrc(url) {
  if (!url) return ''
  const token = localStorage.getItem('token')
  return token ? url + (url.includes('?') ? '&' : '?') + 'token=' + encodeURIComponent(token) : url
}

const chatStore = useChatStore()
const userStore = useUserStore()

const messagesContainer = ref(null)
const inputMessage = ref('')
const isTyping = computed(() => chatStore.isTyping)
const streamingContent = ref('')
const webSearchEnabled = ref(false)
const videoPlayer = ref(null)

const messages = computed(() => chatStore.messages)
const currentSessionId = computed(() => chatStore.currentSessionId)
// 展示用：过滤掉思考中的空 assistant 占位（靠 typing 指示器代替），回答回来后占位被填充自然显示
const visibleMessages = computed(() => chatStore.messages.filter(m => !(m.role === 'assistant' && !m.content)))

// Video state
const chapters = ref([])
const expandedChapters = ref([])
const currentVideoSection = ref(null)
const currentVideoChapter = ref(null)
const videoProgressMap = ref({})
let progressSaveTimer = null

// Class state：取自 chatStore（学生可同时加入多个班级，在侧边栏切换）
const inClass = computed(() => !!chatStore.currentClassId)
const currentClassId = computed(() => chatStore.currentClassId)

// Layout state
const leftWidth = ref(0)
const isResizing = ref(false)
const isVideoCollapsed = ref(false)

function toggleVideoPanel() {
  if (isVideoCollapsed.value) {
    isVideoCollapsed.value = false
    const savedWidth = localStorage.getItem('chatPanelWidth')
    leftWidth.value = savedWidth ? parseInt(savedWidth) : Math.floor(window.innerWidth * 0.5)
    localStorage.setItem('isVideoCollapsed', 'false')
  } else {
    localStorage.setItem('chatPanelWidth', leftWidth.value.toString())
    isVideoCollapsed.value = true
    leftWidth.value = window.innerWidth
    localStorage.setItem('isVideoCollapsed', 'true')
  }
}

onMounted(async () => {
  // 确保学生班级已加载（侧边栏也会加载，这里兜底）
  if (!chatStore.classes.length) {
    await chatStore.fetchClasses()
  }
  // 重新挂载时同步当前会话消息（回答可能已在后端但前端占位为空）
  if (chatStore.currentSessionId) {
    await chatStore.fetchMessages(chatStore.currentSessionId)
  }
  await loadChaptersAndProgress()

  const wasCollapsed = localStorage.getItem('isVideoCollapsed') === 'true'
  if (wasCollapsed) {
    isVideoCollapsed.value = true
    leftWidth.value = window.innerWidth
  } else {
    const saved = localStorage.getItem('chatPanelWidth')
    leftWidth.value = saved ? parseInt(saved) : Math.floor(window.innerWidth * 0.5)
  }
})

// 切换班级时重新加载该班级的视频章节
watch(() => chatStore.currentClassId, () => {
  loadChaptersAndProgress()
})

onBeforeUnmount(() => {
  if (progressSaveTimer) clearInterval(progressSaveTimer)
  if (localStorage.getItem('token')) saveCurrentProgress()
})

async function loadChaptersAndProgress() {
  try {
    if (!currentClassId.value) {
      chapters.value = []
      return
    }
    const [chaptersRes, progressRes] = await Promise.all([getClassChapters(currentClassId.value), getVideoProgress()])
    chapters.value = chaptersRes.data || []
    expandedChapters.value = chapters.value.map(c => c.id)

    const progressList = progressRes.data || []
    const map = {}
    progressList.forEach(p => { map[p.sectionId] = p })
    videoProgressMap.value = map

    const lastSectionId = localStorage.getItem('lastVideoSectionId')
    if (lastSectionId) {
      for (const ch of chapters.value) {
        const sec = ch.sections?.find(s => s.id === Number(lastSectionId))
        if (sec && sec.videoUrl) { selectVideoSection(sec, ch); break }
      }
    }
  } catch (e) { console.error('加载视频数据失败:', e) }
}

function selectVideoSection(section, chapter) {
  if (!section.videoUrl) return
  saveCurrentProgress()
  currentVideoSection.value = section
  currentVideoChapter.value = chapter
  localStorage.setItem('lastVideoSectionId', section.id)
  nextTick(() => {
    const progress = videoProgressMap.value[section.id]
    if (videoPlayer.value && progress && progress.playTime > 0) {
      videoPlayer.value.currentTime = progress.playTime
    }
  })
}

const completedCount = computed(() => {
  let count = 0
  chapters.value.forEach(ch => {
    ch.sections?.forEach(sec => {
      if (videoProgressMap.value[sec.id]?.completed === 1) count++
    })
  })
  return count
})

const totalSectionCount = computed(() => {
  let count = 0
  chapters.value.forEach(ch => { count += (ch.sections?.length || 0) })
  return count
})

function isSectionCompleted(sectionId) {
  return videoProgressMap.value[sectionId]?.completed === 1
}

function onVideoLoaded() {
  const progress = videoProgressMap.value[currentVideoSection.value?.id]
  if (progress && progress.playTime > 0 && videoPlayer.value) {
    videoPlayer.value.currentTime = progress.playTime
  }
}

function onVideoTimeUpdate() {
  if (!progressSaveTimer) progressSaveTimer = setInterval(saveCurrentProgress, 5000)
}

function onVideoEnded() {
  if (currentVideoSection.value) saveCurrentProgress(true)
}

async function saveCurrentProgress(completed = false) {
  if (!currentVideoSection.value || !videoPlayer.value) return
  const sectionId = currentVideoSection.value.id
  const currentTime = videoPlayer.value.currentTime
  const duration = videoPlayer.value.duration || 0
  const isCompleted = completed || (duration > 0 && currentTime / duration > 0.9)
  try {
    await saveVideoProgress(sectionId, currentTime, isCompleted, currentClassId.value)
    videoProgressMap.value[sectionId] = { ...videoProgressMap.value[sectionId], sectionId, playTime: currentTime, completed: isCompleted ? 1 : 0 }
  } catch (e) { /* silent */ }
}

// Resize
function startResize(e) {
  isResizing.value = true
  const startX = e.clientX
  const startWidth = leftWidth.value
  const onMouseMove = (e) => {
    const diff = e.clientX - startX
    leftWidth.value = Math.max(300, Math.min(window.innerWidth - 300, startWidth + diff))
  }
  const onMouseUp = () => {
    isResizing.value = false
    localStorage.setItem('chatPanelWidth', leftWidth.value.toString())
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

function askAboutVideo() {
  if (currentVideoSection.value) {
    if (isVideoCollapsed.value) toggleVideoPanel()
    inputMessage.value = `请解释当前视频中的：`
    nextTick(() => {
      const textarea = document.querySelector('.chat-input-area textarea')
      if (textarea) textarea.focus()
    })
  }
}

// KaTeX
function renderKatex(text) {
  if (!text) return ''
  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (_, formula) => {
    try { return katex.renderToString(formula.trim(), { displayMode: true, throwOnError: false }) } catch { return `$$${formula}$$` }
  })
  text = text.replace(/\\\[([\s\S]*?)\\\]/g, (_, formula) => {
    try { return katex.renderToString(formula.trim(), { displayMode: true, throwOnError: false }) } catch { return `\\[${formula}\\]` }
  })
  text = text.replace(/\$([^\$\n]+?)\$/g, (_, formula) => {
    try { return katex.renderToString(formula.trim(), { displayMode: false, throwOnError: false }) } catch { return `$${formula}$` }
  })
  text = text.replace(/\\\(([\s\S]*?)\\\)/g, (_, formula) => {
    try { return katex.renderToString(formula.trim(), { displayMode: false, throwOnError: false }) } catch { return `\\(${formula}\\)` }
  })
  return text
}

// Markdown
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) return hljs.highlight(code, { language: lang }).value
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

function renderMarkdown(content) {
  if (!content) return ''
  const katexRendered = renderKatex(content)
  const rawHtml = marked.parse(katexRendered)
  const sanitized = DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['target'], ADD_TAGS: ['span', 'math'] })
  return sanitized.replace(/<a /g, '<a target="_blank" rel="noopener noreferrer" ')
}

function renderCitation(citation) {
  if (!citation) return ''
  const rawHtml = marked.parse(citation)
  const sanitized = DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['target'] })
  return sanitized.replace(/<a /g, '<a target="_blank" rel="noopener noreferrer" ')
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  })
}

watch(visibleMessages, () => scrollToBottom(), { deep: true })

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || isTyping.value) return

  if (!currentSessionId.value) await chatStore.createSession()
  const sessionId = currentSessionId.value

  const hasVideoContext = !!currentVideoSection.value && !isVideoCollapsed.value
  chatStore.addMessage({ role: 'user', content, createTime: new Date().toISOString(), videoContext: hasVideoContext })
  inputMessage.value = ''

  chatStore.isTyping = true
  // 保存占位引用：回答返回后直接填充该对象，而非盲取 messages 末尾——
  // 否则若期间重新加载过会话（如切换路由后点了侧边栏会话触发 fetchMessages），
  // 末尾可能已变成 user 提问，会把 AI 回答错误塞进 user 消息，
  // 导致"提问不见了，变成我的气泡作出的回答"。
  const assistantMsg = { role: 'assistant', content: '', createTime: new Date().toISOString(), citation: null }
  chatStore.addMessage(assistantMsg)

  try {
    const token = localStorage.getItem('token')
    const body = { content, webSearch: webSearchEnabled.value }
    if (hasVideoContext) {
      body.videoContext = `${currentVideoChapter.value?.title} - ${currentVideoSection.value.title}`
      body.sectionId = currentVideoSection.value.id
    }

    const response = await fetch(`/api/chat/sessions/${sessionId}/ask`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify(body)
    })

    if (!response.ok) {
      if (response.status === 429) throw new Error('请求过于频繁，请稍后再试')
      throw new Error(`请求失败: ${response.status}`)
    }

    const res = await response.json()
    if (res.code === 200 && res.data) {
      if (chatStore.currentSessionId !== sessionId) {
        // 用户已切到别的会话，不打扰当前展示
      } else if (chatStore.messages.indexOf(assistantMsg) !== -1) {
        // 占位仍在，直接填充
        assistantMsg.content = res.data.content || '暂无回答'
        assistantMsg.citation = res.data.citation || null
        assistantMsg.sourceType = webSearchEnabled.value ? 'web' : 'textbook'
      } else {
        // 占位已被重新加载覆盖（期间点过会话等）；此时 DB 已含 assistant，以 DB 为准同步
        await chatStore.fetchMessages(sessionId)
      }
      chatStore.fetchSessions()
    } else {
      throw new Error(res.message || '请求失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error(error.message || '发送失败，请重试')
    const idx = chatStore.messages.indexOf(assistantMsg)
    if (idx !== -1 && !assistantMsg.content) chatStore.messages.splice(idx, 1)
  } finally {
    chatStore.isTyping = false
  }
}
</script>

<style scoped lang="scss">
.learn-container {
  height: 100%;
  display: flex;
  background: var(--color-bg);
  overflow: hidden;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  background: #fff;
  min-width: 300px;
  overflow: hidden;
}

// Messages
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  scroll-behavior: smooth;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;

  .empty-icon { margin-bottom: 20px; }

  h3 {
    font-size: 20px;
    color: var(--color-text-primary);
    margin-bottom: 8px;
    font-weight: 600;
  }

  p { color: var(--color-text-tertiary); font-size: 14px; margin: 4px 0; }

  .hint-text { font-size: 13px; }
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;

  &.user {
    flex-direction: row-reverse;

    .message-body { align-items: flex-end; }
    .message-meta { flex-direction: row-reverse; }
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 75%;
  min-width: 0;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 10px;

  .sender-name {
    font-size: 12px;
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .msg-time {
    font-size: 11px;
    color: var(--color-text-tertiary);
  }
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;

  &.user {
    background: var(--color-primary);
    color: #fff;
    border-bottom-right-radius: 6px;
  }

  &.assistant {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
    border-bottom-left-radius: 6px;
  }

  &.typing-bubble {
    padding: 16px 20px;
    display: flex;
    align-items: center;
  }
}

.user-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.dot-pulse {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: dotPulse 1.4s infinite ease-in-out both;
  position: relative;

  &::before, &::after {
    content: '';
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--color-primary);
    position: absolute;
    top: 0;
    animation: dotPulse 1.4s infinite ease-in-out both;
  }

  &::before { left: -16px; animation-delay: -0.32s; }
  &::after { left: 16px; animation-delay: 0.32s; }
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.75); }
  40% { opacity: 1; transform: scale(1); }
}

// Markdown in messages
.markdown-content {
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 14px 0 8px;
    color: var(--color-text-primary);
    font-weight: 600;
  }

  :deep(h1) { font-size: 1.4em; }
  :deep(h2) { font-size: 1.2em; }
  :deep(h3) { font-size: 1.05em; }

  :deep(p) { margin: 6px 0; }

  :deep(code) {
    background: #1e293b;
    color: #e2e8f0;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: var(--font-mono);
    font-size: 0.9em;
  }

  :deep(pre) {
    background: #1e293b;
    padding: 14px 16px;
    border-radius: 10px;
    overflow-x: auto;
    margin: 12px 0;

    code {
      background: transparent;
      padding: 0;
      color: #e2e8f0;
    }
  }

  :deep(ul), :deep(ol) {
    padding-left: 24px;
    margin: 8px 0;
  }

  :deep(li) { margin: 3px 0; }

  :deep(blockquote) {
    border-left: 3px solid var(--color-primary);
    padding: 4px 0 4px 14px;
    margin: 10px 0;
    color: var(--color-text-secondary);
    background: var(--color-primary-bg);
    border-radius: 0 6px 6px 0;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0;
    font-size: 13px;

    th, td {
      border: 1px solid var(--color-border);
      padding: 8px 12px;
      text-align: left;
    }

    th {
      background: var(--color-bg-secondary);
      font-weight: 600;
    }
  }
}

// Citation
.citation-bar {
  margin-top: 4px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 12px;
  display: flex;
  align-items: flex-start;
  gap: 8px;

  &.textbook {
    background: #fefce8;
    border: 1px solid #fef08a;
    color: #a16207;
  }

  &.web {
    background: #f0f9ff;
    border: 1px solid #bae6fd;
    color: #0369a1;
  }

  .citation-icon { flex-shrink: 0; font-size: 14px; }

  .citation-text {
    flex: 1;
    :deep(a) { color: var(--color-primary); text-decoration: none; &:hover { text-decoration: underline; } }
    :deep(p) { margin: 0; }
  }
}

// Input area
.chat-input-area {
  padding: 12px 20px 16px;
  border-top: 1px solid var(--color-border-light);
  background: #fff;
}

.input-options-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.web-search-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  transition: all 0.15s ease;
  user-select: none;

  input[type="checkbox"] { display: none; }

  &:hover {
    border-color: var(--color-primary-light);
  }

  &.active {
    background: var(--color-primary-bg);
    border-color: var(--color-primary-light);
    color: var(--color-primary);
    font-weight: 500;
  }
}

.toggle-hint {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.msg-input {
  flex: 1;
  :deep(.el-textarea__inner) {
    border-radius: 12px;
    resize: none;
    font-size: 14px;
    line-height: 1.6;
    padding: 10px 14px;
    border-color: var(--color-border);
    transition: border-color 0.2s;
    &:focus { border-color: var(--color-primary-light); }
  }
}

.send-btn {
  height: 44px;
  width: 44px;
  border-radius: 12px;
  flex-shrink: 0;
}

.input-hint {
  text-align: right;
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 6px;
}

// Resize
.resize-bar {
  width: 5px;
  cursor: col-resize;
  background: var(--color-border-light);
  transition: background 0.2s;
  flex-shrink: 0;

  &:hover { background: var(--color-primary-light); }
}

// Video panel
.video-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 300px;
  background: #fff;
  border-left: 1px solid var(--color-border);
  position: relative;
  transition: min-width 0.25s ease-out, flex 0.25s ease-out;

  &.collapsed {
    min-width: 0;
    flex: 0 0 0;
    overflow: hidden;
    border-left: none;
  }
}

.video-panel-inner {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-width: 300px;
}

.collapse-toggle {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  width: 22px;
  height: 40px;
  background: rgba(0,0,0,0.4);
  border-radius: 0 6px 6px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: background 0.2s;
  color: #fff;

  &:hover { background: rgba(0,0,0,0.6); }

  &.at-edge {
    position: fixed;
    right: 0;
    left: auto;
    border-radius: 6px 0 0 6px;
  }
}

.video-topbar {
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-secondary);

  .video-title {
    font-weight: 600;
    font-size: 14px;
    color: var(--color-text-primary);
  }
}

.chapter-selector {
  max-height: 200px;
  overflow-y: auto;
  border-bottom: 1px solid var(--color-border-light);

  .section-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px 8px 32px;
    cursor: pointer;
    transition: background 0.15s;
    font-size: 13px;

    &:hover { background: var(--color-bg); }
    &.active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 500; }
    &.disabled { color: var(--color-text-tertiary); cursor: not-allowed; }

    .section-name { flex: 1; }
  }

  .empty-section {
    padding: 8px 32px;
    color: var(--color-text-tertiary);
    font-size: 12px;
  }
}

.player-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #000;
  position: relative;
  min-height: 0;

  .video-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #94a3b8;
    p { margin-top: 10px; font-size: 14px; }
    h3 { margin-top: 12px; color: #f59e0b; font-size: 16px; }
  }

  .player-wrapper {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    position: relative;

    video {
      flex: 1;
      width: 100%;
      object-fit: contain;
      min-height: 0;
    }

    .ask-video-btn {
      position: absolute;
      bottom: 12px;
      right: 12px;
      z-index: 5;
      border-radius: 8px;
    }
  }
}

.progress-bar {
  padding: 10px 16px;
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg-secondary);

  .progress-label {
    font-size: 12px;
    color: var(--color-text-tertiary);
    margin-bottom: 4px;
  }
}
</style>
