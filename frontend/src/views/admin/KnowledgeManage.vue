<template>
  <div class="knowledge-manage">
    <div class="page-header">
      <h2>知识库管理</h2>
      <div class="header-actions">
        <el-button @click="fetchStatus">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
        <el-button type="success" :disabled="!currentKbId" @click="showImportDialog = true">
          <el-icon><Document /></el-icon>
          文本导入
        </el-button>
        <el-button type="primary" :disabled="!currentKbId" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
      </div>
    </div>

    <div class="kb-bar">
      <span class="kb-label">知识库：</span>
      <el-select
        v-model="currentKbId"
        placeholder="请选择知识库"
        style="width: 280px"
        @change="onKbChange"
      >
        <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name + (kb.documentCount != null ? ` (${kb.documentCount}篇)` : '')" :value="kb.id" />
      </el-select>
      <el-button @click="handleCreateKb">新建知识库</el-button>
      <el-button :disabled="!currentKbId" @click="handleRenameKb">重命名</el-button>
      <el-button :disabled="!currentKbId" type="danger" plain @click="handleDeleteKb">删除</el-button>
    </div>

    <div v-if="!currentKbId" class="empty-kb-tip">
      <el-empty description="请先新建或选择一个知识库，再上传文档" />
    </div>

    <template v-else>
    
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

onMounted(async () => {
  await fetchKnowledgeBases()
})

async function fetchKnowledgeBases() {
  const res = await getKnowledgeBases()
  knowledgeBases.value = res.data || []
  if (knowledgeBases.value.length > 0) {
    currentKbId.value = knowledgeBases.value[0].id
    await fetchDocuments()
    await fetchStatus()
  } else {
    currentKbId.value = null
    documents.value = []
    status.value = {}
  }
}

async function onKbChange() {
  await fetchDocuments()
  await fetchStatus()
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
  currentKbId.value = knowledgeBases.value[0].id
  await fetchDocuments()
  await fetchStatus()
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

.kb-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 20px;

  .kb-label {
    font-size: 13px;
    color: var(--color-text-secondary);
    white-space: nowrap;
  }
}

.empty-kb-tip {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 40px 0;
}
</style>
