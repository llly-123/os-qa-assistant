<template>
  <div class="knowledge-manage">
    <!-- 列表视图 -->
    <template v-if="viewMode === 'list'">
      <div class="page-header">
        <h2>知识库管理</h2>
        <div class="header-actions">
          <el-button type="primary" @click="handleCreateKb">
            <el-icon><Plus /></el-icon>
            新建知识库
          </el-button>
        </div>
      </div>

      <div class="kb-section">
        <div v-if="knowledgeBases.length === 0" class="kb-empty">
          <el-empty description="暂无知识库，请新建" />
        </div>

        <div v-else class="kb-grid">
          <div
            v-for="kb in knowledgeBases"
            :key="kb.id"
            class="kb-card"
            @click="enterKb(kb.id)"
          >
            <div class="kb-card-cover">
              <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" width="40" height="40">
                <rect width="48" height="48" rx="10" fill="rgba(99,102,241,0.12)"/>
                <path d="M12 14h24v20H12z" stroke="#6366f1" stroke-width="2.5" stroke-linejoin="round"/>
                <path d="M18 20h12M18 24h12M18 28h8" stroke="#6366f1" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="kb-card-info">
              <h4 class="kb-card-name">{{ kb.name }}</h4>
              <p class="kb-card-meta">{{ kb.documentCount != null ? kb.documentCount : 0 }} 篇文档</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 详情视图 -->
    <template v-else>
      <div class="page-header">
        <div class="detail-title">
          <el-button text @click="backToList">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <h2>{{ currentKbName }}</h2>
        </div>
        <div class="header-actions">
          <el-button @click="fetchStatus">
            <el-icon><Refresh /></el-icon>
            刷新状态
          </el-button>
          <el-button type="success" @click="showImportDialog = true">
            <el-icon><Document /></el-icon>
            文本导入
          </el-button>
          <el-button type="primary" @click="showUploadDialog = true">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
          <el-button @click="handleRenameKb">重命名</el-button>
          <el-button type="danger" plain @click="handleDeleteKb">删除</el-button>
        </div>
      </div>

      <el-card class="status-card">
        <template #header>
          <span>知识库状态</span>
        </template>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="文档数量">
            {{ status.documentCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="切片数量">
            {{ status.chunkCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="向量维度">
            {{ status.embeddingDimension || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="最后更新">
            {{ formatDate(status.lastUpdate) || '暂无' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div style="margin-top: 20px">
          <el-button type="warning" @click="handleRebuildIndex">
            <el-icon><RefreshRight /></el-icon>
            重建索引
          </el-button>
        </div>
      </el-card>
      
      <el-card style="margin-top: 20px">
        <template #header>
          <span>已上传文档</span>
        </template>
        
        <el-table :data="documents" v-loading="loading" stripe>
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag>{{ getFileType(row.fileName) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="120">
            <template #default="{ row }">
              {{ formatSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="切片数" width="100" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'">
                {{ row.status === 1 ? '已处理' : row.status === 2 ? '处理失败' : '处理中' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上传时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-dialog v-model="showUploadDialog" title="上传知识文档" width="600px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #title>
          支持上传PDF、Word、PPT、TXT格式的教材文档，系统将自动进行切片和向量化处理
        </template>
      </el-alert>
      
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="5"
        accept=".pdf,.doc,.docx,.ppt,.pptx,.txt"
        :on-change="handleFileChange"
        :file-list="fileList"
        drag
        multiple
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、PPT、TXT 格式，单个文件不超过50MB
          </div>
        </template>
      </el-upload>
      
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          开始上传
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportDialog" title="文本导入" width="800px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #title>
          适用于扫描版PDF或已有文本内容。将教材内容粘贴到下方，系统将自动切分和索引。
        </template>
      </el-alert>
      
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="importTitle" placeholder="如：课程讲义-第一章" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="importContent"
            type="textarea"
            :rows="15"
            placeholder="将教材文本内容粘贴到此处..."
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleFileImport">从TXT文件导入</el-button>
          <input
            ref="fileInputRef"
            type="file"
            accept=".txt"
            style="display: none"
            @change="onFileSelected"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import {
  getKnowledgeList,
  uploadKnowledge,
  deleteKnowledge,
  importKnowledgeText,
  rebuildKnowledgeIndex,
  getKnowledgeStatus,
  getKnowledgeBases,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase
} from '@/api/knowledge'

const loading = ref(false)
const uploading = ref(false)
const importing = ref(false)
const showUploadDialog = ref(false)
const showImportDialog = ref(false)
const documents = ref([])
const status = ref({})
const fileList = ref([])
const uploadRef = ref(null)
const fileInputRef = ref(null)
const importTitle = ref('')
const importContent = ref('')

const knowledgeBases = ref([])
const currentKbId = ref(null)
// 视图模式：list=知识库列表，detail=知识库详情
const viewMode = ref('list')

// 当前知识库名称
const currentKbName = computed(() => {
  const kb = knowledgeBases.value.find(k => k.id === currentKbId.value)
  return kb ? kb.name : ''
})

onMounted(async () => {
  await fetchKnowledgeBases()
})

async function fetchKnowledgeBases() {
  const res = await getKnowledgeBases()
  knowledgeBases.value = res.data || []
  // 不自动进入详情，停留在列表视图
  currentKbId.value = null
  documents.value = []
  status.value = {}
}

async function onKbChange() {
  await fetchDocuments()
  await fetchStatus()
}

// 点击卡片进入知识库详情
async function enterKb(kbId) {
  if (!kbId) return
  currentKbId.value = kbId
  viewMode.value = 'detail'
  await onKbChange()
}

// 返回知识库列表
function backToList() {
  viewMode.value = 'list'
}

async function handleCreateKb() {
  const { value } = await ElMessageBox.prompt('请输入知识库名称', '新建知识库', {
    confirmButtonText: '确定', cancelButtonText: '取消',
    inputPattern: /\S+/, inputErrorMessage: '名称不能为空'
  }).catch(() => ({ value: null }))
  if (!value) return
  await createKnowledgeBase({ name: value.trim() })
  ElMessage.success('创建成功')
  await fetchKnowledgeBases()
}

async function handleRenameKb() {
  const cur = knowledgeBases.value.find(k => k.id === currentKbId.value)
  const { value } = await ElMessageBox.prompt('请输入新的知识库名称', '重命名', {
    confirmButtonText: '确定', cancelButtonText: '取消',
    inputValue: cur?.name || '', inputPattern: /\S+/, inputErrorMessage: '名称不能为空'
  }).catch(() => ({ value: null }))
  if (!value) return
  await updateKnowledgeBase(currentKbId.value, { name: value.trim() })
  ElMessage.success('重命名成功')
  await fetchKnowledgeBases()
}

async function handleDeleteKb() {
  await ElMessageBox.confirm('删除知识库将同时删除其下所有文档与切片，确定删除？', '警告', { type: 'warning' })
  await deleteKnowledgeBase(currentKbId.value)
  ElMessage.success('删除成功')
  // 删除后返回列表视图
  viewMode.value = 'list'
  currentKbId.value = null
  await fetchKnowledgeBases()
}

async function fetchDocuments() {
  if (!currentKbId.value) { documents.value = []; return }
  loading.value = true
  try {
    const res = await getKnowledgeList({ kbId: currentKbId.value })
    documents.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function fetchStatus() {
  if (!currentKbId.value) { status.value = {}; return }
  try {
    const res = await getKnowledgeStatus({ kbId: currentKbId.value })
    status.value = res.data || {}
  } catch (error) {
    console.error('获取状态失败:', error)
  }
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(2)} ${units[i]}`
}

function getFileType(fileName) {
  if (!fileName) return '未知'
  const ext = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase()
  return ext
}

function handleFileChange(file, files) {
  fileList.value = files
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  
  console.log('开始上传，文件列表:', fileList.value.map(f => ({ name: f.name, raw: !!f.raw, size: f.size })))
  
  uploading.value = true
  let successCount = 0
  
  for (const file of fileList.value) {
    try {
      await uploadKnowledge(file.raw, currentKbId.value)
      successCount++
    } catch (error) {
      console.error(`上传 ${file.name} 失败:`, error)
      ElMessage.error(`上传 ${file.name} 失败: ${error.message || '未知错误'}`)
    }
  }
  
  uploading.value = false
  showUploadDialog.value = false
  fileList.value = []
  
  if (successCount > 0) {
    ElMessage.info(`已上传 ${successCount} 个文件，正在后台处理中，请等待...`)
    fetchDocuments()
    fetchStatus()
    startPolling()
  }
}

function startPolling() {
  let count = 0
  const maxPolls = 60
  const timer = setInterval(async () => {
    count++
    await fetchDocuments()
    await fetchStatus()
    const processing = documents.value.some(d => d.status === 0)
    if (!processing || count >= maxPolls) {
      clearInterval(timer)
      if (count >= maxPolls) {
        ElMessage.warning('处理超时，请稍后刷新查看结果')
      } else {
        const failed = documents.value.filter(d => d.status === 2)
        if (failed.length > 0) {
          ElMessage.error(`${failed.length} 个文件处理失败`)
        } else {
          ElMessage.success('所有文件处理完成！')
        }
      }
    }
  }, 3000)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档 ${row.fileName} 吗？相关的向量数据也将被删除`,
      '警告',
      { type: 'error' }
    )
    await deleteKnowledge(row.id)
    ElMessage.success('删除成功')
    fetchDocuments()
    fetchStatus()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

async function handleRebuildIndex() {
  try {
    await ElMessageBox.confirm(
      '确定要重建知识库索引吗？这可能需要较长时间',
      '提示',
      { type: 'warning' }
    )
    
    ElMessage.info('开始重建索引，请稍候...')
    await rebuildKnowledgeIndex()
    ElMessage.success('索引重建完成')
    fetchStatus()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重建失败:', error)
    }
  }
}

async function handleImport() {
  if (!importContent.value.trim()) {
    ElMessage.warning('请输入或导入文本内容')
    return
  }
  if (!importTitle.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  
  importing.value = true
  try {
    await importKnowledgeText(importTitle.value.trim(), importContent.value, currentKbId.value)
    ElMessage.info('文本导入成功，正在后台处理中...')
    showImportDialog.value = false
    importTitle.value = ''
    importContent.value = ''
    fetchDocuments()
    fetchStatus()
    startPolling()
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.message || '未知错误'))
  } finally {
    importing.value = false
  }
}

function handleFileImport() {
  fileInputRef.value.click()
}

function onFileSelected(event) {
  const file = event.target.files[0]
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = (e) => {
    importContent.value = e.target.result
    if (!importTitle.value) {
      importTitle.value = file.name.replace('.txt', '')
    }
    ElMessage.success(`已读取文件: ${file.name}`)
  }
  reader.readAsText(file, 'UTF-8')
  event.target.value = ''
}
</script>

<style scoped lang="scss">
.knowledge-manage {
  padding: 28px 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.status-card {
  margin-bottom: 20px;
}

/* 知识库 Grid 卡片 */
.kb-section {
  margin-bottom: 24px;
}

.kb-empty {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 40px 0;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

@media (max-width: 1000px) {
  .kb-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .kb-grid {
    grid-template-columns: 1fr;
  }
}

.kb-card {
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  display: flex;
  align-items: center;
  gap: 16px;

  &:hover {
    border-color: #c7d2fe;
    box-shadow: 0 8px 20px rgba(99, 102, 241, 0.1);
    transform: translateY(-3px);
  }
}

.kb-card-cover {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(99, 102, 241, 0.06);
}

.kb-card-info {
  flex: 1;
  min-width: 0;
}

.kb-card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-card-meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

/* 详情视图标题 */
.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
  }
}

.empty-kb-tip {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 40px 0;
}
</style>
