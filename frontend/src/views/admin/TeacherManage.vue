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
      <el-table-column label="注册时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="300" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.auditStatus === 0" size="small" type="success" @click="handleAudit(row, 1)">通过</el-button>
          <el-button v-if="row.auditStatus === 0" size="small" type="danger" @click="handleAudit(row, 2)">拒绝</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getTeacherList, auditTeacher, toggleTeacherStatus, resetTeacherPassword, deleteTeacher } from '@/api/teacher'

const loading = ref(false)
const teachers = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')
const filterAuditStatus = ref(null)

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
