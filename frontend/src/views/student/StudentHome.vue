<template>
  <div class="home-container">
    <!-- 加载中 -->
    <div v-if="loading" class="home-center">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>

    <!-- 未加入任何班级 -->
    <div v-else-if="classes.length === 0" class="home-center">
      <div class="home-card">
        <h2>欢迎使用 {{ userStore.siteName }}</h2>
        <p>您还未加入任何班级，请等待教师将您加入班级后即可开始学习</p>
      </div>
    </div>

    <!-- 已有班级但未选择 -->
    <div v-else-if="!chatStore.currentClassId" class="home-center">
      <div class="home-card">
        <h2>选择班级开始学习</h2>
        <p>我是《{{ userStore.courseName }}》课程的 AI 答疑助手</p>
        <div class="class-select-area">
          <el-select
            v-model="selectedClassId"
            placeholder="请选择班级"
            size="large"
            style="width: 260px"
          >
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button type="primary" size="large" :disabled="!selectedClassId" @click="enterClass">
            进入班级
          </el-button>
        </div>
      </div>
    </div>

    <!-- 已选择班级，显示聊天界面 -->
    <ChatView v-else />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import ChatView from '@/views/student/Chat.vue'

const chatStore = useChatStore()
const userStore = useUserStore()

const loading = ref(true)
const selectedClassId = ref(null)
const classes = computed(() => chatStore.classes)

onMounted(async () => {
  // 如果有保存的班级ID，先验证班级列表
  if (!chatStore.classes.length) {
    await chatStore.fetchClasses()
  }
  loading.value = false
})

async function enterClass() {
  if (!selectedClassId.value) return
  await chatStore.setCurrentClass(selectedClassId.value)
  await chatStore.createSession()
}
</script>

<style scoped lang="scss">
.home-container {
  height: 100%;
  width: 100%;
}

.home-center {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.home-card {
  text-align: center;
  padding: 40px 36px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  max-width: 400px;
  width: 90%;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: var(--color-text-tertiary);
    line-height: 1.6;
    margin-bottom: 24px;
  }
}

.class-select-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;

  .el-button {
    width: 260px;
    height: 40px;
    border-radius: var(--radius-sm);
    font-weight: 500;
  }
}
</style>
