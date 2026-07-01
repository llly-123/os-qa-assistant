<template>
  <div class="learn-container">
    <!-- 左栏：聊天区 -->
    <div class="chat-panel" :style="{ width: leftWidth + 'px' }">
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
          <p v-if="!inClass" style="color: #e6a23c; font-size: 13px; margin-top: 8px">
            ⚠️ 未进入班级，教师无法统计问答情况
          </p>
          <p v-else>点击上方标签快速提问，或直接输入您的问题</p>
        </div>
        
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          :class="['message-item', msg.role]"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="36" icon="User" />
            <el-avatar v-else :size="36" style="background: #409eff">
              <el-icon><Reading /></el-icon>
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="role-name">
                {{ msg.role === 'user' ? '我' : 'AI助手' }}
                <el-tag v-if="msg.videoContext" size="small" type="info" style="margin-left:4px">📺</el-tag>
              </span>
              <span class="time">{{ formatTime(msg.createTime) }}</span>
            </div>
            <div class="message-body">
              <div v-if="msg.role === 'assistant'" class="markdown-content" v-html="renderMarkdown(msg.content)"></div>
              <div v-else class="user-content">{{ msg.content }}</div>
            </div>
            <div v-if="msg.citation" class="citation-card" :class="msg.sourceType === 'web' ? 'web-source' : ''">
              <el-icon><Document /></el-icon>
              <div class="citation-content" v-html="renderCitation(msg.citation)"></div>
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
            <div class="message-header"><span class="role-name">AI助手</span></div>
            <div class="message-body">
              <div class="markdown-content" v-html="renderMarkdown(streamingContent)"></div>
              <span class="typing-indicator"></span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="chat-input">
        <div class="input-options">
          <el-checkbox v-model="webSearchEnabled" label="联网搜索" />
          <span class="search-tip">开启后将搜索网络资源获取答案</span>
        </div>
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          :placeholder="currentVideoSection ? `正在观看：${currentVideoSection.title}，请输入问题...` : '请输入您的操作系统问题...'"
          @keydown.enter.ctrl="sendMessage"
          :disabled="isTyping"
        />
        <div class="input-actions">
          <span class="tip">Ctrl + Enter 发送</span>
          <el-button type="primary" :loading="isTyping" :disabled="!inputMessage.trim()" @click="sendMessage">
            <el-icon><Promotion /></el-icon> 发送
          </el-button>
        </div>
      </div>
    </div>

    <!-- 拖拽分隔条 -->
    <div v-if="!isVideoCollapsed" class="resize-bar" @mousedown="startResize"></div>

    <!-- 右栏：视频学习区 -->
    <div class="video-panel" :class="{ collapsed: isVideoCollapsed }">
      <!-- 收起/展开浮动箭头 -->
      <div
        class="collapse-toggle"
        :class="{ 'at-edge': isVideoCollapsed }"
        @click="toggleVideoPanel"
        :title="isVideoCollapsed ? '显示视频' : '隐藏视频'"
        tabindex="0"
        @keydown.enter="toggleVideoPanel"
        @keydown.space.prevent="toggleVideoPanel"
      >
        <el-icon :size="16">
          <ArrowRight v-if="isVideoCollapsed" />
          <ArrowLeft v-else />
        </el-icon>
      </div>

      <div class="video-panel-content">
        <!-- 未进入班级：锁定视频区 -->
        <div v-if="!inClass" class="no-class-notice">
          <el-icon :size="48" color="#e6a23c"><Lock /></el-icon>
          <h3>请先进入班级</h3>
          <p>联系教师将你加入班级后即可观看视频</p>
        </div>

        <!-- 已进入班级：正常显示 -->
        <template v-else>
        <div class="video-header">
        <span class="video-title">视频学习</span>
        <el-tag v-if="currentVideoSection" type="success" size="small">
          {{ currentVideoChapter?.title }} - {{ currentVideoSection.title }}
        </el-tag>
      </div>

      <!-- 章节选择器 -->
      <div class="chapter-selector">
        <el-collapse v-model="expandedChapters">
          <el-collapse-item v-for="chapter in chapters" :key="chapter.id" :title="chapter.title" :name="chapter.id">
            <div
              v-for="section in chapter.sections"
              :key="section.id"
              :class="['section-option', { active: currentVideoSection?.id === section.id, disabled: !section.videoUrl }]"
              @click="selectVideoSection(section, chapter)"
            >
              <span class="section-icon">{{ section.videoUrl ? '🎬' : '🚫' }}</span>
              <span class="section-name">{{ section.title }}</span>
              <el-tag v-if="isSectionCompleted(section.id)" size="small" type="success">已学</el-tag>
            </div>
            <div v-if="!chapter.sections || chapter.sections.length === 0" class="empty-section">暂无内容</div>
          </el-collapse-item>
        </el-collapse>
        <el-empty v-if="chapters.length === 0" description="暂无课程视频" :image-size="60" />
      </div>

      <!-- 视频播放器 -->
      <div class="video-player-area">
        <div v-if="!currentVideoSection" class="no-video">
          <el-icon :size="48" color="#c0c4cc"><VideoCamera /></el-icon>
          <p>请从上方选择章节观看视频</p>
        </div>
        <div v-else-if="!currentVideoSection.videoUrl" class="no-video">
          <el-icon :size="48" color="#c0c4cc"><VideoCamera /></el-icon>
          <p>该章节暂无视频</p>
        </div>
        <div v-else class="player-wrapper">
          <video
            ref="videoPlayer"
            :src="currentVideoSection.videoUrl"
            controls
            @timeupdate="onVideoTimeUpdate"
            @loadedmetadata="onVideoLoaded"
            @ended="onVideoEnded"
          ></video>
          <el-button class="ask-btn" size="small" type="primary" @click="askAboutVideo">
            <el-icon><ChatDotRound /></el-icon> 提问
          </el-button>
        </div>
      </div>

      <!-- 学习进度 -->
      <div class="progress-bar-area">
        <span class="progress-text">学习进度：{{ completedCount }} / {{ totalSectionCount }} 节</span>
        <el-progress :percentage="totalSectionCount > 0 ? Math.round(completedCount / totalSectionCount * 100) : 0" :stroke-width="8" />
      </div>
        </template>
      </div><!-- end video-panel-content -->
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Reading, Document, Promotion, VideoCamera, ArrowLeft, ArrowRight, Lock } from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import { useChatStore } from '@/stores/chat'
import { getQuickPrompts } from '@/api/chat'
import { getChapters, getVideoProgress, saveVideoProgress } from '@/api/video'
import { getMyClass } from '@/api/clazz'

