<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>系统设置</h2>
      <p class="page-desc">配置站点名称、课程名称与学校名称，将应用于登录页、侧边栏与 AI 答疑提示词。</p>
    </div>

    <el-card class="settings-card" v-loading="loading">
      <el-form :model="form" label-width="120px" class="settings-form">
        <el-form-item label="站点名称">
          <el-input v-model="form.site_name" placeholder="如：智能答疑助手" maxlength="50" show-word-limit />
          <div class="field-hint">显示在浏览器标题与侧边栏 Logo。</div>
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="form.course_name" placeholder="如：操作系统、数据结构" maxlength="50" show-word-limit />
          <div class="field-hint">用于 AI 答疑提示词与联网搜索范围，建议填写实际课程名。</div>
        </el-form-item>
        <el-form-item label="学校 / 院系">
          <el-input v-model="form.school_name" placeholder="可选，如：西安电子科技大学" maxlength="100" show-word-limit />
          <div class="field-hint">用于 AI 提示词中的归属描述，留空则不显示。</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
          <el-button @click="loadSettings">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSettings, updateSettings } from '@/api/setting'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  site_name: '',
  course_name: '',
  school_name: ''
})

async function loadSettings() {
  loading.value = true
  try {
    const res = await getSettings()
    const data = res.data || res || {}
    form.site_name = data.site_name || ''
    form.course_name = data.course_name || ''
    form.school_name = data.school_name || ''
  } catch (e) {
    ElMessage.error('加载设置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.site_name.trim()) {
    ElMessage.warning('站点名称不能为空')
    return
  }
  if (!form.course_name.trim()) {
    ElMessage.warning('课程名称不能为空')
    return
  }
  saving.value = true
  try {
    await updateSettings({
      site_name: form.site_name.trim(),
      course_name: form.course_name.trim(),
      school_name: form.school_name.trim()
    })
    ElMessage.success('保存成功')
    // 刷新前端缓存的公开设置，使登录页/侧边栏立即生效
    await userStore.fetchPublicSettings()
    document.title = userStore.siteName
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped lang="scss">
.settings-page {
  padding: 24px;
  max-width: 760px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    margin: 0 0 6px;
    font-size: 22px;
    color: var(--color-text-primary);
  }

  .page-desc {
    margin: 0;
    color: var(--color-text-tertiary);
    font-size: 13px;
  }
}

.settings-card {
  border-radius: 12px;
}

.settings-form {
  max-width: 560px;
}

.field-hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
  line-height: 1.5;
}
</style>
