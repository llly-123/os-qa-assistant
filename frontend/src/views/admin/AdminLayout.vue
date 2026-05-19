<template>
  <div class="admin-layout">
    <div class="admin-sidebar">
      <div class="admin-header">
        <el-icon :size="24"><Setting /></el-icon>
        <span>管理后台</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        router
      >
        <el-menu-item index="/admin/students">
          <el-icon><User /></el-icon>
          <span>学生管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/statistics">
          <el-icon><DataLine /></el-icon>
          <span>问答统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/knowledge">
          <el-icon><FolderOpened /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
      </el-menu>
      
      <div class="admin-footer">
        <el-button type="text" @click="goBack">
          <el-icon><Back /></el-icon>
          返回聊天
        </el-button>
      </div>
    </div>
    
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

function goBack() {
  router.push('/chat')
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100%;
}

.admin-sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.admin-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #e4e7ed;
}

.admin-menu {
  flex: 1;
  border-right: none;
}

.admin-footer {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
}

.admin-content {
  flex: 1;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