const chatStore = useChatStore()

const messagesContainer = ref(null)
const inputMessage = ref('')
const isTyping = ref(false)
const streamingContent = ref('')
const webSearchEnabled = ref(false)
const videoPlayer = ref(null)

const messages = computed(() => chatStore.messages)
const currentSessionId = computed(() => chatStore.currentSessionId)

const quickQuestions = ref([])

// ===== 视频相关 =====
const chapters = ref([])
const expandedChapters = ref([])
const currentVideoSection = ref(null)
const currentVideoChapter = ref(null)
const videoProgressMap = ref({}) // sectionId -> { currentTime, completed }
let progressSaveTimer = null

// ===== 班级状态 =====
const myClass = ref(null) // null = 未进班
const inClass = ref(false)

// ===== 拖拽分隔 =====
const leftWidth = ref(0)
const isResizing = ref(false)

// ===== 视频区收起/展开 =====
const isVideoCollapsed = ref(false)
const savedVideoWidth = ref(0) // 收起前保存的视频区宽度

function toggleVideoPanel() {
  if (isVideoCollapsed.value) {
    // 展开：恢复之前的宽度
    isVideoCollapsed.value = false
    const containerWidth = window.innerWidth
    const savedWidth = localStorage.getItem('chatPanelWidth')
    leftWidth.value = savedWidth ? parseInt(savedWidth) : Math.floor(containerWidth * 0.5)
    localStorage.setItem('isVideoCollapsed', 'false')
  } else {
    // 收起：保存当前宽度，然后收起
    localStorage.setItem('chatPanelWidth', leftWidth.value.toString())
    savedVideoWidth.value = leftWidth.value
    isVideoCollapsed.value = true
    leftWidth.value = window.innerWidth
    localStorage.setItem('isVideoCollapsed', 'true')
  }
}

