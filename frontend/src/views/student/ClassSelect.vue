<template>
  <div class="select-page">
    <!-- Decorative orbs -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>

    <!-- Loading -->
    <div v-if="loading" class="page-center">
      <el-icon class="is-loading" :size="36" color="#fff"><Loading /></el-icon>
    </div>

    <!-- No classes -->
    <div v-else-if="classes.length === 0" class="page-center">
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

    <!-- Class selection -->
    <div v-else class="page-center">
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
        <div class="divider"></div>

        <el-select
          v-model="selectedClassId"
          placeholder="请选择班级"
          size="large"
          class="class-select"
        >
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>

        <el-button
          type="primary"
          size="large"
          class="enter-btn"
          :disabled="!selectedClassId"
          @click="enterClass"
        >
          进入班级
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()

const loading = ref(true)
const selectedClassId = ref(null)
const classes = computed(() => chatStore.classes)

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
</script>

<style scoped lang="scss">
.select-page {
  position: relative;
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 40%, #3730a3 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
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

.page-center {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 24px;
}

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

.divider {
  height: 1px;
  background: #e2e8f0;
  margin: 24px 0;
}

.class-select {
  width: 100%;
}

:deep(.class-select .el-select__wrapper) {
  border-radius: 10px;
  min-height: 48px;
}

.enter-btn {
  width: 100%;
  margin-top: 16px;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.05em;
}
</style>
