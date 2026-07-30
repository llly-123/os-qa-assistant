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

    <!-- Class selection: Grid 卡片布局 -->
    <div v-else class="page-center">
      <div class="grid-container">
        <h1 class="grid-title">欢迎使用 {{ userStore.siteName }}</h1>
        <p class="grid-subtitle">请选择要进入的班级课程</p>

        <div class="course-grid">
          <div
            v-for="c in classes"
            :key="c.id"
            class="course-card"
            @click="enterClassById(c.id)"
          >
            <!-- 封面区：渐变色 + 课程首字 -->
            <div class="course-cover" :style="coverStyle(c)">
              <span class="cover-letter">{{ c.name }}</span>
            </div>
            <!-- 信息区 -->
            <div class="course-info">
              <h3 class="course-name">{{ c.name }}</h3>

              <!-- 时间信息 -->
              <div class="course-meta">
                <svg class="meta-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                  <rect x="1.5" y="3" width="13" height="11" rx="2" stroke="currentColor" stroke-width="1.4"/>
                  <path d="M1.5 6h13M5 1.5v3M11 1.5v3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                </svg>
                <span>{{ formatDateRange(c.startTime, c.endTime) }}</span>
              </div>

              <!-- 资源标签 -->
              <div class="course-tags">
                <span v-if="c.videoSetName" class="tag tag-video">
                  <svg viewBox="0 0 16 16" width="11" height="11" fill="none">
                    <rect x="1" y="3" width="10" height="10" rx="2" stroke="currentColor" stroke-width="1.4"/>
                    <path d="M11 7l4-2v6l-4-2z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                  </svg>
                  {{ c.videoSetName }}
                </span>
                <span v-if="c.kbName" class="tag tag-kb">
                  <svg viewBox="0 0 16 16" width="11" height="11" fill="none">
                    <path d="M2 2h7l4 4v8H2z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                    <path d="M9 2v4h4" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                  </svg>
                  {{ c.kbName }}
                </span>
              </div>
            </div>
          </div>
        </div>
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

// 渐变色调色板：不同课程名映射到不同渐变色
const GRADIENT_PALETTES = [
  ['#667eea', '#764ba2'], // 紫蓝
  ['#f093fb', '#f5576c'], // 粉红
  ['#4facfe', '#00f2fe'], // 青蓝
  ['#43e97b', '#38f9d7'], // 翠绿
  ['#fa709a', '#fee140'], // 橙粉
  ['#a8edea', '#fed6e3'], // 薄荷
  ['#ff9a9e', '#fecfef'], // 暖粉
  ['#ffecd2', '#fcb69f'], // 暖橙
  ['#a18cd1', '#fbc2eb'], // 淡紫
  ['#5ee7df', '#b490ca'], // 青紫
]

// 根据课程 id 选取渐变色，保证不同课程颜色不同
function coverStyle(c) {
  const idx = (c.id || 0) % GRADIENT_PALETTES.length
  const [c1, c2] = GRADIENT_PALETTES[idx]
  return { background: `linear-gradient(135deg, ${c1}, ${c2})` }
}

// 格式化时间范围
function formatDateRange(start, end) {
  const fmt = (d) => {
    if (!d) return ''
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  }
  const s = fmt(start)
  const e = fmt(end)
  if (s && e) return `${s} - ${e}`
  if (s) return `${s} 起`
  return '长期有效'
}

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

// 卡片点击直接进入班级
async function enterClassById(classId) {
  if (!classId) return
  selectedClassId.value = classId
  await enterClass()
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

/* 无班级时的卡片（保留原样式） */
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

/* Grid 卡片布局 */
.grid-container {
  width: 100%;
  max-width: 1100px;
  text-align: center;
}

.grid-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
}

.grid-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 32px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

@media (max-width: 900px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
}

.course-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 24px 56px rgba(0, 0, 0, 0.28);

    .course-cover .cover-letter {
      transform: scale(1.1);
    }

    .course-enter {
      gap: 8px;
    }
  }
}

.course-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 30% 40%, rgba(255, 255, 255, 0.2), transparent 60%);
  }
}

.cover-letter {
  font-size: 22px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
  transition: transform 0.3s ease;
  position: relative;
  z-index: 1;
  padding: 0 16px;
  text-align: center;
  line-height: 1.4;
  word-break: break-all;
}

.course-info {
  padding: 18px 20px 20px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.course-name {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-icon {
  flex-shrink: 0;
  color: #cbd5e1;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  line-height: 1.4;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-video {
  background: #eff6ff;
  color: #3b82f6;
}

.tag-kb {
  background: #f0fdf4;
  color: #22c55e;
}

.course-enter {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #6366f1;
  margin-top: 2px;
  transition: gap 0.2s ease;
}
</style>
