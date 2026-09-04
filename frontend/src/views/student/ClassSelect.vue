<template>
  <div class="select-page">
    <!-- Decorative orbs -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>

    <div class="layout">
      <!-- 左侧边栏 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <el-dropdown trigger="click" placement="bottom">
            <div class="avatar-wrapper">
              <div class="avatar">{{ displayName.charAt(0) || '?' }}</div>
              <div class="avatar-edit">⚙</div>
            </div>
            <template #dropdown>
              <el-dropdown-menu popper-class="sidebar-dropdown">
                <el-dropdown-item @click="showPasswordDialog = true">修改密码</el-dropdown-item>
                <el-dropdown-item @click="showPhoneDialog = true">{{ userPhone ? '手机号管理' : '绑定手机号' }}</el-dropdown-item>
                <el-dropdown-item @click="showEmailDialog = true">{{ userEmail ? '邮箱管理' : '绑定邮箱' }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <h2 class="sidebar-name">{{ displayName }}</h2>
          <p class="sidebar-role">学生</p>
        </div>

        <div class="info-list">
          <div class="info-item">
            <span class="info-label">学号</span>
            <span class="info-value">{{ userStore.userInfo?.username || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">姓名</span>
            <span class="info-value">{{ userStore.userInfo?.realName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">学院</span>
            <span class="info-value">{{ userStore.userInfo?.college || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">专业</span>
            <span class="info-value">{{ userStore.userInfo?.major || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">年级</span>
            <span class="info-value">{{ userStore.userInfo?.grade || '-' }}</span>
          </div>
        </div>

        <div class="sidebar-footer">
          <button class="logout-btn" @click="handleLogout">
            <svg viewBox="0 0 16 16" width="16" height="16" fill="none">
              <path d="M6 2H3a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <path d="M10 11l3-3-3-3M13 8H6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            退出登录
          </button>
        </div>
      </aside>

      <!-- 右侧主区域 -->
      <div class="main-area">
        <!-- Loading -->
        <div v-if="loading" class="main-center">
          <el-icon class="is-loading" :size="36" color="#fff"><Loading /></el-icon>
        </div>

        <!-- No classes -->
        <div v-else-if="classes.length === 0" class="main-center">
          <div class="select-card">
            <div class="card-icon">
              <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" width="48" height="48">
                <rect width="48" height="48" rx="12" fill="rgba(99,102,241,0.1)"/>
                <path d="M14 16h20M14 24h14M14 32h20" stroke="#6366f1" stroke-width="2.5" stroke-linecap="round"/>
                <circle cx="36" cy="24" r="5" stroke="#6366f1" stroke-width="2" fill="none"/>
                <circle cx="36" cy="24" r="1.5" fill="#6366f1"/>
              </svg>
            </div>
            <h1>欢迎使用 {{ userStore.siteName }}</h1>
            <p class="card-sub">您还未加入任何班级</p>
            <p class="card-hint">请等待教师将您加入班级后即可开始学习</p>
          </div>
        </div>

        <!-- Class selection: Grid 卡片布局 -->
        <div v-else class="main-center">
          <div class="grid-container">
            <h1 class="grid-title">欢迎使用 {{ userStore.siteName }}</h1>
            <p class="grid-subtitle">请选择要进入的班级课程</p>

            <div class="course-grid">
              <div
                v-for="c in classes"
                :key="c.id"
                class="course-card"
                :class="{ 'course-card-disabled': isNotStarted(c) }"
                @click="enterClassById(c.id, c)"
              >
                <!-- 封面区：渐变色 + 课程首字 -->
                <div class="course-cover" :style="coverStyle(c)">
                  <span class="cover-letter">{{ c.name }}</span>
                  <span v-if="isNotStarted(c)" class="cover-badge">未开班</span>
                </div>
                <!-- 信息区 -->
                <div class="course-info">
                  <h3 class="course-name">{{ c.name }}</h3>

                  <!-- 时间信息 -->
                  <div class="course-meta">
                    <svg class="meta-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                      <rect x="1.5" y="3" width="13" height="11" rx="2" stroke="currentColor" stroke-width="1.4"/>
                      <path d="M1.5 6h13M5 1.5v3M11 1.5v3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                    </svg>
                    <span>{{ formatDateRange(c.startTime, c.endTime) }}</span>
                  </div>

                  <!-- 资源标签 -->
                  <div class="course-tags">
                    <span v-if="c.videoSetName" class="tag tag-video">
                      <svg viewBox="0 0 16 16" width="11" height="11" fill="none">
                        <rect x="1" y="3" width="10" height="10" rx="2" stroke="currentColor" stroke-width="1.4"/>
                        <path d="M11 7l4-2v6l-4-2z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                      </svg>
                      {{ c.videoSetName }}
                    </span>
                    <span v-if="c.kbName" class="tag tag-kb">
                      <svg viewBox="0 0 16 16" width="11" height="11" fill="none">
                        <path d="M2 2h7l4 4v8H2z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                        <path d="M9 2v4h4" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                      </svg>
                      {{ c.kbName }}
                    </span>
                  </div>

                  <div v-if="isNotStarted(c)" class="course-hint">
                    {{ formatStartTime(c.startTime) }} 开放
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="400px" modal-class="glass-modal">
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

    <!-- 绑定/更改手机号对话框 -->
    <el-dialog v-model="showPhoneDialog" :title="userPhone ? '手机号管理' : '绑定手机号'" width="440px" modal-class="glass-modal" @close="resetPhoneForm">
      <el-alert v-if="!userPhone" type="info" :closable="false" style="margin-bottom: 18px">
        <template #title>绑定手机号后可通过验证码找回密码</template>
      </el-alert>

      <template v-if="userPhone">
        <div style="margin-bottom: 18px">
          <div style="color: #94a3b8; font-size:13px; margin-bottom:6px">当前手机号</div>
          <el-tag type="success" size="large">{{ maskedPhone }}</el-tag>
        </div>
        <div>
          <div style="font-weight:600; margin-bottom:12px; font-size:14px">更改手机号</div>
          <div v-if="changePhoneStep === 1">
            <div style="color: #94a3b8; font-size:13px; margin-bottom:12px">
              验证码将发送至 {{ maskedPhone }}
            </div>
            <el-button type="primary" :loading="sendingCode" :disabled="codeCountdown > 0" @click="handleSendChangeCode" style="width:100%">
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

    <!-- 绑定/解绑邮箱对话框 -->
    <el-dialog v-model="showEmailDialog" :title="userEmail ? '邮箱管理' : '绑定邮箱'" width="440px" modal-class="glass-modal" @close="emailForm.email = ''">
      <el-alert v-if="!userEmail" type="info" :closable="false" style="margin-bottom: 18px">
        <template #title>绑定邮箱后可通过邮箱验证码找回密码</template>
      </el-alert>

      <template v-if="userEmail">
        <div style="margin-bottom: 18px">
          <div style="color: #94a3b8; font-size:13px; margin-bottom:6px">当前邮箱</div>
          <el-tag type="success" size="large">{{ maskedEmail }}</el-tag>
        </div>
        <el-button type="danger" plain @click="handleUnbindEmail" style="width:100%">解绑邮箱</el-button>
      </template>
      <template v-else>
        <el-form label-width="80px">
          <el-form-item label="邮箱">
            <el-input v-model="emailForm.email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button v-if="!userEmail" type="primary" :loading="bindingEmail" @click="handleBindEmail">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { bindPhone, changePhone, changePassword, sendPhoneCode, bindEmail, unbindEmail } from '@/api/auth'

const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()

const loading = ref(true)
const selectedClassId = ref(null)
const classes = computed(() => chatStore.classes)

const displayName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '')
const userPhone = computed(() => userStore.userInfo?.phone || '')
const maskedPhone = computed(() => {
  const p = userPhone.value
  return p ? p.substring(0, 3) + '****' + p.substring(7) : ''
})
const userEmail = computed(() => userStore.userInfo?.email || '')
const maskedEmail = computed(() => {
  const e = userEmail.value
  if (!e) return ''
  const at = e.indexOf('@')
  return at > 2 ? e.substring(0, 2) + '****' + e.substring(at) : e
})

// 修改密码
const showPasswordDialog = ref(false)
const changingPassword = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 绑定/更改手机号
const showPhoneDialog = ref(false)
const phoneForm = reactive({ phone: '' })
const changePhoneForm = reactive({ code: '', newPhone: '' })
const changePhoneStep = ref(1)
const sendingCode = ref(false)
const codeCountdown = ref(0)
let codeTimer = null
const changingPhone = ref(false)

// 绑定邮箱
const showEmailDialog = ref(false)
const emailForm = reactive({ email: '' })
const bindingEmail = ref(false)

// 渐变色调色板：不同课程名映射到不同渐变色
const GRADIENT_PALETTES = [
  ['#667eea', '#764ba2'], // 紫蓝
  ['#f093fb', '#f5576c'], // 粉红
  ['#4facfe', '#00f2fe'], // 青蓝
  ['#43e97b', '#38f9d7'], // 翠绿
  ['#fa709a', '#fee140'], // 橙粉
  ['#a8edea', '#fed6e3'], // 薄荷
  ['#ff9a9e', '#fecfef'], // 暖粉
  ['#ffecd2', '#fcb69f'], // 暖橙
  ['#a18cd1', '#fbc2eb'], // 淡紫
  ['#5ee7df', '#b490ca'], // 青紫
]

// 根据课程 id 选取渐变色，保证不同课程颜色不同
function coverStyle(c) {
  const idx = (c.id || 0) % GRADIENT_PALETTES.length
  const [c1, c2] = GRADIENT_PALETTES[idx]
  return { background: `linear-gradient(135deg, ${c1}, ${c2})` }
}

// 格式化时间范围
function formatDateRange(start, end) {
  const fmt = (d) => {
    if (!d) return ''
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  }
  const s = fmt(start)
  const e = fmt(end)
  if (s && e) return `${s} - ${e}`
  if (s) return `${s} 起`
  return '长期有效'
}

// 是否未开班
function isNotStarted(c) {
  if (!c.startTime) return false
  return new Date(c.startTime).getTime() > Date.now()
}

// 格式化开班时间
function formatStartTime(start) {
  if (!start) return ''
  const dt = new Date(start)
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const d = String(dt.getDate()).padStart(2, '0')
  const h = String(dt.getHours()).padStart(2, '0')
  const min = String(dt.getMinutes()).padStart(2, '0')
  return `${m}/${d} ${h}:${min}`
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}

// ===== 修改密码 =====
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
    await userStore.logout()
    router.push('/login')
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    changingPassword.value = false
  }
}

// ===== 绑定手机号 =====
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

// ===== 绑定邮箱 =====
async function handleBindEmail() {
  if (!emailForm.email || !/^[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}$/.test(emailForm.email)) {
    ElMessage.warning('请输入正确的邮箱'); return
  }
  bindingEmail.value = true
  try {
    await bindEmail(emailForm.email)
    ElMessage.success('绑定成功')
    showEmailDialog.value = false
    emailForm.email = ''
    await userStore.fetchUserInfo()
  } catch (error) { console.error('绑定失败:', error) } finally { bindingEmail.value = false }
}

async function handleUnbindEmail() {
  try {
    await ElMessageBox.confirm('确定要解绑邮箱吗？解绑后将无法通过邮箱找回密码。', '提示', { type: 'warning' })
    await unbindEmail()
    ElMessage.success('已解绑')
    showEmailDialog.value = false
    await userStore.fetchUserInfo()
  } catch (error) { /* 用户取消或失败 */ }
}

// ===== 更改手机号 =====
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
    if (codeTimer) clearInterval(codeTimer)
    codeTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) { clearInterval(codeTimer); codeTimer = null }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    sendingCode.value = false
  }
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

onBeforeUnmount(() => {
  if (codeTimer) clearInterval(codeTimer)
})

onMounted(async () => {
  // 已有班级选择（从 localStorage 恢复），直接进入主界面
  if (chatStore.currentClassId) {
    router.replace('/home')
    return
  }
  await chatStore.fetchClasses()
  loading.value = false
})

async function enterClass() {
  if (!selectedClassId.value) return
  await chatStore.setCurrentClass(selectedClassId.value)
  router.push('/home')
}

// 卡片点击直接进入班级
async function enterClassById(classId, cls) {
  if (!classId) return
  if (cls && isNotStarted(cls)) {
    ElMessage.info(`班级尚未开班，${formatStartTime(cls.startTime)} 开放`)
    return
  }
  selectedClassId.value = classId
  await enterClass()
}
</script>

<style scoped lang="scss">
.select-page {
  position: relative;
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 40%, #3730a3 100%);
  overflow: hidden;
  display: flex;
}

.orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  pointer-events: none;
}

.orb-1 {
  top: -25%;
  right: -10%;
  width: 600px;
  height: 600px;
}

.orb-2 {
  bottom: -15%;
  left: -8%;
  width: 420px;
  height: 420px;
  background: rgba(255, 255, 255, 0.04);
}

/* 左右布局 */
.layout {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  min-height: 100vh;
}

/* 左侧边栏 */
.sidebar {
  width: 280px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  padding: 32px 24px;
}

.sidebar-header {
  text-align: center;
  margin-bottom: 32px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  cursor: pointer;
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  color: #6366f1;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.avatar-wrapper:hover .avatar-edit {
  transform: rotate(90deg);
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.sidebar-name {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 4px;
}

.sidebar-role {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.info-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  word-break: break-all;
}

.sidebar-footer {
  margin-top: 24px;
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    color: #fff;
    border-color: rgba(255, 255, 255, 0.3);
  }
}

/* 右侧主区域 */
.main-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  min-width: 0;
}

.main-center {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

/* 无班级时的卡片 */
.select-card {
  background: #fff;
  border-radius: 20px;
  padding: 48px 40px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.18);
  max-width: 420px;
  width: 100%;
  text-align: center;
}

.card-icon {
  display: inline-flex;
  margin-bottom: 24px;
}

.select-card h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
  line-height: 1.3;
}

.card-sub {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin: 0;
}

.card-hint {
  font-size: 14px;
  color: #94a3b8;
  line-height: 1.6;
  margin: 8px 0 0;
}

/* Grid 卡片布局 */
.grid-container {
  width: 100%;
  max-width: 1100px;
  text-align: center;
}

.grid-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
}

