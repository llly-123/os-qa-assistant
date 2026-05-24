<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="48" color="#409eff"><Reading /></el-icon>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  
  .logo {
    margin-bottom: 16px;
  }
  
  h1 {
    font-size: 24px;
    color: #303133;
    margin-bottom: 8px;
  }
  
  p {
    font-size: 14px;
    color: #909399;
  }
}

.login-form {
  .login-btn {
    width: 100%;
    margin-top: 10px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
}

.verify-code-input {
  display: flex;
  gap: 10px;
  
  .el-input {
    flex: 1;
  }
}
</style>
