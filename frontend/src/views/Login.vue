<template>
  <div class="login-page">
    <!-- Left: Brand Panel -->
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="rgba(255,255,255,0.2)"/>
            <path d="M14 16h20M14 24h14M14 32h20" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
            <circle cx="36" cy="24" r="5" stroke="white" stroke-width="2" fill="none"/>
            <circle cx="36" cy="24" r="1.5" fill="white"/>
          </svg>
        </div>
        <h1>{{ userStore.siteName }}</h1>
        <p class="brand-desc">智能检索 · 精准回答 · 高效学习</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">📚</span>
            <span>教材知识库检索</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🤖</span>
            <span>AI 智能答疑</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🎬</span>
            <span>视频同步学习</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Login Form -->
    <div class="form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请使用学号/工号登录</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入学号/工号"
              :prefix-icon="User"
              size="large"
              class="custom-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              class="custom-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <el-button link class="forgot-link" @click="showResetDialog = true">
            忘记密码？
          </el-button>
          <el-button link class="forgot-link" @click="showRegisterDialog = true">
            教师注册
          </el-button>
        </div>
      </div>
    </div>

    <!-- Reset Password Dialog -->
    <el-dialog
      v-model="showResetDialog"
      title="找回密码"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        <template #title>
          绑定手机号或邮箱后可通过验证码找回密码；均未绑定请联系教师重置。
        </template>
      </el-alert>

      <el-tabs v-model="resetTab">
        <el-tab-pane label="手机号找回" name="phone">
          <el-form :model="resetForm" label-width="80px">
            <el-form-item label="手机号">
              <el-input v-model="resetForm.phone" placeholder="请输入绑定的手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="验证码">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input v-model="resetForm.code" placeholder="请输入验证码" maxlength="6" style="flex:1" />
                <el-button :disabled="phoneCountdown > 0 || !resetForm.phone" @click="sendPhoneCodeHandler">
                  {{ phoneCountdown > 0 ? `${phoneCountdown}s` : '获取验证码' }}
                </el-button>
              </div>
              <div v-if="phoneDevCode" class="dev-code-tip" @click="copyDevCode(phoneDevCode)">
                <span class="dev-label">开发模式验证码：</span>
                <span class="dev-value">{{ phoneDevCode }}</span>
                <span class="dev-hint">(点击复制)</span>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="邮箱找回" name="email">
          <el-form :model="resetForm" label-width="80px">
            <el-form-item label="邮箱">
              <el-input v-model="resetForm.email" placeholder="请输入绑定的邮箱" />
            </el-form-item>
            <el-form-item label="验证码">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input v-model="resetForm.emailCode" placeholder="请输入验证码" maxlength="6" style="flex:1" />
                <el-button :disabled="emailCountdown > 0 || !resetForm.email" @click="sendEmailCodeHandler">
                  {{ emailCountdown > 0 ? `${emailCountdown}s` : '获取验证码' }}
                </el-button>
              </div>
              <div v-if="emailDevCode" class="dev-code-tip" @click="copyDevCode(emailDevCode)">
                <span class="dev-label">开发模式验证码：</span>
                <span class="dev-value">{{ emailDevCode }}</span>
                <span class="dev-hint">(点击复制)</span>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="showResetDialog = false">取消</el-button>
        <el-button type="primary" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
    <!-- Register Dialog -->
    <el-dialog
      v-model="showRegisterDialog"
      title="教师注册"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        <template #title>注册后需等待管理员审核，审核通过后方可登录。</template>
      </el-alert>
      <el-form :model="registerForm" label-width="80px">
        <el-form-item label="工号">
          <el-input v-model="registerForm.username" placeholder="请输入工号" maxlength="50" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="registerForm.realName" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegisterDialog = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="handleRegister">提交注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { sendPhoneCode, resetPasswordByPhone, sendEmailCode, resetPasswordByEmail, register } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const showResetDialog = ref(false)
const resetTab = ref('phone')
const phoneCountdown = ref(0)
const emailCountdown = ref(0)
const phoneDevCode = ref('')
const emailDevCode = ref('')

const showRegisterDialog = ref(false)
const registering = ref(false)
const registerForm = reactive({
  username: '',
  realName: '',
  password: ''
})

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const resetForm = reactive({
  phone: '',
  code: '',
  email: '',
  emailCode: ''
})

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')

    if (userStore.role === 'SUPER_ADMIN') {
      router.push('/admin/teachers')
    } else if (userStore.role === 'TEACHER') {
      router.push('/admin/students')
    } else {
      // 每次登录都重新选择班级，清除 store 和 localStorage 中的残留
      const chatStore = useChatStore()
      chatStore.abortAllAsk()
      chatStore.currentClassId = null
      chatStore.currentSessionId = null
      chatStore.sessions = []
      chatStore.messages = []
      localStorage.removeItem('currentClassId')
      localStorage.removeItem('currentSessionId')
      router.push('/select-class')
    }
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username.trim()) {
    ElMessage.warning('请输入工号')
    return
  }
  if (!registerForm.realName.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!registerForm.password || registerForm.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  registering.value = true
  try {
    await register({
      username: registerForm.username.trim(),
      realName: registerForm.realName.trim(),
      password: registerForm.password
    })
    ElMessage.success('注册成功，请等待管理员审核')
    showRegisterDialog.value = false
    registerForm.username = ''
    registerForm.realName = ''
    registerForm.password = ''
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    registering.value = false
  }
}

