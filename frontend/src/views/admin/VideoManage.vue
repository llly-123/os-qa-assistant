<template>
  <div class="video-manage">
    <div class="page-header">
      <h2>视频管理</h2>
      <el-button type="primary" @click="handleAddChapter">
        <el-icon><Plus /></el-icon>
        添加章
      </el-button>
    </div>

    <div class="content-layout">
      <!-- 左侧：章节树 -->
      <div class="chapter-tree">
        <el-empty v-if="chapters.length === 0" description="暂无章节，请添加" />
        <div v-for="chapter in chapters" :key="chapter.id" class="chapter-block">
          <div class="chapter-header">
            <div class="chapter-title">
              <el-icon><Folder /></el-icon>
              <span v-if="editingChapterId !== chapter.id">{{ chapter.title }}</span>
              <el-input
                v-else
                v-model="editingTitle"
                size="small"
                style="width: 200px"
                @keyup.enter="confirmEditChapter(chapter.id)"
                @blur="confirmEditChapter(chapter.id)"
              />
            </div>
            <div class="chapter-actions">
              <el-button size="small" link @click="moveChapterHandler(chapter.id, 'up')">
                <el-icon><Top /></el-icon>
              </el-button>
              <el-button size="small" link @click="moveChapterHandler(chapter.id, 'down')">
                <el-icon><Bottom /></el-icon>
              </el-button>
              <el-button size="small" link @click="startEditChapter(chapter)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button size="small" link @click="handleAddSection(chapter.id)">
                <el-icon><Plus /></el-icon>节
              </el-button>
              <el-button size="small" link type="danger" @click="handleDeleteChapter(chapter)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <div class="section-list">
            <div
              v-for="section in chapter.sections"
              :key="section.id"
              :class="['section-item', { active: selectedSection?.id === section.id }]"
              @click="selectSection(section, chapter)"
            >
              <div class="section-info">
                <span class="section-icon">{{ section.videoUrl ? '🎬' : '🚫' }}</span>
                <span v-if="editingSectionId !== section.id" class="section-title">{{ section.title }}</span>
                <el-input
                  v-else
                  v-model="editingTitle"
                  size="small"
                  style="width: 160px"
                  @keyup.enter="confirmEditSection(section.id)"
                  @blur="confirmEditSection(section.id)"
                />
              </div>
              <div class="section-actions" @click.stop>
                <el-button size="small" link @click="moveSectionHandler(section.id, 'up')">
                  <el-icon><Top /></el-icon>
                </el-button>
                <el-button size="small" link @click="moveSectionHandler(section.id, 'down')">
                  <el-icon><Bottom /></el-icon>
                </el-button>
                <el-button size="small" link @click="startEditSection(section)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button size="small" link type="danger" @click="handleDeleteSection(section)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <div v-if="!chapter.sections || chapter.sections.length === 0" class="empty-section">
              暂无节，点击上方添加
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：视频编辑区 -->
      <div class="video-edit-area">
        <div v-if="!selectedSection" class="empty-edit">
          <el-icon :size="48" color="#c0c4cc"><VideoCamera /></el-icon>
          <p>请选择左侧的节来管理视频</p>
        </div>
        <div v-else class="edit-content">
          <h3>{{ selectedChapter?.title }} - {{ selectedSection.title }}</h3>

          <!-- 已有视频 -->
          <div v-if="selectedSection.videoUrl" class="video-preview">
            <video
              :src="selectedSection.videoUrl"
              controls
              class="preview-player"
            ></video>
            <div class="video-meta">
              <span>文件大小：{{ formatSize(selectedSection.videoSize) }}</span>
            </div>
            <div class="video-actions">
              <el-button type="primary" @click="triggerUpload">更换视频</el-button>
              <el-button type="danger" @click="handleDeleteVideo">删除视频</el-button>
            </div>
          </div>

          <!-- 无视频：上传区 -->
          <div v-else class="upload-area">
            <el-upload
              :auto-upload="false"
              accept="video/*,.mp4,.webm,.avi,.mov,.mkv"
              :on-change="handleVideoChange"
              :show-file-list="false"
              drag
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">拖拽视频到此处，或<em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">支持 mp4、webm、avi 等格式，单个文件不超过500MB</div>
              </template>
            </el-upload>
          </div>

          <!-- 上传进度 -->
          <div v-if="uploading" class="upload-progress">
            <el-progress :percentage="uploadPercent" :format="() => `${uploadPercent}%`" />
            <p>正在上传，请勿关闭页面...</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getChapters, addChapter, updateChapter, deleteChapter, moveChapter,
  addSection, updateSection, deleteSection, moveSection,
  uploadVideo, deleteVideo
} from '@/api/video'

const chapters = ref([])
const selectedSection = ref(null)
const selectedChapter = ref(null)
const editingChapterId = ref(null)
const editingSectionId = ref(null)
const editingTitle = ref('')
const uploading = ref(false)
const uploadPercent = ref(0)

onMounted(() => {
  fetchChapters()
})

async function fetchChapters() {
  const res = await getChapters()
  chapters.value = res.data || []
}

function selectSection(section, chapter) {
  selectedSection.value = section
  selectedChapter.value = chapter
}