onMounted(() => {
  loadQuickPrompts()
  scrollToBottom()
  loadClassAndVideoData()

  // 恢复宽度和收起状态偏好
  const wasCollapsed = localStorage.getItem('isVideoCollapsed') === 'true'
  if (wasCollapsed) {
    isVideoCollapsed.value = true
    leftWidth.value = window.innerWidth
  } else {
    const saved = localStorage.getItem('chatPanelWidth')
    leftWidth.value = saved ? parseInt(saved) : Math.floor(window.innerWidth * 0.5)
  }
})

onBeforeUnmount(() => {
  if (progressSaveTimer) clearInterval(progressSaveTimer)
  // 仅在仍有有效token时保存进度
  if (localStorage.getItem('token')) {
    saveCurrentProgress()
  }
})

// ===== 快捷提示 =====
async function loadQuickPrompts() {
  try {
    const res = await getQuickPrompts()
    if (res?.data?.length > 0) {
      quickQuestions.value = res.data
    } else {
      quickQuestions.value = ['什么是进程死锁？', '死锁的四个必要条件', 'LRU算法原理', '页面置换算法', '信号量与P/V操作', '进程调度算法']
    }
  } catch (e) {
    quickQuestions.value = ['什么是进程死锁？', '死锁的四个必要条件', 'LRU算法原理', '页面置换算法', '信号量与P/V操作', '进程调度算法']
  }
}

// ===== 视频数据 =====
async function loadClassAndVideoData() {
  try {
    // 先检查班级状态
    const classRes = await getMyClass()
    if (classRes.data) {
      myClass.value = classRes.data
      inClass.value = true
    } else {
      myClass.value = null
      inClass.value = false
    }

    // 仅在班级中才加载视频数据
    if (inClass.value) {
      const [chaptersRes, progressRes] = await Promise.all([getChapters(), getVideoProgress()])
      chapters.value = chaptersRes.data || []
      expandedChapters.value = chapters.value.map(c => c.id)

      const progressList = progressRes.data || []
      const map = {}
      progressList.forEach(p => { map[p.sectionId] = p })
      videoProgressMap.value = map

      // 恢复上次观看的视频
      const lastSectionId = localStorage.getItem('lastVideoSectionId')
      if (lastSectionId) {
        for (const ch of chapters.value) {
          const sec = ch.sections?.find(s => s.id === Number(lastSectionId))
          if (sec && sec.videoUrl) {
            selectVideoSection(sec, ch)
            break
          }
        }
      }
    }
  } catch (e) {
    console.error('加载班级/视频数据失败:', e)
  }
}

function selectVideoSection(section, chapter) {
  if (!section.videoUrl) return
  // 保存当前视频进度
  saveCurrentProgress()

  currentVideoSection.value = section
  currentVideoChapter.value = chapter
  localStorage.setItem('lastVideoSectionId', section.id)

  // 恢复该节的播放进度
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

// ===== 视频进度 =====
function onVideoLoaded() {
  const progress = videoProgressMap.value[currentVideoSection.value?.id]
  if (progress && progress.playTime > 0 && videoPlayer.value) {
    videoPlayer.value.currentTime = progress.playTime
  }
}

function onVideoTimeUpdate() {
  // 每5秒自动保存
  if (!progressSaveTimer) {
    progressSaveTimer = setInterval(saveCurrentProgress, 5000)
  }
}

function onVideoEnded() {
  if (currentVideoSection.value) {
    saveCurrentProgress(true)
  }
}

async function saveCurrentProgress(completed = false) {
  if (!currentVideoSection.value || !videoPlayer.value) return
  const sectionId = currentVideoSection.value.id
  const currentTime = videoPlayer.value.currentTime
  const duration = videoPlayer.value.duration || 0

  // 播放超过90%视为完成
  const isCompleted = completed || (duration > 0 && currentTime / duration > 0.9)

  try {
    await saveVideoProgress(sectionId, currentTime, isCompleted)
    videoProgressMap.value[sectionId] = {
      ...videoProgressMap.value[sectionId],
      sectionId,
      playTime: currentTime,
      completed: isCompleted ? 1 : 0
    }
  } catch (e) {
    // 静默失败
  }
}

// ===== 拖拽分隔 =====
function startResize(e) {
  isResizing.value = true
  const startX = e.clientX
  const startWidth = leftWidth.value

  const onMouseMove = (e) => {
    const diff = e.clientX - startX
    const newWidth = Math.max(300, Math.min(window.innerWidth - 300, startWidth + diff))
    leftWidth.value = newWidth
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

// ===== 提问视频 =====
function askAboutVideo() {
  if (currentVideoSection.value) {
    if (isVideoCollapsed.value) toggleVideoPanel()
    inputMessage.value = `请解释当前视频中的：`
    nextTick(() => {
      const textarea = document.querySelector('.chat-input textarea')
      if (textarea) textarea.focus()
    })
  }
}

// ===== KaTeX公式渲染 =====
function renderKatex(text) {
  if (!text) return ''
  // 渲染块级公式 $$...$$ 或 \[...\]
  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: true, throwOnError: false })
    } catch { return `$$${formula}$$` }
  })
  text = text.replace(/\\\[([\s\S]*?)\\\]/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: true, throwOnError: false })
    } catch { return `\\[${formula}\\]` }
  })
  // 渲染行内公式 $...$ 或 \(...\)
  text = text.replace(/\$([^\$\n]+?)\$/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: false, throwOnError: false })
    } catch { return `$${formula}$` }
  })
  text = text.replace(/\\\(([\s\S]*?)\\\)/g, (_, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: false, throwOnError: false })
    } catch { return `\\(${formula}\\)` }
  })
  return text
}

