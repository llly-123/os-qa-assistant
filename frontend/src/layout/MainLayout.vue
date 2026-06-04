<template>
  <div class="main-layout">
    <el-container class="layout-container">
      <el-aside width="260px" class="sidebar">
        <div class="sidebar-header">
          <el-icon :size="28" color="#409eff"><Reading /></el-icon>
          <span class="title">OS AI助手</span>
        </div>
        
        <!-- 学生端：聊天侧边栏 -->
        <div v-if="role === 'STUDENT'" class="sidebar-content">
          <div class="new-chat-btn">
            <el-button type="primary" @click="createNewSession" style="width: 100%">
              <el-icon><Plus /></el-icon>
              新建对话
            </el-button>
          </div>
          
          <div class="session-list">
            <div 
              v-for="session in sessions" 
              :key="session.id"
              :class="['session-item', { active: currentSessionId === session.id }]"
              @click="selectSession(session.id)"
            >
              <el-icon><ChatDotRound /></el-icon>
              <div class="session-info">
                <span class="session-title">{{ session.title }}</span>
                <span class="session-time">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
              </div>
              <el-dropdown trigger="click" @command="handleSessionCommand($event, session)">
                <el-icon class="more-icon"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">重命名</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <div class="student-nav">
            <div 
              :class="['nav-item', { active: currentRoute === '/my-stats' }]"
              @click="router.push('/my-stats')"
            >
              <el-icon><DataAnalysis /></el-icon>
              <span>学习统计</span>
            </div>
          </div>
        </div>
        
        <!-- 教师端：管理导航 -->
        <div v-else class="sidebar-content">
          <div class="admin-nav">
            <div 
              :class="['nav-item', { active: currentRoute === '/admin/students' }]"
              @click="router.push('/admin/students')"
            >
              <el-icon><User /></el-icon>
              <span>学生管理</span>
            </div>
            <div 
              :class="['nav-item', { active: currentRoute === '/admin/knowledge' }]"
              @click="router.push('/admin/knowledge')"
            >
              <el-icon><Folder /></el-icon>
              <span>知识库管理</span>
            </div>
            <div 
              :class="['nav-item', { active: currentRoute === '/admin/statistics' }]"
              @click="router.push('/admin/statistics')"
            >
              <el-icon><DataAnalysis /></el-icon>
              <span>问答统计</span>
            </div>
          </div>
        </div>
        
        <div class="sidebar-footer">
          <div class="user-info">
            <el-avatar :size="32" :icon="role === 'TEACHER' ? 'UserFilled' : 'User'" />
            <div class="user-detail">
              <span class="username">{{ username }}</span>
              <el-tag :type="role === 'TEACHER' ? 'warning' : 'success'" size="small">
                {{ role === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </div>
          </div>
          
          <div class="action-btns">
            <el-button v-if="role === 'STUDENT'" link @click="showPhoneDialog = true">
              <el-icon><Phone /></el-icon>
              {{ userPhone ? '更换手机' : '绑定手机' }}
            </el-button>
            <el-button link @click="showPasswordDialog = true">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-button>
            <el-button link @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </div>
      </el-aside>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
    
    <el-dialog v-model="renameDialogVisible" title="重命名对话" width="400px">
      <el-input v-model="newTitle" placeholder="请输入新的对话标题" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPhoneDialog" title="手机号绑定" width="440px">
      <el-alert
        v-if="!userPhone"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #title>
          绑定手机号后可通过验证码找回密码，否则只能联系教师重置。
        </template>
      </el-alert>
      <div v-if="userPhone" style="margin-bottom: 16px">
        <el-tag type="success">已绑定：{{ maskedPhone }}</el-tag>
      </div>
      <el-form label-width="80px">
        <el-form-item label="手机号">
          <el-input v-model="phoneForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button v-if="userPhone" type="danger" @click="handleUnbindPhone">解绑</el-button>
        <el-button type="primary" @click="handleBindPhone">绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPasswordDialog" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { bindPhone, unbindPhone, changePassword } from '@/api/auth'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()

const renameDialogVisible = ref(false)
const newTitle = ref('')
const renamingSession = ref(null)
const showPhoneDialog = ref(false)
const phoneForm = reactive({ phone: '' })
const showPasswordDialog = ref(false)
const changingPassword = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const sessions = computed(() => chatStore.sessions)
const currentSessionId = computed(() => chatStore.currentSessionId)
const role = computed(() => userStore.role)
const username = computed(() => userStore.username)
const currentRoute = computed(() => route.path)
const userPhone = computed(() => userStore.userInfo?.phone || '')
const maskedPhone = computed(() => {
  const p = userPhone.value
  return p ? p.substring(0, 3) + '****' + p.substring(7) : ''
})

onMounted(async () => {
  if (role.value === 'STUDENT') {
    await chatStore.fetchSessions()
  }
})

async function createNewSession() {
  await chatStore.createSession()
}

async function selectSession(sessionId) {
  if (currentRoute.value !== '/chat') {
    router.push('/chat')
  }
  await chatStore.fetchMessages(sessionId)
}

function handleSessionCommand(command, session) {
  if (command === 'rename') {
    renamingSession.value = session
    newTitle.value = session.title
    renameDialogVisible.value = true
  } else if (command === 'delete') {
    ElMessageBox.confirm('确定要删除这个对话吗？', '提示', {
      type: 'warning'
    }).then(async () => {
      await chatStore.deleteSession(session.id)
      ElMessage.success('删除成功')
    }).catch(() => {})
  }
}

async function confirmRename() {
  if (!newTitle.value.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  await chatStore.updateSessionTitle(renamingSession.value.id, newTitle.value)
  renameDialogVisible.value = false
  ElMessage.success('重命名成功')
}

async function handleLogout() {
  try {
    await userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  } catch (error) {
    console.error('退出失败:', error)
  }
}

async function handleBindPhone() {
  if (!phoneForm.phone || !/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await bindPhone(phoneForm.phone)
    ElMessage.success('绑定成功')
    showPhoneDialog.value = false
    phoneForm.phone = ''
    await userStore.fetchUserInfo()
  } catch (error) {
    console.error('绑定失败:', error)
  }
}

async function handleUnbindPhone() {
  try {
    await ElMessageBox.confirm('解绑后将无法通过手机号找回密码，确定解绑？', '提示', { type: 'warning' })
    await unbindPhone()
    ElMessage.success('已解绑')
    showPhoneDialog.value = false
    await userStore.fetchUserInfo()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解绑失败:', error)
    }
  }
}

async function handleChangePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    showPasswordDialog.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    await userStore.logout()
    router.push('/login')
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    changingPassword.value = false
  }
}

function formatSessionTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

.layout-container {
  height: 100%;
}

.sidebar {
  background: #1e1e2e;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #313244;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #313244;
  
  .title {
    font-size: 18px;
    font-weight: 600;
    color: #cdd6f4;
  }
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.new-chat-btn {
  margin-bottom: 16px;
}

.admin-nav {
  .nav-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 16px;
    border-radius: 8px;
    cursor: pointer;
    color: #a6adc8;
    transition: all 0.2s;
    margin-bottom: 4px;
    font-size: 15px;
    
    &:hover {
      background: #313244;
      color: #cdd6f4;
    }
    
    &.active {
      background: #45475a;
      color: #cdd6f4;
    }
  }
}

.student-nav {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #313244;
  
  .nav-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    color: #a6adc8;
    transition: all 0.2s;
    font-size: 14px;
    
    &:hover {
      background: #313244;
      color: #cdd6f4;
    }
    
    &.active {
      background: #45475a;
      color: #cdd6f4;
    }
  }
}

.session-list {
  .session-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    color: #a6adc8;
    transition: all 0.2s;
    margin-bottom: 4px;
    
    &:hover {
      background: #313244;
      color: #cdd6f4;
    }
    
    &.active {
      background: #45475a;
      color: #cdd6f4;
    }
    
    .session-info {
      flex: 1;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    
    .session-title {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
    }
    
    .session-time {
      font-size: 11px;
      color: #6c7086;
    }
    
    .more-icon {
      opacity: 0;
      transition: opacity 0.2s;
    }
    
    &:hover .more-icon {
      opacity: 1;
    }
  }
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #313244;
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    
    .user-detail {
      display: flex;
      flex-direction: column;
      gap: 4px;
      
      .username {
        font-size: 14px;
        color: #cdd6f4;
      }
    }
  }
  
  .action-btns {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .el-button {
      justify-content: flex-start;
      color: #a6adc8;
      
      &:hover {
        color: #cdd6f4;
      }
    }
  }
}

.main-content {
  background: #f5f7fa;
  padding: 0;
}
</style>
