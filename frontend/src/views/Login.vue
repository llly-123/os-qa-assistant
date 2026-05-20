<template>
  <div class="login-container">
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="36" color="#fff"><Reading /></el-icon>
          </div>
        </div>
        <h1>操作系统AI答疑助手</h1>
        <p>基于西安电子科技大学《操作系统》教材</p>
      </div>
      
      <el-form 
        ref="loginFormRef" 
        :model="loginForm" 
        :rules="loginRules" 
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入学号/工号"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
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
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <el-button type="text" @click="showResetDialog = true">
          忘记密码？
        </el-button>
      </div>
    </div>
    
    <el-dialog 
      v-model="showResetDialog" 
      title="重置密码" 
      width="400px"
    >
      <el-form :model="resetForm" label-width="80px">
        <el-form-item label="学号">
          <el-input v-model="resetForm.studentId" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="resetForm.email" placeholder="请输入绑定邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="verify-code-input">
            <el-input v-model="resetForm.code" placeholder="请输入验证码" />
            <el-button 
              :disabled="countdown > 0" 
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetDialog = false">取消</el-button>
        <el-button type="primary" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { sendVerifyCode, resetPassword } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const showResetDialog = ref(false)
const countdown = ref(0)

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
  studentId: '',
  email: '',
  code: ''
})

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
    
    if (userStore.role === 'TEACHER') {
      router.push('/admin/students')
    } else {
      router.push('/chat')
    }
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

async function sendCode() {
  if (!resetForm.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  
  try {
    await sendVerifyCode(resetForm.email)
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

async function handleResetPassword() {
  if (!resetForm.studentId || !resetForm.email || !resetForm.code) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    await resetPassword(resetForm.email)
    ElMessage.success('密码已重置为学号后6位，请登录后修改')
    showResetDialog.value = false
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  
  .circle {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.4;
  }
  
  .circle-1 {
    width: 400px;
    height: 400px;
    background: #667eea;
    top: -100px;
    right: -100px;
    animation: float1 8s ease-in-out infinite;
  }
  
  .circle-2 {
    width: 300px;
    height: 300px;
    background: #764ba2;
    bottom: -50px;
    left: -50px;
    animation: float2 10s ease-in-out infinite;
  }
  
  .circle-3 {
    width: 200px;
    height: 200px;
    background: #f093fb;
    top: 50%;
    left: 60%;
    animation: float3 12s ease-in-out infinite;
  }
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-30px, 30px); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -20px); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-15px, -25px); }
}

.login-card {
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.4), 0 0 40px rgba(102, 126, 234, 0.15);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 1;
  animation: cardAppear 0.6s ease-out;
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
  
  .logo {
    margin-bottom: 20px;
    display: flex;
    justify-content: center;
    
    .logo-icon {
      width: 64px;
      height: 64px;
      border-radius: 18px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
    }
  }
  
  h1 {
    font-size: 26px;
    color: #1a1a2e;
    margin-bottom: 8px;
    font-weight: 700;
    letter-spacing: 1px;
  }
  
  p {
    font-size: 14px;
    color: #8c8c9a;
  }
}

.login-form {
  .login-btn {
    width: 100%;
    margin-top: 10px;
    height: 44px;
    font-size: 16px;
    border-radius: 10px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    letter-spacing: 4px;
    
    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    }
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  
  .el-button {
    color: #667eea;
  }
}

.verify-code-input {
  display: flex;
  gap: 10px;
  
  .el-input {
    flex: 1;
  }
}
</style>