// ===== Markdown渲染 =====
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
  // 先渲染KaTeX公式，再渲染Markdown
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

watch(messages, () => scrollToBottom(), { deep: true })

function askQuickQuestion(question) {
  inputMessage.value = question
  sendMessage()
}

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || isTyping.value) return
  
  if (!currentSessionId.value) await chatStore.createSession()
  
  const hasVideoContext = !!currentVideoSection.value && !isVideoCollapsed.value
  const userMessage = {
    role: 'user',
    content: content,
    createTime: new Date().toISOString(),
    videoContext: hasVideoContext
  }
  chatStore.addMessage(userMessage)
  inputMessage.value = ''
  
  isTyping.value = true
  
  const assistantMessage = {
    role: 'assistant',
    content: '',
    createTime: new Date().toISOString(),
    citation: null
  }
  chatStore.addMessage(assistantMessage)
  
  try {
    const token = localStorage.getItem('token')
    const body = { content, webSearch: webSearchEnabled.value }
    // 附带视频上下文
    if (hasVideoContext) {
      body.videoContext = `${currentVideoChapter.value?.title} - ${currentVideoSection.value.title}`
      body.sectionId = currentVideoSection.value.id
    }
    
    const response = await fetch(`/api/chat/sessions/${currentSessionId.value}/ask`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(body)
    })
    
    if (!response.ok) {
      if (response.status === 429) throw new Error('请求过于频繁，请稍后再试')
      throw new Error(`请求失败: ${response.status}`)
    }
    
    const res = await response.json()
    
    if (res.code === 200 && res.data) {
      const lastMsg = chatStore.messages[chatStore.messages.length - 1]
      lastMsg.content = res.data.content || '暂无回答'
      lastMsg.citation = res.data.citation || null
      lastMsg.sourceType = webSearchEnabled.value ? 'web' : 'textbook'
      chatStore.fetchSessions()
    } else {
      throw new Error(res.message || '请求失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error(error.message || '发送失败，请重试')
    const lastMsg = chatStore.messages[chatStore.messages.length - 1]
    if (lastMsg && lastMsg.role === 'assistant' && !lastMsg.content) {
      chatStore.messages.pop()
    }
  } finally {
    isTyping.value = false
  }
}
</script>

<style scoped lang="scss">
.learn-container {
  height: 100%;
  display: flex;
  background: #f5f7fa;
  overflow: hidden;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  background: #fff;
  min-width: 300px;
  overflow: hidden;
}

.resize-bar {
  width: 6px;
  cursor: col-resize;
  background: #e4e7ed;
  transition: background 0.2s;
  flex-shrink: 0;

  &:hover {
    background: #409eff;
  }
}

.video-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 300px;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  position: relative;
  transition: min-width 0.25s ease-out, flex 0.25s ease-out;

  &.collapsed {
    min-width: 0;
    flex: 0 0 0px;
    overflow: hidden;
    border-left: none;
  }

  .video-panel-content {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    min-width: 300px;
  }
}

