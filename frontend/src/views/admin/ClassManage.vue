<template>
  <div class="class-manage">
    <!-- 班级列表视图 -->
    <template v-if="!selectedClass">
      <div class="page-header">
        <h2>班级管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon> 创建班级
        </el-button>
      </div>

      <div class="class-list">
        <el-card v-for="cls in classes" :key="cls.id" class="class-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="class-info">
                <span class="class-name">{{ cls.name }}</span>
                <el-tag :type="cls.status === 1 ? 'success' : 'info'" size="small">
                  {{ cls.status === 1 ? '活跃' : '已解散' }}
                </el-tag>
                <el-tag type="warning" size="small">{{ cls.studentCount || 0 }} 人</el-tag>
              </div>
              <div class="card-actions">
                <el-button size="small" type="primary" @click="enterClass(cls)">进入管理</el-button>
                <el-button size="small" :type="(!cls.videoSetId && !cls.kbId) ? 'primary' : 'default'" @click="openResourceDialog(cls)">配置资源</el-button>
                <el-button v-if="cls.status === 1" size="small" type="warning" @click="handleDissolve(cls)">解散</el-button>
                <el-button size="small" type="danger" @click="handleDeleteClass(cls)">删除</el-button>
              </div>
            </div>
          </template>
          <div class="class-time">
            <div><el-icon><Calendar /></el-icon> 开班：{{ formatTime(cls.startTime) }}</div>
            <div><el-icon><Calendar /></el-icon> 结班：{{ formatTime(cls.endTime) }}</div>
          </div>
          <div class="class-mounts">
            <el-tag v-if="cls.videoSetName" type="primary" size="small" effect="plain">🎬 {{ cls.videoSetName }}</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">🎬 未挂载视频集</el-tag>
            <el-tag v-if="cls.kbName" type="success" size="small" effect="plain">📚 {{ cls.kbName }}</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">📚 未挂载知识库</el-tag>
          </div>
        </el-card>

        <el-empty v-if="classes.length === 0" description="暂无班级，请创建" />
      </div>
    </template>

    <!-- 班级内学生管理视图 -->
    <template v-else>
      <div class="page-header">
        <div class="header-left">
          <el-button @click="backToClassList" link>
            <el-icon><ArrowLeft /></el-icon> 返回班级列表
          </el-button>
          <h2>{{ selectedClass.name }} - 学生管理</h2>
        </div>
        <div class="header-actions">
          <el-button size="small" link type="info" @click="showOptionDialog = true">
            <el-icon><Setting /></el-icon> 选项设置
          </el-button>
          <el-button @click="showImportDialog = true">
            <el-icon><Upload /></el-icon> 批量导入
          </el-button>
          <el-button @click="openSelectStudentDialog">
            <el-icon><UserFilled /></el-icon> 从已有学生选择
          </el-button>
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon> 添加学生
          </el-button>
        </div>
      </div>

      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索学号/姓名"
          clearable
          style="width: 260px"
          @clear="filterStudents"
          @keyup.enter="filterStudents"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterCollege" placeholder="学院" clearable style="width: 180px" @change="filterStudents">
          <el-option v-for="c in collegeOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px" @change="filterStudents">
          <el-option label="正常" :value="1" />
          <el-option label="冻结" :value="0" />
        </el-select>
        <el-button type="primary" @click="filterStudents">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="filteredStudents" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="学号" width="130" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="college" label="学院" />
        <el-table-column prop="major" label="专业" />
        <el-table-column prop="grade" label="年级" width="80" />
        <el-table-column prop="createTime" label="加入时间" width="180">
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
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
            <el-tooltip content="编辑" placement="top">
              <el-button size="small" circle @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="查看记录" placement="top">
              <el-button size="small" circle @click="handleViewRecords(row)">
                <el-icon><Document /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="row.status === 1 ? '冻结' : '解冻'" placement="top">
              <el-button size="small" circle @click="handleToggleStatus(row)">
                <el-icon><Lock /></el-icon>
              </el-button>
            </el-tooltip>
            <el-dropdown trigger="click">
              <el-button size="small" circle>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleResetPassword(row)">重置密码</el-dropdown-item>
                  <el-dropdown-item @click="handleRemoveFromClass(row)" style="color: #e6a23c">移出班级</el-dropdown-item>
                  <el-dropdown-item @click="handleDelete(row)" style="color: #f56c6c">彻底删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 批量导入 -->
      <el-dialog v-model="showImportDialog" title="批量导入学生" width="500px">
        <el-alert type="info" :closable="false" style="margin-bottom: 20px">
          <template #title>
            请上传包含学号和姓名的Excel文件，格式：第1列学号，第2列姓名，第3列学院，第4列专业，第5列年级（第3-5列可选）
          </template>
        </el-alert>
        <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
          <el-button size="small" type="primary" link @click="downloadTemplate">
            <el-icon><Download /></el-icon> 下载导入模板
          </el-button>
        </div>
        <el-upload ref="uploadRef" :auto-upload="false" :limit="1" accept=".xlsx,.xls" :on-change="handleFileChange" drag>
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
          </template>
        </el-upload>
        <template #footer>
          <el-button @click="showImportDialog = false">取消</el-button>
          <el-button type="primary" :loading="importing" @click="handleImport">确认导入</el-button>
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
                <el-tag v-if="(row.sourceType || row.SOURCE_TYPE || row.source_type) === 'web'" type="primary" size="small">网络</el-tag>
                <el-tag v-else-if="(row.sourceType || row.SOURCE_TYPE || row.source_type) === 'no_class'" type="warning" size="small">未进班级</el-tag>
                <el-tag v-else type="success" size="small">教材</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <template #footer>
          <el-button @click="showRecordsDialog = false">关闭</el-button>
        </template>
      </el-dialog>
    </template>

    <!-- 创建班级对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建班级" width="460px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="班级名称">
          <el-input v-model="createForm.name" placeholder="如：2025春季班" />
        </el-form-item>
        <el-form-item label="视频集">
          <el-select v-model="createForm.videoSetId" placeholder="选择视频集（可空）" clearable style="width: 100%">
            <el-option v-for="vs in videoSets" :key="vs.id" :label="vs.name" :value="vs.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库">
          <el-select v-model="createForm.kbId" placeholder="选择知识库（可空）" clearable style="width: 100%">
            <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开班时间">
          <el-date-picker
            v-model="createForm.startTime"
            type="datetime"
            placeholder="选择开班时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结班时间">
          <el-date-picker
            v-model="createForm.endTime"
            type="datetime"
            placeholder="选择结班时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <div class="create-tip">提示：视频集与知识库可在“视频管理”“知识库管理”中预先配置。</div>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 配置资源（挂载/修改/取消挂载视频集与知识库） -->
    <el-dialog v-model="showResourceDialog" :title="'配置资源 - ' + (resourceTarget?.name || '')" width="460px">
      <el-form :model="resourceForm" label-width="80px">
        <el-form-item label="视频集">
          <el-select v-model="resourceForm.videoSetId" placeholder="选择视频集（可空）" clearable style="width: 100%">
            <el-option v-for="vs in videoSets" :key="vs.id" :label="vs.name" :value="vs.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库">
          <el-select v-model="resourceForm.kbId" placeholder="选择知识库（可空）" clearable style="width: 100%">
            <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
        </el-form-item>
        <div class="create-tip">提示：清空选择即取消挂载；视频集与知识库可在“视频管理”“知识库管理”中预先配置。</div>
      </el-form>
      <template #footer>
        <el-button @click="showResourceDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingResource" @click="handleSaveResource">保存</el-button>
      </template>
    </el-dialog>

    <!-- 从已有学生选择 -->
    <el-dialog v-model="showSelectStudentDialog" :title="'从已有学生选择 - ' + (selectedClass?.name || '')" width="700px">
      <el-input
        v-model="selectStudentKeyword"
        placeholder="搜索学号/姓名"
        clearable
        style="margin-bottom: 16px"
      />
      <el-table
        ref="selectStudentTableRef"
        :data="filteredAllStudents"
        max-height="400"
        @selection-change="handleSelectStudentChange"
        stripe
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="username" label="学号" width="130" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="college" label="学院" />
        <el-table-column prop="major" label="专业" />
        <el-table-column prop="grade" label="年级" width="80" />
      </el-table>
      <div v-if="selectedStudentIds.length > 0" class="select-count">
        已选 {{ selectedStudentIds.length }} 名学生
      </div>
      <template #footer>
        <el-button @click="showSelectStudentDialog = false">取消</el-button>
        <el-button type="primary" :loading="addingStudents" @click="handleSelectStudentAdd">
          添加到班级
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Calendar, ArrowLeft, Search, Upload, Setting, Download, Document, UploadFilled, Loading, Edit, Lock, MoreFilled, UserFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { getClasses, createClass, deleteClass, dissolveClass, getClassStudents, removeStudent, createStudentInClass, importStudentsInClass, updateClassResources, addStudent } from '@/api/clazz'
import { updateStudent, deleteStudent, resetStudentPassword, toggleStudentStatus, getAllStudents } from '@/api/student'
import { getOptionsByCategory, addOption, deleteOption } from '@/api/option'
import { getUserQuestions } from '@/api/statistics'
import { getVideoSets } from '@/api/video'
import { getKnowledgeBases } from '@/api/knowledge'

