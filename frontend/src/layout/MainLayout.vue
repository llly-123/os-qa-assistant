<template>
  <div class="main-layout">
    <el-container class="layout-container">
      <el-aside width="260px" class="sidebar">
        <div class="sidebar-header">
          <div class="logo-icon">
            <el-icon :size="22" color="#fff"><Reading /></el-icon>
          </div>
          <span class="title">OS AI助手</span>
        </div>
        
        <div class="sidebar-content">
          <div class="new-chat-btn">
            <el-button type="primary" @click="createNewSession" style="width: 100%">
              <el-icon><Plus /></el-icon>
              新建对话
            </el-button>
          </div>
          
          <div class="session-list">
            <div 
              v-for="session in sessions" 
              :key="session.id"
              :class="['session-item', { active: currentSessionId === session.id }]"
              @click="selectSession(session.id)"
            >
              <el-icon><ChatDotRound /></el-icon>
              <span class="session-title">{{ session.title }}</span>
              <el-dropdown trigger="click" @command="handleSessionCommand($event, session)">
                <el-icon class="more-icon"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">重命名</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
        
        <div class="sidebar-footer">
          <div class="user-info">
            <el-avatar :size="32" :icon="role === 'TEACHER' ? 'UserFilled' : 'User'" />
            <div class="user-detail">
              <span class="username">{{ username }}</span>
              <el-tag :type="role === 'TEACHER' ? 'warning' : 'success'" size="small">
                {{ role === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </div>
          </div>
          
          <div class="action-btns">
            <el-button 
              v-if="role === 'TEACHER'" 
              type="text" 
              @click="goToAdmin"
            >
              <el-icon><Setting /></el-icon>
              管理后台
            </el-button>
            <el-button type="text" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </div>
      </el-aside>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
    
    <el-dialog v-model="renameDialogVisible" title="重命名对话" width="400px">
      <el-input v-model="newTitle" placeholder="请输入新的对话标题" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const renameDialogVisible = ref(false)
const newTitle = ref('')
const renamingSession = ref(null)

const sessions = computed(() => chatStore.sessions)
const currentSessionId = computed(() => chatStore.currentSessionId)
const role = computed(() => userStore.role)
const username = computed(() => userStore.username)

onMounted(async () => {
  await chatStore.fetchSessions()
})

async function createNewSession() {
  await chatStore.createSession()
}

async function selectSession(sessionId) {
  await chatStore.fetchMessages(sessionId)
}

function handleSessionCommand(command, session) {
  if (command === 'rename') {
    renamingSession.value = session
    newTitle.value = session.title
    renameDialogVisible.value = true
  } else if (command === 'delete') {
    ElMessageBox.confirm('确定要删除这个对话吗？', '提示', {
      type: 'warning'
    }).then(async () => {
      await chatStore.deleteSession(session.id)
      ElMessage.success('删除成功')
    }).catch(() => {})
  }
}

async function confirmRename() {
  if (!newTitle.value.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  await chatStore.updateSessionTitle(renamingSession.value.id, newTitle.value)
  renameDialogVisible.value = false
  ElMessage.success('重命名成功')
}

function goToAdmin() {
  router.push('/admin/students')
}

async function handleLogout() {
  try {
    await userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  } catch (error) {
    console.error('退出失败:', error)
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

.layout-container {
  height: 100%;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  flex-direction: column;
  border-right: none;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
}

.sidebar-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  
  .logo-icon {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  }
  
  .title {
    font-size: 18px;
    font-weight: 700;
    color: #e8e8f0;
    letter-spacing: 0.5px;
  }
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.new-chat-btn {
  margin-bottom: 16px;
  
  .el-button {
    border-radius: 10px;
    height: 40px;
    font-size: 14px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    
    &:hover {
      opacity: 0.9;
      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    }
  }
}

.session-list {
  .session-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    border-radius: 10px;
    cursor: pointer;
    color: #9ca3c0;
    transition: all 0.25s ease;
    margin-bottom: 4px;
    
    &:hover {
      background: rgba(255, 255, 255, 0.08);
      color: #e8e8f0;
    }
    
    &.active {
      background: rgba(102, 126, 234, 0.2);
      color: #e8e8f0;
      border-left: 3px solid #667eea;
    }
    
    .session-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
    }
    
    .more-icon {
      opacity: 0;
      transition: opacity 0.2s;
    }
    
    &:hover .more-icon {
      opacity: 1;
    }
  }
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    
    .el-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    
    .user-detail {
      display: flex;
      flex-direction: column;
      gap: 4px;
      
      .username {
        font-size: 14px;
        color: #e8e8f0;
        font-weight: 500;
      }
    }
  }
  
  .action-btns {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .el-button {
      justify-content: flex-start;
      color: #9ca3c0;
      border-radius: 8px;
      
      &:hover {
        color: #e8e8f0;
        background: rgba(255, 255, 255, 0.05);
      }
    }
  }
}

.main-content {
  background: #f0f2f5;
  padding: 0;
}
</style>
