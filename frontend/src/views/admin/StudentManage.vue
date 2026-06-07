<template>
  <div class="student-manage">
    <div class="page-header">
      <h2>学生账号管理</h2>
      <div class="header-actions">
        <el-button @click="showOptionDialog = true">
          <el-icon><Setting /></el-icon>
          选项设置
        </el-button>
        <el-button type="primary" @click="showImportDialog = true">
          <el-icon><Upload /></el-icon>
          批量导入
        </el-button>
        <el-button @click="openAddDialog">
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
      <el-table-column prop="username" label="学号" width="130" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="college" label="学院" />
      <el-table-column prop="major" label="专业" />
      <el-table-column prop="grade" label="年级" width="80" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '冻结' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-dropdown trigger="click">
            <el-button size="small">
              操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleViewRecords(row)">
                  <el-icon><Document /></el-icon>查看记录
                </el-dropdown-item>
                <el-dropdown-item @click="handleResetPassword(row)">重置密码</el-dropdown-item>
                <el-dropdown-item @click="handleToggleStatus(row)">
                  {{ row.status === 1 ? '冻结' : '解冻' }}
                </el-dropdown-item>
                <el-dropdown-item @click="handleDelete(row)" style="color: #f56c6c">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
    
    <!-- 批量导入 -->
    <el-dialog v-model="showImportDialog" title="批量导入学生" width="500px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #title>
          请上传包含学号和姓名的Excel文件，格式：第1列学号，第2列姓名，第3列学院，第4列专业，第5列年级（第3-5列可选）
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
    
    <!-- 添加学生 -->
    <el-dialog v-model="showAddDialog" title="添加学生" width="400px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="学号">
          <el-input v-model="addForm.studentId" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="addForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="addForm.college" placeholder="请选择学院" clearable filterable allow-create style="width: 100%">
            <el-option v-for="c in collegeOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="addForm.major" placeholder="请选择专业" clearable filterable allow-create style="width: 100%">
            <el-option v-for="m in majorOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="addForm.grade" placeholder="请选择年级" clearable filterable allow-create style="width: 100%">
            <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确认</el-button>
      </template>
    </el-dialog>

    <!-- 选项设置 -->
    <el-dialog v-model="showOptionDialog" title="选项设置" width="600px">
      <el-tabs>
        <el-tab-pane label="学院">
          <div class="option-list">
            <div v-for="item in optionData.college || []" :key="item.id" class="option-item">
              <span>{{ item.optionValue }}</span>
              <el-button size="small" type="danger" link @click="handleDeleteOption(item.id, 'college')">删除</el-button>
            </div>
            <div v-if="!optionData.college || optionData.college.length === 0" class="option-empty">暂无选项</div>
            <div class="option-add">
              <el-input v-model="newOption.college" placeholder="输入新学院名称" size="small" style="width: 300px" />
              <el-button size="small" type="primary" @click="handleAddOption('college')">添加</el-button>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="专业">
          <div class="option-list">
            <div v-for="item in optionData.major || []" :key="item.id" class="option-item">
              <span>{{ item.optionValue }}</span>
              <el-button size="small" type="danger" link @click="handleDeleteOption(item.id, 'major')">删除</el-button>
            </div>
            <div v-if="!optionData.major || optionData.major.length === 0" class="option-empty">暂无选项</div>
            <div class="option-add">
              <el-input v-model="newOption.major" placeholder="输入新专业名称" size="small" style="width: 300px" />
              <el-button size="small" type="primary" @click="handleAddOption('major')">添加</el-button>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="年级">
          <div class="option-list">
            <div v-for="item in optionData.grade || []" :key="item.id" class="option-item">
              <span>{{ item.optionValue }}</span>
              <el-button size="small" type="danger" link @click="handleDeleteOption(item.id, 'grade')">删除</el-button>
            </div>
            <div v-if="!optionData.grade || optionData.grade.length === 0" class="option-empty">暂无选项</div>
            <div class="option-add">
              <el-input v-model="newOption.grade" placeholder="输入新年级" size="small" style="width: 300px" />
              <el-button size="small" type="primary" @click="handleAddOption('grade')">添加</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 编辑学生 -->
    <el-dialog v-model="showEditDialog" title="编辑学生信息" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="学号">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="editForm.college" placeholder="请选择学院" clearable filterable allow-create style="width: 100%">
            <el-option v-for="c in collegeOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="editForm.major" placeholder="请选择专业" clearable filterable allow-create style="width: 100%">
            <el-option v-for="m in majorOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="editForm.grade" placeholder="请选择年级" clearable filterable allow-create style="width: 100%">
            <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看提问记录 -->
    <el-dialog v-model="showRecordsDialog" :title="'提问记录 - ' + currentStudentName" width="700px">
      <div v-if="recordsLoading" style="text-align: center; padding: 40px">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p style="margin-top: 10px">加载中...</p>
      </div>
      <div v-else>
        <div v-if="questionRecords.length === 0" style="text-align: center; padding: 40px; color: #909399">
          <el-icon :size="48"><Document /></el-icon>
          <p style="margin-top: 10px">暂无提问记录</p>
        </div>
        <el-table v-else :data="questionRecords" stripe max-height="400">
          <el-table-column label="问题内容" min-width="300">
            <template #default="{ row }">
              <span>{{ row.question || row.QUESTION }}</span>
              <el-tag v-if="row.isRelated === false" type="danger" size="small" style="margin-left: 8px">无关内容</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="180">
            <template #default="{ row }">
              {{ formatRecordTime(row.create_time || row.CREATE_TIME) }}
            </template>
          </el-table-column>
          <el-table-column label="来源" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.source_type === 'web'" type="primary" size="small">网络</el-tag>
              <el-tag v-else type="success" size="small">教材</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="showRecordsDialog = false">关闭</el-button>
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
  updateStudent,
  deleteStudent,
  resetStudentPassword,
  toggleStudentStatus
} from '@/api/student'
import { getOptionsByCategory, addOption, deleteOption } from '@/api/option'
import { getUserQuestions } from '@/api/statistics'

