<template>
  <div class="student-home">
    <ChatView v-if="chatStore.currentClassId" />
    <div v-else class="redirect-notice">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>正在跳转...</span>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import ChatView from '@/views/student/Chat.vue'

const router = useRouter()
const chatStore = useChatStore()

onMounted(() => {
  if (!chatStore.currentClassId) {
    router.replace('/select-class')
  }
})
</script>

<style scoped>
.student-home { height: 100%; }
.redirect-notice {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--color-text-tertiary);
  font-size: 14px;
}
</style>