.collapse-toggle {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 0 4px 4px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  transition: background 0.2s, transform 0.2s;
  color: #fff;

  &:hover {
    background: rgba(0, 0, 0, 0.7);
  }

  &:focus {
    outline: 2px solid #409eff;
    outline-offset: 2px;
  }

  &.at-edge {
    position: fixed;
    right: 0;
    left: auto;
    border-radius: 4px 0 0 4px;
  }
}

@media (max-width: 600px) {
  .collapse-toggle {
    display: none;
  }
}

.video-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fafafa;

  .video-title {
    font-weight: 600;
    font-size: 15px;
    color: #303133;
  }
}

.chapter-selector {
  max-height: 200px;
  overflow-y: auto;
  border-bottom: 1px solid #e4e7ed;

  .section-option {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px 8px 32px;
    cursor: pointer;
    transition: background 0.2s;
    font-size: 14px;

    &:hover { background: #f5f7fa; }
    &.active { background: #ecf5ff; color: #409eff; }
    &.disabled { color: #c0c4cc; cursor: not-allowed; }

    .section-icon { font-size: 14px; }
    .section-name { flex: 1; }
  }

  .empty-section {
    padding: 8px 32px;
    color: #c0c4cc;
    font-size: 13px;
  }
}

.video-player-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #000;
  position: relative;

  .no-video {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #909399;
    p { margin-top: 12px; }
  }

  .no-class-notice {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #909399;
    text-align: center;

    h3 {
      margin-top: 16px;
      color: #e6a23c;
    }
    p {
      margin-top: 8px;
      font-size: 14px;
    }
  }

  .player-wrapper {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;

    video {
      flex: 1;
      width: 100%;
      object-fit: contain;
    }

    .ask-btn {
      position: absolute;
      bottom: 50px;
      right: 16px;
      z-index: 10;
    }
  }
}

.progress-bar-area {
  padding: 10px 16px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;

  .progress-text {
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;
    display: block;
  }
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
  
  .quick-questions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    
    .quick-tag {
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: #409eff; color: #fff; }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #909399;
    h3 { margin: 16px 0 8px; color: #606266; }
    p { margin: 4px 0; font-size: 14px; }
  }
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  
  &.user .message-content { background: #ecf5ff; }
  &.assistant .message-content { background: #f5f7fa; }
}

.message-avatar { flex-shrink: 0; }

.message-content {
  flex: 1;
  max-width: 80%;
  border-radius: 8px;
  padding: 12px 16px;
  
  .message-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    .role-name { font-weight: 600; color: #303133; }
    .time { font-size: 12px; color: #909399; }
  }
  
  .message-body .user-content { color: #303133; line-height: 1.6; white-space: pre-wrap; }
}

.markdown-content {
  line-height: 1.8;
  color: #303133;
  
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) { margin: 16px 0 8px; color: #303133; }
  :deep(p) { margin: 8px 0; }
  :deep(code) { background: #282c34; color: #abb2bf; padding: 2px 6px; border-radius: 4px; font-family: 'Fira Code', monospace; }
  :deep(pre) { background: #282c34; padding: 16px; border-radius: 8px; overflow-x: auto; margin: 12px 0; code { background: transparent; padding: 0; } }
  :deep(ul), :deep(ol) { padding-left: 24px; margin: 8px 0; }
  :deep(blockquote) { border-left: 4px solid #409eff; padding-left: 16px; margin: 12px 0; color: #606266; }
  :deep(table) { border-collapse: collapse; width: 100%; margin: 12px 0; th, td { border: 1px solid #dcdfe6; padding: 8px 12px; } th { background: #f5f7fa; } }
}

.citation-card {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #fdf6ec;
  color: #e6a23c;
  
  .citation-content {
    flex: 1;
    line-height: 1.8;
    :deep(a) { color: #409eff; text-decoration: none; &:hover { text-decoration: underline; } }
    :deep(p) { margin: 0; }
  }

  &.web-source { background: #ecf5ff; color: #409eff; border: 1px solid #d9ecff; }
}

.typing-indicator {
  display: inline-block;
  width: 8px; height: 8px;
  background: #409eff;
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
  border-top: 1px solid #e4e7ed;
  background: #fff;

  .input-options {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    .search-tip { font-size: 12px; color: #909399; }
  }

  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    .tip { font-size: 12px; color: #909399; }
  }
}
</style>
