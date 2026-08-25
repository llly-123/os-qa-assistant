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
        title="回答学生问题所用的大模型接口。留空则使用服务器默认配置。"
      />
      <el-form :model="aiForm" label-width="120px" label-position="left">
        <el-form-item label="API Key">
          <el-input
            v-model="aiForm.ai_api_key"
            type="password"
            show-password
            placeholder="留空使用默认 Key"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input
            v-model="aiForm.ai_base_url"
            placeholder="如 https://api.deepseek.com（OpenAI 兼容接口）"
          />
        </el-form-item>
        <el-form-item label="模型名">
          <el-input
            v-model="aiForm.ai_model_name"
            placeholder="如 deepseek-v4-pro"
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
import { ElMessage } from 'element-plus'
import { Connection, Check } from '@element-plus/icons-vue'
import { getSettings, updateSettings, testAi } from '@/api/setting'

const aiForm = reactive({
  ai_api_key: '',
  ai_base_url: '',
  ai_model_name: ''
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
    }
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateSettings({
      ...aiForm
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
