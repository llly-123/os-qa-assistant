<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>系统设置</h2>
    </div>

    <!-- AI 接口配置 -->
    <el-card class="setting-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><Connection /></el-icon>
          <span>AI 接口配置</span>
        </div>
      </template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
        title="回答学生问题所用的大模型接口。留空则 AI 答疑功能不可用。"
      />
      <el-form :model="aiForm" label-width="120px" label-position="left">
        <el-form-item label="API Key">
          <el-input
            v-model="aiForm.ai_api_key"
            type="password"
            show-password
            placeholder="已设置，留空则不修改"
            autocomplete="new-password"
          >
            <template #append>
              <el-button @click="handleClearAiKey">清空</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input
            v-model="aiForm.ai_base_url"
            placeholder="OpenAI 兼容接口地址，如 https://api.deepseek.com"
          />
        </el-form-item>
        <el-form-item label="模型名">
          <el-input
            v-model="aiForm.ai_model_name"
            placeholder="填写对应厂商的模型名，如 deepseek-chat / glm-4 / doubao-seed-1.6"
          />
        </el-form-item>
      </el-form>
      <div class="test-bar">
        <el-button :loading="testing" @click="handleTest">
          <el-icon><Connection /></el-icon>
          测试连接
        </el-button>
      </div>
      <el-alert
        v-if="testResult"
        :type="testResult.success ? 'success' : 'error'"
        :title="testResult.message"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      />
    </el-card>

    <!-- 短信配置 -->
    <el-card class="setting-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><Message /></el-icon>
          <span>短信配置</span>
        </div>
      </template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
        title="用于找回密码/更换手机号时发送验证码（阿里云短信）。四项留空则走开发模式，验证码打印到后端日志。"
      />
      <el-form :model="smsForm" label-width="140px" label-position="left">
        <el-form-item label="AccessKey ID">
          <el-input v-model="smsForm.sms_access_key_id" autocomplete="off" />
        </el-form-item>
        <el-form-item label="AccessKey Secret">
          <el-input
            v-model="smsForm.sms_access_key_secret"
            type="password"
            show-password
            placeholder="已设置，留空则不修改"
            autocomplete="new-password"
          >
            <template #append>
              <el-button @click="handleClearSmsSecret">清空</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="签名名称">
          <el-input v-model="smsForm.sms_sign_name" placeholder="短信签名，如：XX答疑" />
        </el-form-item>
        <el-form-item label="模板 Code">
          <el-input v-model="smsForm.sms_template_code" placeholder="验证码模板 Code，模板需包含 code 变量" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 邮箱配置 -->
    <el-card class="setting-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><Message /></el-icon>
          <span>邮箱配置</span>
        </div>
      </template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
        title="用于邮箱验证码找回密码（SMTP 发信）。四项留空则走开发模式，验证码打印到后端日志。"
      />
      <el-form :model="mailForm" label-width="140px" label-position="left">
        <el-form-item label="SMTP 服务器">
          <el-input v-model="mailForm.mail_host" placeholder="如 smtp.qq.com" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="mailForm.mail_port" placeholder="如 587" />
        </el-form-item>
        <el-form-item label="发件邮箱">
          <el-input v-model="mailForm.mail_username" placeholder="如 xxx@qq.com" />
        </el-form-item>
        <el-form-item label="授权码">
          <el-input
            v-model="mailForm.mail_password"
            type="password"
            show-password
            placeholder="已设置，留空则不修改"
            autocomplete="new-password"
          >
            <template #append>
              <el-button @click="handleClearMailPassword">清空</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="save-bar">
      <el-button type="primary" :loading="saving" @click="handleSave">
        <el-icon><Check /></el-icon>
        保存设置
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Check, Message } from '@element-plus/icons-vue'
import { getSettings, updateSettings, testAi, clearSetting } from '@/api/setting'

const aiForm = reactive({
  ai_api_key: '',
  ai_base_url: '',
  ai_model_name: ''
})

const smsForm = reactive({
  sms_access_key_id: '',
  sms_access_key_secret: '',
  sms_sign_name: '',
  sms_template_code: ''
})

const mailForm = reactive({
  mail_host: '',
  mail_port: '',
  mail_username: '',
  mail_password: ''
})

const saving = ref(false)
const testing = ref(false)
const testResult = ref(null)

async function fetchSettings() {
  try {
    const res = await getSettings()
    const data = res.data || res
    if (data) {
      aiForm.ai_api_key = data.ai_api_key || ''
      aiForm.ai_base_url = data.ai_base_url || ''
      aiForm.ai_model_name = data.ai_model_name || ''
      smsForm.sms_access_key_id = data.sms_access_key_id || ''
      smsForm.sms_access_key_secret = data.sms_access_key_secret || ''
      smsForm.sms_sign_name = data.sms_sign_name || ''
      smsForm.sms_template_code = data.sms_template_code || ''
      mailForm.mail_host = data.mail_host || ''
      mailForm.mail_port = data.mail_port || ''
      mailForm.mail_username = data.mail_username || ''
      mailForm.mail_password = data.mail_password || ''
    }
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateSettings({
      ...aiForm,
      ...smsForm,
      ...mailForm
    })
    ElMessage.success('设置已保存')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  testing.value = true
  testResult.value = null
  try {
    const res = await testAi({ ...aiForm })
    testResult.value = res.data || res
  } catch (e) {
    testResult.value = { success: false, message: '测试请求失败，请检查后端服务' }
  } finally {
    testing.value = false
  }
}

async function handleClearAiKey() {
  try {
    await ElMessageBox.confirm('确定要清空 AI API Key 吗？清空后 AI 答疑功能将不可用。', '提示', { type: 'warning' })
    await clearSetting('ai_api_key')
    aiForm.ai_api_key = ''
    ElMessage.success('已清空')
    await fetchSettings()
  } catch (e) {
    // 用户取消或请求失败
  }
}

async function handleClearSmsSecret() {
  try {
    await ElMessageBox.confirm('确定要清空短信 AccessKey Secret 吗？清空后短信将退回开发模式。', '提示', { type: 'warning' })
    await clearSetting('sms_access_key_secret')
    smsForm.sms_access_key_secret = ''
    ElMessage.success('已清空')
    await fetchSettings()
  } catch (e) {
    // 用户取消或请求失败
  }
}

async function handleClearMailPassword() {
  try {
    await ElMessageBox.confirm('确定要清空邮箱 SMTP 授权码吗？清空后邮件将退回开发模式。', '提示', { type: 'warning' })
    await clearSetting('mail_password')
    mailForm.mail_password = ''
    ElMessage.success('已清空')
    await fetchSettings()
  } catch (e) {
    // 用户取消或请求失败
  }
}

onMounted(fetchSettings)
</script>

<style scoped lang="scss">
.settings-page {
  padding: 28px 32px;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
  }
}

.setting-card {
  margin-bottom: 20px;
  border-radius: 12px;

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
}

.save-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.test-bar {
  display: flex;
  gap: 10px;
}
</style>