async function sendPhoneCodeHandler() {
  if (!resetForm.phone || !/^1[3-9]\d{9}$/.test(resetForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    const res = await sendPhoneCode(resetForm.phone)
    const data = res.data || res

    if (data.devMode && data.devCode) {
      phoneDevCode.value = data.devCode
      ElMessage.success('验证码已生成（开发模式）')
    } else {
      phoneDevCode.value = ''
      ElMessage.success('验证码已发送')
    }

    phoneCountdown.value = 60
    const timer = setInterval(() => {
      phoneCountdown.value--
      if (phoneCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

async function sendEmailCodeHandler() {
  if (!resetForm.email || !/^[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}$/.test(resetForm.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }

  try {
    const res = await sendEmailCode(resetForm.email)
    const data = res.data || res

    if (data.devMode && data.devCode) {
      emailDevCode.value = data.devCode
      ElMessage.success('验证码已生成（开发模式）')
    } else {
      emailDevCode.value = ''
      ElMessage.success('验证码已发送')
    }

    emailCountdown.value = 60
    const timer = setInterval(() => {
      emailCountdown.value--
      if (emailCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

async function handleResetPassword() {
  if (resetTab.value === 'email') {
    if (!resetForm.email || !resetForm.emailCode) {
      ElMessage.warning('请填写邮箱和验证码')
      return
    }
    try {
      await resetPasswordByEmail(resetForm.email, resetForm.emailCode)
      ElMessage.success('密码已重置为学号后6位，请登录后修改')
      showResetDialog.value = false
      emailDevCode.value = ''
    } catch (error) {
      console.error('重置密码失败:', error)
    }
    return
  }

  if (!resetForm.phone || !resetForm.code) {
    ElMessage.warning('请填写手机号和验证码')
    return
  }

  try {
    await resetPasswordByPhone(resetForm.phone, resetForm.code)
    ElMessage.success('密码已重置为学号后6位，请登录后修改')
    showResetDialog.value = false
    phoneDevCode.value = ''
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}

function copyDevCode(code) {
  if (code) {
    navigator.clipboard.writeText(code).then(() => {
      ElMessage.success('验证码已复制')
    }).catch(() => {
      const input = document.createElement('input')
      input.value = code
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      ElMessage.success('验证码已复制')
    })
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  min-height: 100vh;
}

// Left: Brand Panel
.brand-panel {
  flex: 1;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 40%, #3730a3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -20%;
    width: 600px;
    height: 600px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.04);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -30%;
    left: -10%;
    width: 400px;
    height: 400px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.03);
    pointer-events: none;
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 40px;
  max-width: 420px;
}

.brand-icon {
  margin-bottom: 28px;
  display: inline-block;
}

.brand-content h1 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 14px;
  letter-spacing: 0.02em;
  line-height: 1.3;
}

.brand-desc {
  font-size: 15px;
  opacity: 0.8;
  line-height: 1.8;
  margin-bottom: 36px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: center;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  font-size: 14px;
  backdrop-filter: blur(4px);
  min-width: 200px;
  justify-content: center;
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.18);
  }

  .feature-icon {
    font-size: 18px;
  }
}

// Right: Form Panel
.form-panel {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 36px;

  h2 {
    font-size: 26px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin-bottom: 8px;
  }

  p {
    color: var(--color-text-tertiary);
    font-size: 15px;
  }
}

.login-form {
  .custom-input {
    :deep(.el-input__wrapper) {
      padding: 4px 16px;
      border-radius: 10px !important;
      height: 48px;
    }
  }

  .el-form-item {
    margin-bottom: 20px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.05em;
  margin-top: 4px;
}

.form-footer {
  text-align: center;

  .forgot-link {
    color: var(--color-text-tertiary);
    font-size: 14px;

    &:hover {
      color: var(--color-primary);
    }
  }
}

.dev-code-tip {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: background 0.2s;

  &:hover {
    background: #fef3c7;
  }

  .dev-label {
    font-size: 12px;
    color: #92400e;
  }

  .dev-value {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-primary);
    letter-spacing: 4px;
    margin: 0 4px;
  }

  .dev-hint {
    font-size: 11px;
    color: #a16207;
  }
}

@media (max-width: 768px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    width: 100%;
  }
}
</style>