// 班级列表
const classes = ref([])
const selectedClass = ref(null)
const showCreateDialog = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '', startTime: '', endTime: '', videoSetId: null, kbId: null })

// 配置资源对话框
const showResourceDialog = ref(false)
const savingResource = ref(false)
const resourceTarget = ref(null)
const resourceForm = reactive({ videoSetId: null, kbId: null })

// 可挂载的视频集 / 知识库
const videoSets = ref([])
const knowledgeBases = ref([])

// 学生列表
const loading = ref(false)
const students = ref([])
const searchKeyword = ref('')
const filterCollege = ref(null)
const filterStatus = ref(null)
const filteredStudents = computed(() => {
  let list = students.value
  const kw = searchKeyword.value?.toLowerCase()
  if (kw) list = list.filter(s => (s.username||'').toLowerCase().includes(kw) || (s.realName||'').toLowerCase().includes(kw))
  if (filterCollege.value) list = list.filter(s => s.college === filterCollege.value)
  if (filterStatus.value !== null && filterStatus.value !== '') list = list.filter(s => s.status === filterStatus.value)
  return list
})

// 对话框
const showImportDialog = ref(false)
const showAddDialog = ref(false)
const showOptionDialog = ref(false)
const showEditDialog = ref(false)
const showRecordsDialog = ref(false)
const showSelectStudentDialog = ref(false)
const importing = ref(false)
const uploadRef = ref(null)
const uploadFile = ref(null)

