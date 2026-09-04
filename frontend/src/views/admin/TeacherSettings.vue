<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>AI 接口配置</h2>
    </div>

    <!-- 体验状态提示 -->
    <el-alert
      v-if="inTrialPeriod && !hasOwnConfig"
      type="success"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
      title="正在体验管理员的 API 配置"
    >
      <template #default>
        <div>您尚未设置自己的 API，当前正在体验时间段内，系统使用管理员提供的 AI 接口。</div>
        <div v-if="trialStartTime && trialEndTime" style="margin-top: 4px; font-size: 13px;">
          体验时间：{{ formatTime(trialStartTime) }} ~ {{ formatTime(trialEndTime) }}
        </div>
      </template>
    </el-alert>
    <el-alert
      v-if="!inTrialPeriod && !hasOwnConfig && configLoaded"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
      title="未配置 API 且不在体验期"
    >
      <template #default>
        <div>您尚未设置自己的 API，且当前不在体验时间段内，AI 答疑功能暂不可用。请配置自己的 API 或联系管理员开放体验。</div>
        <div v-if="trialStartTime && trialEndTime" style="margin-top: 4px; font-size: 13px;">
          体验时间段：{{ formatTime(trialStartTime) }} ~ {{ formatTime(trialEndTime) }}
        </div>
      </template>
    </el-alert>
    <el-alert
      v-if="hasOwnConfig"
      type="success"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
      title="正在使用您自己的 API 配置"
    />

    <!-- 教师自己的 API 配置 -->
    <el-card class="setting-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><Connection /></el-icon>
          <span>我的 AI 接口配置</span>
        </div>
      </template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
        title="配置您自己的 AI 接口后，学生提问将使用您的 API。留空则使用管理员默认配置（需在体验期内）。"
      />
      <el-form :model="aiForm" label-width="120px" label-position="left">
        <el-form-item label="API Key">
          <el-input
            v-model="aiForm.apiKey"
            type="password"
            show-password
            placeholder="已设置，留空则不修改"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input
            v-model="aiForm.baseUrl"
            placeholder="OpenAI 兼容接口地址，如 https://api.deepseek.com"
          />
        </el-form-item>
        <el-form-item label="模型名">
          <el-input
            v-model="aiForm.modelName"
            placeholder="填写对应厂商的模型名，如 deepseek-chat / glm-4 / doubao-seed-1.6"
          />
        </el-form-item>
      </el-form>

      <!-- 管理员默认配置参考 -->
      <div v-if="configLoaded" class="admin-ref">
        <span class="admin-ref-label">管理员默认配置：</span>
        <el-tag v-if="adminApiKeySet" type="success" size="small">已设置</el-tag>
        <el-tag v-else type="info" size="small">未设置</el-tag>
        <span v-if="adminBaseUrl" class="admin-ref-value">{{ adminBaseUrl }} / {{ adminModelName }}</span>
      </div>

      <div class="action-bar">
        <el-button :loading="testing" @click="handleTest">
          <el-icon><Connection /></el-icon>
          测试连接
        </el-button>
        <el-button v-if="hasOwnConfig" type="danger" plain @click="handleClear">
          清除配置
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
import { Connection, Check } from '@element-plus/icons-vue'
import { getTeacherApiConfig, saveTeacherApiConfig, clearTeacherApiConfig, testTeacherApiConfig } from '@/api/setting'

const aiForm = reactive({
  apiKey: '',
  baseUrl: '',
  modelName: ''
})

const saving = ref(false)
const testing = ref(false)
const testResult = ref(null)
const configLoaded = ref(false)
const hasOwnConfig = ref(false)
const inTrialPeriod = ref(false)
const trialStartTime = ref('')
const trialEndTime = ref('')
const adminApiKeySet = ref(false)
const adminBaseUrl = ref('')
const adminModelName = ref('')

async function fetchConfig() {
  try {
    const res = await getTeacherApiConfig()
    const data = res.data || res
    if (data) {
      aiForm.apiKey = data.apiKey || ''
      aiForm.baseUrl = data.baseUrl || ''
      aiForm.modelName = data.modelName || ''
      hasOwnConfig.value = data.hasOwnConfig || false
      inTrialPeriod.value = data.inTrialPeriod || false
      trialStartTime.value = data.trialStartTime || ''
      trialEndTime.value = data.trialEndTime || ''
      adminApiKeySet.value = data.adminApiKeySet || false
      adminBaseUrl.value = data.adminBaseUrl || ''
      adminModelName.value = data.adminModelName || ''
    }
    configLoaded.value = true
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveTeacherApiConfig({ ...aiForm })
    ElMessage.success('设置已保存')
    await fetchConfig()
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
    const res = await testTeacherApiConfig({ ...aiForm })
    testResult.value = res.data || res
  } catch (e) {
    testResult.value = { success: false, message: '测试请求失败，请检查后端服务' }
  } finally {
    testing.value = false
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清除自己的 API 配置吗？清除后将回退到管理员默认配置（需在体验期内）。', '提示', { type: 'warning' })
    await clearTeacherApiConfig()
    ElMessage.success('已清除配置')
    aiForm.apiKey = ''
    aiForm.baseUrl = ''
    aiForm.modelName = ''
    await fetchConfig()
  } catch (e) {
    // 用户取消或错误
  }
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 19)
}

onMounted(fetchConfig)
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

.admin-ref {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  font-size: 13px;
  color: var(--color-text-tertiary);

  .admin-ref-value {
    margin-left: 4px;
  }
}

.save-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.action-bar {
  display: flex;
  gap: 10px;
}
</style>
