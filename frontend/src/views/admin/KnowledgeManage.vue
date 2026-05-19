<template>
  <div class="knowledge-manage">
    <div class="page-header">
      <h2>知识库管理</h2>
      <div class="header-actions">
        <el-button @click="fetchStatus">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
        <el-button type="primary" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
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
        <el-table-column prop="name" label="文件名" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="100" />
        <el-table-column prop="uploadTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.uploadTime) }}
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getKnowledgeList, 
  uploadKnowledge, 
  deleteKnowledge,
  rebuildKnowledgeIndex,
  getKnowledgeStatus
} from '@/api/knowledge'

const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const documents = ref([])
const status = ref({})
const fileList = ref([])
const uploadRef = ref(null)

onMounted(() => {
  fetchDocuments()
  fetchStatus()
})

async function fetchDocuments() {
  loading.value = true
  try {
    const res = await getKnowledgeList()
    documents.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function fetchStatus() {
  try {
    const res = await getKnowledgeStatus()
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
  while (bytes >= 1024 && i < units.length - 1) {
    bytes /= 1024
    i++
  }
  return `${bytes.toFixed(2)} ${units[i]}`
}

function handleFileChange(file, files) {
  fileList.value = files
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  
  uploading.value = true
  let successCount = 0
  
  for (const file of fileList.value) {
    try {
      await uploadKnowledge(file.raw)
      successCount++
    } catch (error) {
      console.error(`上传 ${file.name} 失败:`, error)
    }
  }
  
  uploading.value = false
  showUploadDialog.value = false
  fileList.value = []
  
  if (successCount > 0) {
    ElMessage.success(`成功上传 ${successCount} 个文件`)
    fetchDocuments()
    fetchStatus()
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档 ${row.name} 吗？相关的向量数据也将被删除`,
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
</script>

<style scoped lang="scss">
.knowledge-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h2 {
    margin: 0;
  }
  
  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.status-card {
  margin-bottom: 20px;
}
</style>