// 从已有学生选择
const allStudents = ref([])
const selectStudentKeyword = ref('')
const selectedStudentIds = ref([])
const addingStudents = ref(false)
const filteredAllStudents = computed(() => {
  const kw = selectStudentKeyword.value?.toLowerCase()
  if (!kw) return allStudents.value
  return allStudents.value.filter(s =>
    (s.username || '').toLowerCase().includes(kw) ||
    (s.realName || '').toLowerCase().includes(kw)
  )
})

// 选项
const collegeOptions = ref([])
const majorOptions = ref([])
const gradeOptions = ref([])
const optionData = reactive({ college: [], major: [], grade: [] })
const newOption = reactive({ college: '', major: '', grade: '' })

// 添加/编辑表单
const addForm = reactive({ studentId: '', name: '', college: '', major: '', grade: '' })
const editForm = reactive({ id: null, username: '', realName: '', college: '', major: '', grade: '' })

// 提问记录
const recordsLoading = ref(false)
const questionRecords = ref([])
const currentStudentName = ref('')

onMounted(() => {
  fetchClasses()
  fetchOptions()
  fetchVideoSets()
  fetchKnowledgeBases()
})

async function fetchVideoSets() {
  try {
    const res = await getVideoSets()
    videoSets.value = res.data || []
  } catch (e) { console.error('获取视频集失败:', e) }
}

async function fetchKnowledgeBases() {
  try {
    const res = await getKnowledgeBases()
    knowledgeBases.value = res.data || []
  } catch (e) { console.error('获取知识库失败:', e) }
}

// ===== 班级列表 =====
async function fetchClasses() {
  try {
    const res = await getClasses()
    classes.value = res.data || []
  } catch (e) {
    console.error('获取班级列表失败:', e)
  }
}

async function handleCreate() {
  if (!createForm.name.trim()) { ElMessage.warning('请输入班级名称'); return }
  if (!createForm.startTime || !createForm.endTime) { ElMessage.warning('请选择起止时间'); return }
  creating.value = true
  try {
    await createClass(createForm.name, createForm.startTime, createForm.endTime, createForm.videoSetId, createForm.kbId)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.name = ''
    createForm.startTime = ''
    createForm.endTime = ''
    createForm.videoSetId = null
    createForm.kbId = null
    fetchClasses()
  } catch (e) {
    console.error('创建失败:', e)
  } finally {
    creating.value = false
  }
}

