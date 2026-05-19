<template>
  <div class="student-manage">
    <div class="page-header">
      <h2>学生账号管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showImportDialog = true">
          <el-icon><Upload /></el-icon>
          批量导入
        </el-button>
        <el-button @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加学生
        </el-button>
      </div>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索学号/姓名"
        clearable
        style="width: 300px"
        @clear="fetchStudents"
        @keyup.enter="fetchStudents"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="fetchStudents">搜索</el-button>
    </div>
    
    <el-table
      :data="students"
      v-loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="studentId" label="学号" width="150" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ row.status === 'ACTIVE' ? '正常' : '冻结' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleResetPassword(row)">
            重置密码
          </el-button>
          <el-button 
            size="small" 
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '冻结' : '解冻' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchStudents"
        @current-change="fetchStudents"
      />
    </div>
    
    <el-dialog v-model="showImportDialog" title="批量导入学生" width="500px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #title>
          请上传包含学号和姓名的Excel文件，格式：第一列学号，第二列姓名
        </template>
      </el-alert>
      
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
        </template>
      </el-upload>
      
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">
          确认导入
        </el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="showAddDialog" title="添加学生" width="400px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="学号">
          <el-input v-model="addForm.studentId" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="addForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="addForm.email" placeholder="请输入邮箱（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getStudentList, 
  batchImportStudents, 
  createStudent,
  deleteStudent,
  resetStudentPassword,
  toggleStudentStatus
} from '@/api/student'

const loading = ref(false)
const students = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')

const showImportDialog = ref(false)
const showAddDialog = ref(false)
const importing = ref(false)
const uploadRef = ref(null)
const uploadFile = ref(null)

const addForm = reactive({
  studentId: '',
  name: '',
  email: ''
})

onMounted(() => {
  fetchStudents()
})

async function fetchStudents() {
  loading.value = true
  try {
    const res = await getStudentList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    students.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

function handleFileChange(file) {
  uploadFile.value = file.raw
}

async function handleImport() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  
  importing.value = true
  try {
    const res = await batchImportStudents(uploadFile.value)
    ElMessage.success(`成功导入 ${res.count} 名学生`)
    showImportDialog.value = false
    uploadFile.value = null
    fetchStudents()
  } catch (error) {
    console.error('导入失败:', error)
  } finally {
    importing.value = false
  }
}

async function handleAdd() {
  if (!addForm.studentId || !addForm.name) {
    ElMessage.warning('请填写学号和姓名')
    return
  }
  
  try {
    await createStudent(addForm)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    addForm.studentId = ''
    addForm.name = ''
    addForm.email = ''
    fetchStudents()
  } catch (error) {
    console.error('添加失败:', error)
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确定要重置学生 ${row.name} 的密码吗？密码将被重置为学号后6位`,
      '提示',
      { type: 'warning' }
    )
    const res = await resetStudentPassword(row.id)
    ElMessage.success(`密码已重置为: ${res.newPassword}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置失败:', error)
    }
  }
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'FROZEN' : 'ACTIVE'
  const action = newStatus === 'FROZEN' ? '冻结' : '解冻'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}学生 ${row.name} 的账号吗？`,
      '提示',
      { type: 'warning' }
    )
    await toggleStudentStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchStudents()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生 ${row.name} 吗？此操作不可恢复`,
      '警告',
      { type: 'error' }
    )
    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    fetchStudents()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}
</script>

<style scoped lang="scss">
.student-manage {
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

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