const loading = ref(false)
const students = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')

const showImportDialog = ref(false)
const showAddDialog = ref(false)
const showOptionDialog = ref(false)
const importing = ref(false)
const uploadRef = ref(null)
const uploadFile = ref(null)

const collegeOptions = ref([])
const majorOptions = ref([])
const gradeOptions = ref([])
const optionData = reactive({ college: [], major: [], grade: [] })
const newOption = reactive({ college: '', major: '', grade: '' })

const addForm = reactive({
  studentId: '',
  name: '',
  college: '',
  major: '',
  grade: ''
})

const showEditDialog = ref(false)
const editForm = reactive({
  id: null,
  username: '',
  realName: '',
  college: '',
  major: '',
  grade: ''
})

// 查看提问记录
const showRecordsDialog = ref(false)
const recordsLoading = ref(false)
const questionRecords = ref([])
const currentStudentName = ref('')

onMounted(() => {
  fetchStudents()
  fetchOptions()
})

async function fetchOptions() {
  try {
    const [collegeRes, majorRes, gradeRes] = await Promise.all([
      getOptionsByCategory('college'),
      getOptionsByCategory('major'),
      getOptionsByCategory('grade')
    ])
    collegeOptions.value = (collegeRes.data || []).map(o => o.optionValue)
    majorOptions.value = (majorRes.data || []).map(o => o.optionValue)
    gradeOptions.value = (gradeRes.data || []).map(o => o.optionValue)
    optionData.college = collegeRes.data || []
    optionData.major = majorRes.data || []
    optionData.grade = gradeRes.data || []
  } catch (e) {
    console.error('获取选项失败:', e)
  }
}

function openAddDialog() {
  showAddDialog.value = true
}

async function fetchStudents() {
  loading.value = true
  try {
    const res = await getStudentList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    const data = res.data || {}
    students.value = data.records || []
    total.value = data.total || 0
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
    const data = res.data || res
    const success = data.success || 0
    const failed = data.failed || 0
    const total = data.total || 0
    if (success > 0) {
      ElMessage.success(`导入完成：共${total}行，成功${success}名，失败${failed}名`)
    } else {
      ElMessage.warning(`导入失败：共${total}行，全部失败`)
    }
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
    addForm.college = ''
    addForm.major = ''
    addForm.grade = ''
    fetchStudents()
  } catch (error) {
    console.error('添加失败:', error)
  }
}

function handleEdit(row) {
  editForm.id = row.id
  editForm.username = row.username
  editForm.realName = row.realName || ''
  editForm.college = row.college || ''
  editForm.major = row.major || ''
  editForm.grade = row.grade || ''
  showEditDialog.value = true
}

async function handleSaveEdit() {
  try {
    await updateStudent(editForm.id, {
      realName: editForm.realName,
      college: editForm.college,
      major: editForm.major,
      grade: editForm.grade
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    fetchStudents()
  } catch (error) {
    console.error('保存失败:', error)
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确定要重置学生 ${row.realName || row.username} 的密码吗？密码将被重置为学号后6位`,
      '提示',
      { type: 'warning' }
    )
    const res = await resetStudentPassword(row.id)
    const data = res.data || res
    ElMessage.success(`密码已重置为: ${data.newPassword}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置失败:', error)
    }
  }
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '冻结' : '解冻'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}学生 ${row.realName || row.username} 的账号吗？`,
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

async function handleViewRecords(row) {
  currentStudentName.value = row.realName || row.username
  showRecordsDialog.value = true
  recordsLoading.value = true
  questionRecords.value = []

  try {
    const res = await getUserQuestions(row.id, 20)
    questionRecords.value = res.data || []
  } catch (error) {
    console.error('获取提问记录失败:', error)
    ElMessage.error('获取提问记录失败')
  } finally {
    recordsLoading.value = false
  }
}

function formatRecordTime(date) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生 ${row.realName || row.username} 吗？此操作不可恢复`,
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

async function handleAddOption(category) {
  const value = newOption[category]
  if (!value || !value.trim()) {
    ElMessage.warning('请输入选项值')
    return
  }
  try {
    await addOption(category, value.trim())
    newOption[category] = ''
    fetchOptions()
    ElMessage.success('添加成功')
  } catch (e) {
    console.error('添加选项失败:', e)
  }
}

async function handleDeleteOption(id, category) {
  try {
    await ElMessageBox.confirm('确定删除此选项？', '提示', { type: 'warning' })
    await deleteOption(id)
    fetchOptions()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') console.error('删除选项失败:', e)
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

.option-list {
  .option-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;
    
    span {
      font-size: 14px;
    }
  }
  
  .option-empty {
    color: #999;
    text-align: center;
    padding: 20px;
  }
  
  .option-add {
    display: flex;
    gap: 10px;
    margin-top: 16px;
  }
}
</style>