// ===== 配置资源（挂载/修改/取消挂载视频集与知识库）=====
function openResourceDialog(cls) {
  resourceTarget.value = cls
  resourceForm.videoSetId = cls.videoSetId ?? null
  resourceForm.kbId = cls.kbId ?? null
  showResourceDialog.value = true
}

async function handleSaveResource() {
  if (!resourceTarget.value) return
  savingResource.value = true
  try {
    await updateClassResources(resourceTarget.value.id, resourceForm.videoSetId, resourceForm.kbId)
    ElMessage.success('资源配置已保存')
    showResourceDialog.value = false
    fetchClasses()
  } catch (e) {
    console.error('资源配置失败:', e)
  } finally {
    savingResource.value = false
  }
}

async function handleDeleteClass(cls) {
  try {
    await ElMessageBox.confirm(`确定删除班级"${cls.name}"吗？此操作不可恢复。`, '提示', { type: 'warning' })
    await deleteClass(cls.id)
    ElMessage.success('已删除')
    fetchClasses()
  } catch (e) {
    if (e !== 'cancel') console.error('删除失败:', e)
  }
}

async function handleDissolve(cls) {
  try {
    await ElMessageBox.confirm(`确定解散班级"${cls.name}"吗？所有学生将被移出。`, '提示', { type: 'warning' })
    await dissolveClass(cls.id)
    ElMessage.success('已解散')
    fetchClasses()
  } catch (e) {
    if (e !== 'cancel') console.error('解散失败:', e)
  }
}

// ===== 班级内学生管理 =====
function enterClass(cls) {
  selectedClass.value = cls
  fetchStudents(cls.id)
}

function backToClassList() {
  selectedClass.value = null
  students.value = []
  fetchClasses()
}

async function fetchStudents(classId) {
  loading.value = true
  try {
    const res = await getClassStudents(classId)
    students.value = res.data || []
  } catch (e) {
    console.error('获取学生列表失败:', e)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  searchKeyword.value = ''
  filterCollege.value = null
  filterStatus.value = null
}

function filterStudents() {
  // computed 自动处理
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

// ===== 添加学生 =====
function openAddDialog() {
  showAddDialog.value = true
}

async function handleAdd() {
  if (!addForm.studentId || !addForm.name) { ElMessage.warning('请填写学号和姓名'); return }
  try {
    await createStudentInClass(selectedClass.value.id, { ...addForm })
    ElMessage.success('添加成功')
    showAddDialog.value = false
    addForm.studentId = ''
    addForm.name = ''
    addForm.college = ''
    addForm.major = ''
    addForm.grade = ''
    fetchStudents(selectedClass.value.id)
    fetchClasses()
  } catch (e) {
    console.error('添加失败:', e)
  }
}

// ===== 从已有学生选择 =====
async function openSelectStudentDialog() {
  showSelectStudentDialog.value = true
  selectStudentKeyword.value = ''
  selectedStudentIds.value = []
  try {
    const res = await getAllStudents()
    // 排除已在当前班级的学生
    const existingIds = new Set(students.value.map(s => s.id))
    allStudents.value = (res.data || []).filter(s => !existingIds.has(s.id))
  } catch (e) {
    console.error('获取学生列表失败:', e)
  }
}

function handleSelectStudentChange(selection) {
  selectedStudentIds.value = selection
}

async function handleSelectStudentAdd() {
  if (selectedStudentIds.value.length === 0) {
    ElMessage.warning('请至少选择一名学生')
    return
  }
  addingStudents.value = true
  let successCount = 0
  let failCount = 0
  for (const student of selectedStudentIds.value) {
    try {
      await addStudent(selectedClass.value.id, student.id)
      successCount++
    } catch (e) {
      failCount++
    }
  }
  addingStudents.value = false
  if (successCount > 0) {
    ElMessage.success(`成功添加 ${successCount} 名学生${failCount > 0 ? `，${failCount} 名失败` : ''}`)
    showSelectStudentDialog.value = false
    fetchStudents(selectedClass.value.id)
    fetchClasses()
  } else {
    ElMessage.error('添加失败，学生可能已在班级中')
  }
}

// ===== 编辑学生 =====
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
    fetchStudents(selectedClass.value.id)
  } catch (e) {
    console.error('保存失败:', e)
  }
}