.grid-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 32px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

@media (max-width: 1200px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 700px) {
  .layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.12);
    padding: 20px 24px;
  }

  .info-list {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 12px 24px;
  }

  .info-item {
    flex: 0 0 auto;
  }

  .sidebar-footer {
    margin-top: 12px;
  }

  .logout-btn {
    width: auto;
  }

  .course-grid {
    grid-template-columns: 1fr;
  }
}

.course-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 24px 56px rgba(0, 0, 0, 0.28);

    .course-cover .cover-letter {
      transform: scale(1.1);
    }
  }
}

.course-card-disabled {
  cursor: not-allowed;
  opacity: 0.7;

  &:hover {
    transform: none;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  }
}

.cover-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.course-hint {
  font-size: 12px;
  color: #f59e0b;
  font-weight: 600;
  margin-top: 2px;
}

.course-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 30% 40%, rgba(255, 255, 255, 0.2), transparent 60%);
  }
}

.cover-letter {
  font-size: 22px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
  transition: transform 0.3s ease;
  position: relative;
  z-index: 1;
  padding: 0 16px;
  text-align: center;
  line-height: 1.4;
  word-break: break-all;
}

.course-info {
  padding: 18px 20px 20px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.course-name {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-icon {
  flex-shrink: 0;
  color: #cbd5e1;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  line-height: 1.4;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-video {
  background: #eff6ff;
  color: #3b82f6;
}

.tag-kb {
  background: #f0fdf4;
  color: #22c55e;
}
</style>

<style>
.glass-modal .el-dialog {
  background: rgba(30, 27, 75, 0.88) !important;
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.3);
}
.glass-modal .el-dialog__header {
  padding: 20px 24px 12px;
}
.glass-modal .el-dialog__title {
  color: #fff;
  font-weight: 600;
  font-size: 17px;
}
.glass-modal .el-dialog__body {
  padding: 12px 24px 20px;
  color: rgba(255, 255, 255, 0.85);
}
.glass-modal .el-dialog__footer {
  padding: 12px 24px 20px;
}
.glass-modal .el-form-item__label {
  color: rgba(255, 255, 255, 0.7);
}
.glass-modal .el-input__wrapper {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  box-shadow: none !important;
}
.glass-modal .el-input__wrapper:hover {
  border-color: rgba(255, 255, 255, 0.3) !important;
}
.glass-modal .el-input__wrapper.is-focus {
  border-color: rgba(129, 140, 248, 0.8) !important;
}
.glass-modal .el-input__inner {
  color: #fff !important;
}
.glass-modal .el-input__inner::placeholder {
  color: rgba(255, 255, 255, 0.4) !important;
}
.glass-modal .el-input__suffix .el-icon {
  color: rgba(255, 255, 255, 0.5);
}
.glass-modal .el-button--default {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: rgba(255, 255, 255, 0.2) !important;
  color: rgba(255, 255, 255, 0.8) !important;
}
.glass-modal .el-button--default:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
}
.glass-modal .el-alert--info {
  background: rgba(99, 102, 241, 0.15) !important;
  border: 1px solid rgba(99, 102, 241, 0.2) !important;
}
.glass-modal .el-alert__title {
  color: rgba(255, 255, 255, 0.8) !important;
}
.glass-modal .el-tag--success {
  background: rgba(34, 197, 94, 0.2) !important;
  border-color: rgba(34, 197, 94, 0.3) !important;
  color: #86efac !important;
}
.glass-modal .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 255, 255, 0.5);
}
.glass-modal .el-dialog__headerbtn:hover .el-dialog__close {
  color: #fff;
}

/* 头像下拉菜单深色风格 */
.el-popper.sidebar-dropdown {
  background: rgba(30, 27, 75, 0.92) !important;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4) !important;
}
.el-popper.sidebar-dropdown .el-dropdown-menu {
  background: transparent !important;
  padding: 4px;
}
.el-popper.sidebar-dropdown .el-dropdown-menu__item {
  color: rgba(255, 255, 255, 0.8) !important;
  border-radius: 8px;
  padding: 8px 16px;
}
.el-popper.sidebar-dropdown .el-dropdown-menu__item:hover {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #fff !important;
}
.el-popper.sidebar-dropdown .el-popper__arrow::before {
  background: rgba(30, 27, 75, 0.92) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
}
</style>
