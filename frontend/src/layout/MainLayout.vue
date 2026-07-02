<template>
  <div class="main-layout">
    <el-container class="layout-container">
      <!-- Sidebar -->
      <el-aside :width="sidebarCollapsed ? '72px' : '260px'" class="sidebar">
        <!-- Logo -->
        <div class="sidebar-header" @click="sidebarCollapsed = !sidebarCollapsed">
          <div class="logo-icon">
            <svg viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg" width="36" height="36">
              <rect width="36" height="36" rx="10" fill="url(#logo-grad)"/>
              <path d="M10 12h16M10 18h10M10 24h16" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
              <defs><linearGradient id="logo-grad" x1="0" y1="0" x2="36" y2="36"><stop stop-color="#6366f1"/><stop offset="1" stop-color="#4f46e5"/></linearGradient></defs>
            </svg>
          </div>
          <transition name="fade">
            <span v-if="!sidebarCollapsed" class="logo-text">OS AI 助手</span>
          </transition>
        </div>

        <!-- Student Navigation -->
        <div v-if="role === 'STUDENT'" class="sidebar-content">
          <div class="nav-section">
            <div class="new-chat-btn-wrap">
              <el-button
                type="primary"
                @click="createNewSession"
                :class="['new-chat-btn', { collapsed: sidebarCollapsed }]"
              >
                <el-icon><Plus /></el-icon>
                <span v-if="!sidebarCollapsed">新建对话</span>
              </el-button>
            </div>
          </div>

          <div class="nav-section">
            <div
              :class="['nav-item', { active: currentRoute === '/my-stats' }]"
              @click="router.push('/my-stats')"
              :title="sidebarCollapsed ? '学习统计' : ''"
            >
              <el-icon><DataAnalysis /></el-icon>
              <span v-if="!sidebarCollapsed">学习统计</span>
            </div>
          </div>

          <div v-if="!sidebarCollapsed" class="nav-section">
            <div class="section-label">对话历史</div>
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
                      <el-dropdown-item command="rename">
                        <el-icon><Edit /></el-icon> 重命名
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <el-icon><Delete /></el-icon> 删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div v-if="sessions.length === 0" class="empty-sessions">
                暂无对话记录
              </div>
            </div>
          </div>
        </div>

        <!-- Teacher Navigation -->
        <div v-else class="sidebar-content">
          <div class="nav-section">
            <div class="section-label" v-if="!sidebarCollapsed">管理功能</div>
            <div
              :class="['nav-item', { active: currentRoute === '/admin/knowledge' }]"
              @click="router.push('/admin/knowledge')"
              :title="sidebarCollapsed ? '知识库管理' : ''"
            >
              <el-icon><Folder /></el-icon>
              <span v-if="!sidebarCollapsed">知识库管理</span>
            </div>
            <div
              :class="['nav-item', { active: currentRoute === '/admin/statistics' }]"
              @click="router.push('/admin/statistics')"
              :title="sidebarCollapsed ? '问答统计' : ''"
            >
              <el-icon><DataAnalysis /></el-icon>
              <span v-if="!sidebarCollapsed">问答统计</span>
            </div>
            <div
              :class="['nav-item', { active: currentRoute === '/admin/videos' }]"
              @click="router.push('/admin/videos')"
              :title="sidebarCollapsed ? '视频管理' : ''"
            >
              <el-icon><VideoCamera /></el-icon>
              <span v-if="!sidebarCollapsed">视频管理</span>
            </div>
            <div
              :class="['nav-item', { active: currentRoute === '/admin/students' }]"
              @click="router.push('/admin/students')"
              :title="sidebarCollapsed ? '学生管理' : ''"
            >
              <el-icon><User /></el-icon>
              <span v-if="!sidebarCollapsed">学生管理</span>
            </div>
            <div
              :class="['nav-item', { active: currentRoute === '/admin/classes' }]"
              @click="router.push('/admin/classes')"
              :title="sidebarCollapsed ? '班级管理' : ''"
            >
              <el-icon><School /></el-icon>
              <span v-if="!sidebarCollapsed">班级管理</span>
            </div>
          </div>
        </div>

        <!-- User Footer -->
        <div class="sidebar-footer" :class="{ collapsed: sidebarCollapsed }">
          <div class="user-bar">
            <el-avatar :size="32" :style="{ background: 'var(--color-primary)' }">
              {{ username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <div v-if="!sidebarCollapsed" class="user-detail">
              <span class="username-text">{{ username }}</span>
              <el-tag :type="role === 'TEACHER' ? 'warning' : 'success'" size="small">
                {{ role === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </div>
          </div>

          <div v-if="!sidebarCollapsed" class="footer-actions">
            <div class="action-item" @click="settingsOpen = !settingsOpen">
              <el-icon><Setting /></el-icon>
              <span>设置</span>
              <el-icon class="arrow" :class="{ open: settingsOpen }"><ArrowRight /></el-icon>
            </div>
            <div v-if="settingsOpen" class="action-sub">
              <div class="sub-item" @click="showPhoneDialog = true">
                <el-icon><Phone /></el-icon>
                <span>{{ userPhone ? '更换手机' : '绑定手机' }}</span>
              </div>
              <div class="sub-item" @click="showPasswordDialog = true">
                <el-icon><Lock /></el-icon>
                <span>修改密码</span>
              </div>
              <div class="sub-item logout" @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </div>
            </div>
          </div>
        </div>
      </el-aside>

      <!-- Main Content -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <!-- Rename Dialog -->
    <el-dialog v-model="renameDialogVisible" title="重命名对话" width="400px">
      <el-input v-model="newTitle" placeholder="请输入新的对话标题" maxlength="50" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确认</el-button>
      </template>
    </el-dialog>

    <!-- Phone Dialog -->
    <el-dialog v-model="showPhoneDialog" :title="userPhone ? '手机号管理' : '绑定手机号'" width="440px" @close="resetPhoneForm">
      <el-alert v-if="!userPhone" type="info" :closable="false" style="margin-bottom: 18px">
        <template #title>绑定手机号后可通过验证码找回密码</template>
      </el-alert>

      <template v-if="userPhone">
        <div style="margin-bottom: 18px">
          <div style="color: var(--color-text-tertiary); font-size:13px; margin-bottom:6px">当前手机号</div>
          <el-tag type="success" size="large">{{ maskedPhone }}</el-tag>
        </div>
        <div>
          <div style="font-weight:600; margin-bottom:12px; font-size:14px">更改手机号</div>
          <div v-if="changePhoneStep === 1">
            <div style="color: var(--color-text-tertiary); font-size:13px; margin-bottom:12px">
              验证码将发送至 {{ maskedPhone }}
            </div>
            <el-button
              type="primary"
              :loading="sendingCode"
              :disabled="codeCountdown > 0"
              @click="handleSendChangeCode"
              style="width:100%"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s 后重新发送` : '发送验证码' }}
            </el-button>
          </div>
          <div v-else>
            <el-form label-width="80px">
              <el-form-item label="验证码">
                <div style="display:flex; gap:8px; width:100%">
                  <el-input v-model="changePhoneForm.code" placeholder="请输入验证码" maxlength="6" />
                  <el-button :loading="sendingCode" :disabled="codeCountdown > 0" @click="handleSendChangeCode">
                    {{ codeCountdown > 0 ? `${codeCountdown}s` : '重发' }}
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

    <!-- Password Dialog -->
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
import { bindPhone, changePhone, changePassword, sendPhoneCode } from '@/api/auth'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()

const sidebarCollapsed = ref(false)
const settingsOpen = ref(false)
const renameDialogVisible = ref(false)
const newTitle = ref('')
const renamingSession = ref(null)

const showPhoneDialog = ref(false)
const phoneForm = reactive({ phone: '' })
const changePhoneForm = reactive({ code: '', newPhone: '' })
const changePhoneStep = ref(1)
const sendingCode = ref(false)
const codeCountdown = ref(0)
const changingPhone = ref(false)

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
  if (currentRoute.value !== '/chat') router.push('/chat')
  await chatStore.fetchMessages(sessionId)
}

function handleSessionCommand(command, session) {
  if (command === 'rename') {
    renamingSession.value = session
    newTitle.value = session.title
    renameDialogVisible.value = true
  } else if (command === 'delete') {
    ElMessageBox.confirm('确定要删除这个对话吗？', '提示', { type: 'warning' })
      .then(async () => {
        await chatStore.deleteSession(session.id)
        ElMessage.success('删除成功')
      }).catch(() => {})
  }
}

async function confirmRename() {
  if (!newTitle.value.trim()) { ElMessage.warning('标题不能为空'); return }
  await chatStore.updateSessionTitle(renamingSession.value.id, newTitle.value)
  renameDialogVisible.value = false
  ElMessage.success('重命名成功')
}

async function handleLogout() {
  try {
    await userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  } catch (error) { console.error('退出失败:', error) }
}

async function handleBindPhone() {
  if (!phoneForm.phone || !/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    ElMessage.warning('请输入正确的手机号'); return
  }
  try {
    await bindPhone(phoneForm.phone)
    ElMessage.success('绑定成功')
    showPhoneDialog.value = false
    phoneForm.phone = ''
    await userStore.fetchUserInfo()
  } catch (error) { console.error('绑定失败:', error) }
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
      if (codeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (error) { console.error('发送验证码失败:', error) } finally { sendingCode.value = false }
}

async function handleChangePhone() {
  if (!changePhoneForm.code) { ElMessage.warning('请输入验证码'); return }
  if (!changePhoneForm.newPhone || !/^1[3-9]\d{9}$/.test(changePhoneForm.newPhone)) {
    ElMessage.warning('请输入正确的新手机号'); return
  }
  changingPhone.value = true
  try {
    await changePhone(changePhoneForm.code, changePhoneForm.newPhone)
    ElMessage.success('手机号更改成功')
    showPhoneDialog.value = false
    await userStore.fetchUserInfo()
  } catch (error) { console.error('更改手机号失败:', error) } finally { changingPhone.value = false }
}

function resetPhoneForm() {
  phoneForm.phone = ''
  changePhoneForm.code = ''
  changePhoneForm.newPhone = ''
  changePhoneStep.value = 1
  codeCountdown.value = 0
}

async function handleChangePassword() {
  if (!passwordForm.oldPassword) { ElMessage.warning('请输入原密码'); return }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位'); return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致'); return
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
  } catch (error) { console.error('修改密码失败:', error) } finally { changingPassword.value = false }
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

// Sidebar
.sidebar {
  background: #fff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.sidebar-header {
  padding: 20px 20px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid var(--color-border-light);

  .logo-icon {
    flex-shrink: 0;
    display: flex;
    align-items: center;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    white-space: nowrap;
    background: linear-gradient(135deg, #6366f1, #4f46e5);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.nav-section {
  margin-bottom: 8px;
}

.section-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-tertiary);
  padding: 8px 12px 6px;
}

.new-chat-btn-wrap {
  padding: 4px 0 8px;
}

.new-chat-btn {
  width: 100%;
  border-radius: 10px;
  height: 40px;
  font-weight: 500;

  &.collapsed {
    width: 40px;
    padding: 0;
    min-width: unset;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all 0.15s ease;
  margin-bottom: 2px;
  font-size: 14px;

  &:hover {
    background: var(--color-primary-bg);
    color: var(--color-primary);
  }

  &.active {
    background: var(--color-primary-bg);
    color: var(--color-primary);
    font-weight: 600;
  }
}

// Session List
.session-list {
  .session-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    color: var(--color-text-secondary);
    transition: all 0.15s ease;
    margin-bottom: 2px;

    &:hover {
      background: var(--color-bg-secondary);
    }

    &.active {
      background: var(--color-primary-bg);
      color: var(--color-primary);

      .session-time {
        color: var(--color-primary-light);
      }
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
      font-size: 13px;
    }

    .session-time {
      font-size: 11px;
      color: var(--color-text-tertiary);
    }

    .more-icon {
      opacity: 0;
      transition: opacity 0.2s;
      font-size: 14px;
      flex-shrink: 0;
    }

    &:hover .more-icon {
      opacity: 1;
    }
  }

  .empty-sessions {
    text-align: center;
    color: var(--color-text-tertiary);
    font-size: 13px;
    padding: 16px 8px;
  }
}

// Footer
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--color-border-light);

  &.collapsed {
    display: flex;
    justify-content: center;
    padding: 16px 12px;
  }

  .user-bar {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  .user-detail {
    display: flex;
    flex-direction: column;
    gap: 2px;
    overflow: hidden;

    .username-text {
      font-size: 13px;
      font-weight: 600;
      color: var(--color-text-primary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.footer-actions {
  .action-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 4px;
    cursor: pointer;
    color: var(--color-text-secondary);
    font-size: 13px;
    border-radius: 6px;
    transition: all 0.15s;

    &:hover {
      color: var(--color-text-primary);
      background: var(--color-bg-secondary);
    }

    span { flex: 1; }

    .arrow {
      font-size: 12px;
      transition: transform 0.2s ease;

      &.open {
        transform: rotate(90deg);
      }
    }
  }

  .action-sub {
    padding-left: 20px;

    .sub-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 7px 4px;
      cursor: pointer;
      color: var(--color-text-secondary);
      font-size: 12px;
      border-radius: 6px;
      transition: all 0.15s;

      &:hover {
        color: var(--color-text-primary);
      }

      &.logout {
        color: #ef4444;

        &:hover {
          color: #dc2626;
        }
      }
    }
  }
}

// Main Content
.main-content {
  background: var(--color-bg);
  padding: 0;
  overflow: hidden;
}
</style>
