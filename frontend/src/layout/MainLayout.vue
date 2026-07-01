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
          <div class="student-top-nav">
            <div 
              :class="['nav-item', { active: currentRoute === '/my-stats' }]"
              @click="router.push('/my-stats')"
            >
              <el-icon><DataAnalysis /></el-icon>
              <span>学习统计</span>
            </div>
          </div>

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
        </div>
        
        <!-- 教师端：管理导航 -->
        <div v-else class="sidebar-content">
          <div class="admin-nav">
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
            <div 
              :class="['nav-item', { active: currentRoute === '/admin/videos' }]"
              @click="router.push('/admin/videos')"
            >
              <el-icon><VideoCamera /></el-icon>
              <span>视频管理</span>
            </div>
            <div 
              :class="['nav-item', { active: currentRoute === '/admin/students' }]"
              @click="router.push('/admin/students')"
            >
              <el-icon><User /></el-icon>
              <span>学生管理</span>
            </div>
            <div 
              :class="['nav-item', { active: currentRoute === '/admin/classes' }]"
              @click="router.push('/admin/classes')"
            >
              <el-icon><School /></el-icon>
              <span>班级管理</span>
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
            <div class="settings-toggle" @click="settingsOpen = !settingsOpen">
              <el-icon><Setting /></el-icon>
              <span>设置</span>
              <el-icon class="arrow" :class="{ open: settingsOpen }"><ArrowRight /></el-icon>
            </div>
            <div v-if="settingsOpen" class="settings-sub">
              <div class="settings-row" @click="showPhoneDialog = true">
                <el-icon><Phone /></el-icon>
                <span>{{ userPhone ? '更换手机' : '绑定手机' }}</span>
              </div>
              <div class="settings-row" @click="showPasswordDialog = true">
                <el-icon><Lock /></el-icon>
                <span>修改密码</span>
              </div>
              <div class="settings-row" @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </div>
            </div>
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

    <el-dialog v-model="showPhoneDialog" :title="userPhone ? '手机号管理' : '绑定手机号'" width="440px" @close="resetPhoneForm">
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

      <!-- 已绑定：显示当前手机号 + 更改手机号 -->
      <template v-if="userPhone">
        <div style="margin-bottom: 20px">
          <div style="color: #909399; font-size: 13px; margin-bottom: 6px">当前手机号</div>
          <el-tag type="success" size="large">{{ maskedPhone }}</el-tag>
        </div>

        <div class="change-phone-section">
          <div style="color: #606266; font-size: 14px; font-weight: 600; margin-bottom: 12px">更改手机号</div>
          
          <!-- 步骤1：发送验证码到原手机 -->
          <div v-if="changePhoneStep === 1">
            <div style="color: #909399; font-size: 13px; margin-bottom: 12px">
              点击发送验证码，验证码将发送至 {{ maskedPhone }}
            </div>
            <el-button 
              type="primary" 
              :loading="sendingCode" 
              :disabled="codeCountdown > 0"
              @click="handleSendChangeCode"
              style="width: 100%"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s 后重新发送` : '发送验证码' }}
            </el-button>
          </div>

          <!-- 步骤2：输入验证码和新手机号 -->
          <div v-else>
            <el-form label-width="80px">
              <el-form-item label="验证码">
                <div style="display: flex; gap: 8px; width: 100%">
                  <el-input v-model="changePhoneForm.code" placeholder="请输入验证码" maxlength="6" />
                  <el-button 
                    :loading="sendingCode" 
                    :disabled="codeCountdown > 0"
                    @click="handleSendChangeCode"
                  >
                    {{ codeCountdown > 0 ? `${codeCountdown}s` : '重新发送' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item label="新手机号">
                <el-input v-model="changePhoneForm.newPhone" placeholder="请输入新手机号" maxlength="11" />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </template>

      <!-- 未绑定：绑定手机号 -->
      <template v-else>
        <el-form label-width="80px">
          <el-form-item label="手机号">
            <el-input v-model="phoneForm.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button v-if="!userPhone" type="primary" @click="handleBindPhone">绑定</el-button>
        <el-button v-if="userPhone && changePhoneStep === 2" type="primary" :loading="changingPhone" @click="handleChangePhone">确认更改</el-button>
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
import { bindPhone, unbindPhone, changePhone, changePassword, sendPhoneCode } from '@/api/auth'
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
const changePhoneForm = reactive({ code: '', newPhone: '' })
const changePhoneStep = ref(1) // 1=发送验证码, 2=输入验证码和新手机号
const sendingCode = ref(false)
const codeCountdown = ref(0)
const changingPhone = ref(false)
const showPasswordDialog = ref(false)
const changingPassword = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const settingsOpen = ref(false)

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

async function handleSendChangeCode() {
  sendingCode.value = true
  try {
    const res = await sendPhoneCode(userPhone.value)
    if (res?.data?.devCode) {
      ElMessage.success(`验证码已发送（开发模式：${res.data.devCode}）`)
    } else {
      ElMessage.success('验证码已发送')
    }
    changePhoneStep.value = 2
    codeCountdown.value = 60
    const timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    sendingCode.value = false
  }
}

async function handleChangePhone() {
  if (!changePhoneForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (!changePhoneForm.newPhone || !/^1[3-9]\d{9}$/.test(changePhoneForm.newPhone)) {
    ElMessage.warning('请输入正确的新手机号')
    return
  }
  changingPhone.value = true
  try {
    await changePhone(changePhoneForm.code, changePhoneForm.newPhone)
    ElMessage.success('手机号更改成功')
    showPhoneDialog.value = false
    await userStore.fetchUserInfo()
  } catch (error) {
    console.error('更改手机号失败:', error)
  } finally {
    changingPhone.value = false
  }
}

function resetPhoneForm() {
  phoneForm.phone = ''
  changePhoneForm.code = ''
  changePhoneForm.newPhone = ''
  changePhoneStep.value = 1
  codeCountdown.value = 0
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

.student-top-nav {
  margin-bottom: 12px;
  
  .nav-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
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
      background: #409eff;
      color: #fff;
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
    
    .settings-toggle {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 4px;
      cursor: pointer;
      color: #a6adc8;
      font-size: 14px;
      transition: color 0.2s;
      
      &:hover {
        color: #cdd6f4;
      }
      
      span {
        flex: 1;
      }
      
      .arrow {
        transition: transform 0.2s;
        font-size: 12px;
        
        &.open {
          transform: rotate(90deg);
        }
      }
    }
    
    .settings-sub {
      padding-left: 24px;
      
      .settings-row {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 4px;
        cursor: pointer;
        color: #a6adc8;
        font-size: 13px;
        transition: color 0.2s;
        
        &:hover {
          color: #cdd6f4;
        }
      }
    }
    
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
