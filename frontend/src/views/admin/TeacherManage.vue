<template>
  <div class="teacher-manage">
    <div class="page-header">
      <h2>教师管理</h2>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索工号/姓名"
        clearable
        style="width: 260px"
        @clear="fetchTeachers"
        @keyup.enter="fetchTeachers"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="filterAuditStatus" placeholder="审核状态" clearable style="width: 140px" @change="fetchTeachers">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-button type="primary" @click="fetchTeachers">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table :data="teachers" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="username" label="工号" width="150" />
      <el-table-column prop="realName" label="姓名" width="130" />
      <el-table-column label="审核状态" width="110">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)">
            {{ auditText(row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="API体验" width="160">
        <template #default="{ row }">
          <el-tag v-if="isInTrial(row)" type="success" size="small">体验中</el-tag>
          <el-tag v-else-if="row.trialStartTime" type="info" size="small">未到/已过</el-tag>
          <el-tag v-else type="info" size="small">未开放</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="360" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.auditStatus === 0" size="small" type="success" @click="handleAudit(row, 1)">通过</el-button>
          <el-button v-if="row.auditStatus === 0" size="small" type="danger" @click="handleAudit(row, 2)">拒绝</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" @click="handleTrial(row)">体验设置</el-button>
          <el-button size="small" @click="handleResetPassword(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
        @size-change="fetchTeachers"
        @current-change="fetchTeachers"
      />
    </div>

    <!-- 体验时间段设置弹窗 -->
    <el-dialog v-model="trialDialogVisible" title="API 体验时间段设置" width="480px">
      <div style="margin-bottom: 16px">
        <span style="color: var(--color-text-secondary); font-size: 14px">
          教师：<strong>{{ trialForm.teacherName }}</strong>
        </span>
      </div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
        title="在体验时间段内，教师未配置自己的 API 时可使用管理员的默认 API。清除时间段则取消体验权限。"
      />
      <el-form label-width="100px">
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="trialForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="trialForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trialDialogVisible = false">取消</el-button>
        <el-button type="warning" plain @click="handleClearTrial">清除体验权限</el-button>
        <el-button type="primary" :loading="trialSaving" @click="handleSaveTrial">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getTeacherList, auditTeacher, toggleTeacherStatus, resetTeacherPassword, deleteTeacher, setTeacherTrial } from '@/api/teacher'

const loading = ref(false)
const teachers = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')
const filterAuditStatus = ref(null)

const trialDialogVisible = ref(false)
const trialSaving = ref(false)
const trialForm = reactive({
  teacherId: null,
  teacherName: '',
  startTime: '',
  endTime: ''
})

function auditTagType(status) {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

function auditText(status) {
  if (status === 0) return '待审核'
  if (status === 1) return '已通过'
  if (status === 2) return '已拒绝'
  return '未知'
}

function formatDate(time) {
  if (!time) return '-'
  return String(time).replace('T', ' ').substring(0, 19)
}

/** 判断教师当前是否在体验时间段内 */
function isInTrial(row) {
  if (!row.trialStartTime || !row.trialEndTime) return false
  const now = new Date()
  const start = new Date(row.trialStartTime)
  const end = new Date(row.trialEndTime)
  return now >= start && now <= end
}

async function fetchTeachers() {
  loading.value = true
  try {
    const res = await getTeacherList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      auditStatus: filterAuditStatus.value ?? undefined
    })
    const data = res.data || {}
    teachers.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  searchKeyword.value = ''
  filterAuditStatus.value = null
  currentPage.value = 1
  fetchTeachers()
}

async function handleAudit(row, auditStatus) {
  const action = auditStatus === 1 ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${action}教师「${row.realName || row.username}」的注册申请吗？`, '审核确认', { type: 'warning' })
    await auditTeacher(row.id, auditStatus)
    ElMessage.success(`已${action}`)
    fetchTeachers()
  } catch (e) {
    // 取消或失败
  }
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定${action}教师「${row.realName || row.username}」吗？`, '提示', { type: 'warning' })
    await toggleTeacherStatus(row.id, newStatus)
    ElMessage.success(`已${action}`)
    fetchTeachers()
  } catch (e) {
    // 取消或失败
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(`确定重置教师「${row.realName || row.username}」的密码吗？密码将重置为工号后6位。`, '提示', { type: 'warning' })
    const res = await resetTeacherPassword(row.id)
    ElMessage.success(res?.data?.message || '密码已重置')
  } catch (e) {
    // 取消或失败
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `<p>确定删除教师「${row.realName || row.username}」吗？</p>` +
      `<p style="color:#f56c6c;margin-top:8px">此操作会一并删除该教师名下的所有信息：</p>` +
      `<p style="margin:4px 0 0 16px">· 学生账号</p>` +
      `<p style="margin:4px 0 0 16px">· 班级及班级学生关联</p>` +
      `<p style="margin:4px 0 0 16px">· 知识库及知识文档</p>` +
      `<p style="margin:4px 0 0 16px">· 视频集及章节</p>` +
      `<p style="margin-top:12px;color:#909399">删除后不可恢复，请谨慎操作。</p>`,
      '删除教师确认',
      {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确认删除',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await deleteTeacher(row.id)
    ElMessage.success('已删除')
    fetchTeachers()
  } catch (e) {
    // 取消或失败
  }
}

function handleTrial(row) {
  trialForm.teacherId = row.id
  trialForm.teacherName = row.realName || row.username
  trialForm.startTime = row.trialStartTime ? row.trialStartTime.replace(' ', 'T') : ''
  trialForm.endTime = row.trialEndTime ? row.trialEndTime.replace(' ', 'T') : ''
  trialDialogVisible.value = true
}

async function handleSaveTrial() {
  if (!trialForm.startTime || !trialForm.endTime) {
    ElMessage.warning('请选择开始和结束时间')
    return
  }
  trialSaving.value = true
  try {
    await setTeacherTrial(trialForm.teacherId, trialForm.startTime, trialForm.endTime)
    ElMessage.success('体验时间段已设置')
    trialDialogVisible.value = false
    fetchTeachers()
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    trialSaving.value = false
  }
}

async function handleClearTrial() {
  try {
    await ElMessageBox.confirm('确定清除该教师的体验权限吗？', '提示', { type: 'warning' })
    await setTeacherTrial(trialForm.teacherId, '', '')
    ElMessage.success('体验权限已清除')
    trialDialogVisible.value = false
    fetchTeachers()
  } catch (e) {
    // 取消或失败
  }
}

onMounted(fetchTeachers)
</script>

<style scoped>
.teacher-manage {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