// ===== 重置密码 =====
async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(`确定要重置学生 ${row.realName || row.username} 的密码吗？密码将被重置为学号后6位`, '提示', { type: 'warning' })
    const res = await resetStudentPassword(row.id)
    const data = res.data || res
    ElMessage.success(`密码已重置为: ${data.newPassword}`)
  } catch (e) {
    if (e !== 'cancel') console.error('重置失败:', e)
  }
}

// ===== 冻结/解冻 =====
async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(`确定要${action}学生 ${row.realName || row.username} 的账号吗？`, '提示', { type: 'warning' })
    await toggleStudentStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchStudents(selectedClass.value.id)
  } catch (e) {
    if (e !== 'cancel') console.error('操作失败:', e)
  }
}

// ===== 移出班级 =====
async function handleRemoveFromClass(row) {
  try {
    await ElMessageBox.confirm(`确定将学生 ${row.realName || row.username} 移出班级吗？学生账号仍保留。`, '提示', { type: 'warning' })
    await removeStudent(selectedClass.value.id, row.id)
    ElMessage.success('已移出班级')
    fetchStudents(selectedClass.value.id)
    fetchClasses()
  } catch (e) {
    if (e !== 'cancel') console.error('移出失败:', e)
  }
}

// ===== 彻底删除 =====
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要彻底删除学生 ${row.realName || row.username} 吗？此操作不可恢复`, '警告', { type: 'error' })
    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    fetchStudents(selectedClass.value.id)
    fetchClasses()
  } catch (e) {
    if (e !== 'cancel') console.error('删除失败:', e)
  }
}

// ===== 查看提问记录 =====
async function handleViewRecords(row) {
  currentStudentName.value = row.realName || row.username
  showRecordsDialog.value = true
  recordsLoading.value = true
  questionRecords.value = []
  try {
    const res = await getUserQuestions(row.id, 20, selectedClass.value.id)
    questionRecords.value = res.data || []
  } catch (e) {
    console.error('获取提问记录失败:', e)
    ElMessage.error('获取提问记录失败')
  } finally {
    recordsLoading.value = false
  }
}

function formatRecordTime(date) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

// ===== 批量导入 =====
function handleFileChange(file) {
  uploadFile.value = file.raw
}

async function handleImport() {
  if (!uploadFile.value) { ElMessage.warning('请选择文件'); return }
  importing.value = true
  try {
    const res = await importStudentsInClass(selectedClass.value.id, uploadFile.value)
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
    fetchStudents(selectedClass.value.id)
    fetchClasses()
  } catch (e) {
    console.error('导入失败:', e)
  } finally {
    importing.value = false
  }
}

function downloadTemplate() {
  const headers = ['学号', '姓名', '学院', '专业', '年级']
  const ws = XLSX.utils.aoa_to_sheet([headers])
  ws['!cols'] = [{ wch: 15 }, { wch: 12 }, { wch: 15 }, { wch: 20 }, { wch: 10 }]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '学生导入')
  XLSX.writeFile(wb, '学生导入模板.xlsx')
  ElMessage.success('模板下载成功')
}

// ===== 选项管理 =====
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

async function handleAddOption(category) {
  const value = newOption[category]
  if (!value || !value.trim()) { ElMessage.warning('请输入选项值'); return }
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

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped lang="scss">
.class-manage {
  padding: 28px 32px;
  max-width: 1100px;
  margin: 0 auto;
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
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.class-card { margin-bottom: 16px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .class-info {
    display: flex;
    align-items: center;
    gap: 8px;
    .class-name { font-size: 16px; font-weight: 600; }
  }
}

.class-time {
  display: flex;
  gap: 24px;
  color: #606266;
  font-size: 14px;
  div { display: flex; align-items: center; gap: 4px; }
}

.class-mounts {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.create-tip {
  font-size: 12px;
  color: var(--color-text-tertiary, #909399);
  margin-top: -4px;
  margin-bottom: 8px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.action-btns {
  display: flex;
  gap: 4px;
  align-items: center;
}

.option-list {
  .option-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;
    span { font-size: 14px; }
  }
  .option-empty { color: #999; text-align: center; padding: 20px; }
  .option-add { display: flex; gap: 10px; margin-top: 16px; }
}

.select-count {
  margin-top: 12px;
  font-size: 13px;
  color: #6366f1;
  font-weight: 600;
}
</style>