// ===== Chapter =====
async function handleAddChapter() {
  const { value } = await ElMessageBox.prompt('请输入章标题', '添加章', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '标题不能为空'
  }).catch(() => ({ value: null }))
  if (!value) return
  await addChapter(value.trim())
  ElMessage.success('添加成功')
  fetchChapters()
}

function startEditChapter(chapter) {
  editingChapterId.value = chapter.id
  editingTitle.value = chapter.title
}

async function confirmEditChapter(id) {
  if (!editingTitle.value.trim()) return
  await updateChapter(id, editingTitle.value.trim())
  editingChapterId.value = null
  fetchChapters()
}

async function handleDeleteChapter(chapter) {
  await ElMessageBox.confirm(
    `确定删除"${chapter.title}"及其下所有节？`,
    '警告', { type: 'warning' }
  )
  await deleteChapter(chapter.id)
  ElMessage.success('删除成功')
  if (selectedChapter.value?.id === chapter.id) {
    selectedSection.value = null
    selectedChapter.value = null
  }
  fetchChapters()
}

async function moveChapterHandler(id, direction) {
  await moveChapter(id, direction)
  fetchChapters()
}

// ===== Section =====
async function handleAddSection(chapterId) {
  const { value } = await ElMessageBox.prompt('请输入节标题', '添加节', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '标题不能为空'
  }).catch(() => ({ value: null }))
  if (!value) return
  await addSection(chapterId, value.trim())
  ElMessage.success('添加成功')
  fetchChapters()
}

function startEditSection(section) {
  editingSectionId.value = section.id
  editingTitle.value = section.title
}

async function confirmEditSection(id) {
  if (!editingTitle.value.trim()) return
  await updateSection(id, editingTitle.value.trim())
  editingSectionId.value = null
  fetchChapters()
}

async function handleDeleteSection(section) {
  await ElMessageBox.confirm(`确定删除"${section.title}"？`, '警告', { type: 'warning' })
  await deleteSection(section.id)
  ElMessage.success('删除成功')
  if (selectedSection.value?.id === section.id) {
    selectedSection.value = null
    selectedChapter.value = null
  }
  fetchChapters()
}

async function moveSectionHandler(id, direction) {
  await moveSection(id, direction)
  fetchChapters()
}

// ===== Video Upload =====
async function handleVideoChange(file) {
  if (!selectedSection.value) return
  uploading.value = true
  uploadPercent.value = 0
  try {
    await uploadVideo(selectedSection.value.id, file.raw, (e) => {
      if (e.total > 0) {
        uploadPercent.value = Math.round((e.loaded / e.total) * 100)
      }
    })
    ElMessage.success('上传成功')
    fetchChapters()
    // refresh selected
    const ch = chapters.value.find(c => c.id === selectedChapter.value.id)
    if (ch) {
      const sec = ch.sections?.find(s => s.id === selectedSection.value.id)
      if (sec) {
        selectedSection.value = sec
        selectedChapter.value = ch
      }
    }
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    uploadPercent.value = 0
  }
}

function triggerUpload() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'video/*,.mp4,.webm,.avi,.mov,.mkv'
  input.onchange = (e) => {
    const file = e.target.files[0]
    if (file) handleVideoChange({ raw: file })
  }
  input.click()
}

async function handleDeleteVideo() {
  await ElMessageBox.confirm('确定删除该视频？', '警告', { type: 'warning' })
  await deleteVideo(selectedSection.value.id)
  ElMessage.success('删除成功')
  fetchChapters()
  selectedSection.value = null
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(1)} ${units[i]}`
}
</script>

<style scoped lang="scss">
.video-manage {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 { margin: 0; }
}

.content-layout {
  flex: 1;
  display: flex;
  gap: 20px;
  overflow: hidden;
}

.chapter-tree {
  width: 400px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.chapter-block {
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.chapter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f5f7fa;
  font-weight: 600;

  .chapter-title {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .chapter-actions {
    display: flex;
    gap: 2px;
  }
}

.section-list {
  padding: 4px 0;
}

.section-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px 8px 28px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover { background: #f5f7fa; }
  &.active { background: #ecf5ff; }

  .section-info {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .section-icon { font-size: 14px; }
  .section-title { font-size: 14px; }

  .section-actions {
    display: flex;
    gap: 2px;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .section-actions { opacity: 1; }
}

.empty-section {
  padding: 8px 28px;
  color: #c0c4cc;
  font-size: 13px;
}

.video-edit-area {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background: #fff;
  overflow-y: auto;
}

.empty-edit {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  p { margin-top: 12px; }
}

.edit-content {
  h3 { margin: 0 0 20px; color: #303133; }
}

.video-preview {
  .preview-player {
    width: 100%;
    max-height: 400px;
    border-radius: 8px;
    background: #000;
  }

  .video-meta {
    margin-top: 8px;
    color: #909399;
    font-size: 13px;
  }

  .video-actions {
    margin-top: 16px;
    display: flex;
    gap: 8px;
  }
}

.upload-area {
  :deep(.el-upload-dragger) {
    padding: 40px;
  }
}

.upload-progress {
  margin-top: 20px;
  text-align: center;
  p { margin-top: 8px; color: #909399; font-size: 13px; }
}
</style>
