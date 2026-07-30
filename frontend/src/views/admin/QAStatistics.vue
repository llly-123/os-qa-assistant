<template>
  <div class="qa-statistics">
    <div class="page-header">
      <h2>问答统计分析</h2>
      <div class="date-filter">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleDateChange"
        />
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 总体统计 -->
      <el-tab-pane label="总体统计" name="overall">
        <el-row :gutter="20" class="stat-cards">
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon :size="32" color="#409eff"><ChatDotRound /></el-icon>
                <div class="stat-info">
                  <span class="stat-value">{{ overview.totalQuestions || 0 }}</span>
                  <span class="stat-label">总提问数</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon :size="32" color="#67c23a"><User /></el-icon>
                <div class="stat-info">
                  <span class="stat-value">{{ overview.activeUsers || 0 }}</span>
                  <span class="stat-label">活跃用户数</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon :size="32" color="#e6a23c"><Timer /></el-icon>
                <div class="stat-info">
                  <span class="stat-value">{{ overview.avgResponseTime || 0 }}s</span>
                  <span class="stat-label">平均响应时间</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon :size="32" color="#f56c6c"><Document /></el-icon>
                <div class="stat-info">
                  <span class="stat-value">{{ overview.citationRate || 0 }}%</span>
                  <span class="stat-label">引用率</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="24">
            <el-card>
              <template #header><span>高频关键词</span></template>
              <div class="keyword-cloud">
                <el-tag
                  v-for="kw in keywords"
                  :key="kw.word"
                  :size="getTagSize(kw.count)"
                  :type="getTagType(kw.count)"
                  style="margin: 4px"
                >{{ kw.word }} ({{ kw.count }})</el-tag>
                <el-empty v-if="keywords.length === 0" description="暂无数据" :image-size="60" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 班级统计 -->
      <el-tab-pane label="班级统计" name="class">
        <!-- 未选班级：Grid 卡片选择 -->
        <div v-if="!selectedClassId" class="class-grid-section">
          <p class="grid-hint">请选择要查看统计的班级</p>
          <div v-if="classList.length === 0" class="class-empty">
            <el-empty description="暂无班级" />
          </div>
          <div v-else class="class-grid">
            <div
              v-for="cls in classList"
              :key="cls.id"
              class="class-card"
              @click="startClassStatsById(cls.id)"
            >
              <div class="class-card-cover" :style="classCoverStyle(cls)">
                <span class="class-cover-name">{{ cls.name }}</span>
              </div>
              <div class="class-card-info">
                <h4 class="class-card-title">{{ cls.name }}</h4>
                <div class="class-card-meta">
                  <span class="meta-item">
                    <el-icon><User /></el-icon>
                    {{ cls.studentCount || 0 }} 人
                  </span>
                  <span v-if="cls.status !== 1" class="meta-status">已解散</span>
                </div>
                <div class="class-card-enter">
                  <span>查看统计</span>
                  <svg viewBox="0 0 16 16" width="13" height="13" fill="none">
                    <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 已选班级：统计数据 -->
        <template v-else>
          <div class="class-stats-header">
            <div class="class-stats-title">
              <el-icon :size="20"><School /></el-icon>
              <span>{{ currentClassName }} - 班级统计</span>
            </div>
            <el-button size="small" type="primary" class="switch-class-btn" @click="switchClass">
              切换班级
            </el-button>
          </div>

          <el-row :gutter="20" class="stat-cards">
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="stat-card">
                  <el-icon :size="32" color="#409eff"><ChatDotRound /></el-icon>
                  <div class="stat-info">
                    <span class="stat-value">{{ classOverview.totalQuestions || 0 }}</span>
                    <span class="stat-label">提问数</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="stat-card">
                  <el-icon :size="32" color="#67c23a"><User /></el-icon>
                  <div class="stat-info">
                    <span class="stat-value">{{ classOverview.activeUsers || 0 }}</span>
                    <span class="stat-label">活跃用户数</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="stat-card">
                  <el-icon :size="32" color="#e6a23c"><Timer /></el-icon>
                  <div class="stat-info">
                    <span class="stat-value">{{ classOverview.avgResponseTime || 0 }}s</span>
                    <span class="stat-label">平均响应时间</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="stat-card">
                  <el-icon :size="32" color="#f56c6c"><Document /></el-icon>
                  <div class="stat-info">
                    <span class="stat-value">{{ classOverview.citationRate || 0 }}%</span>
                    <span class="stat-label">引用率</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="24">
              <el-card>
                <template #header><span>高频关键词</span></template>
                <div class="keyword-cloud">
                  <el-tag
                    v-for="kw in classKeywords"
                    :key="kw.word"
                    :size="getTagSize(kw.count)"
                    :type="getTagType(kw.count)"
                    style="margin: 4px"
                  >{{ kw.word }} ({{ kw.count }})</el-tag>
                  <el-empty v-if="classKeywords.length === 0" description="暂无提问数据" :image-size="60" />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getQAStatistics, getHotKeywords, getClassList, getClassOverview, getClassHotKeywords } from '@/api/statistics'

const dateRange = ref([])
const activeTab = ref('overall')

// 总体统计
const overview = ref({})
const keywords = ref([])

// 班级统计
const classList = ref([])
const selectedClassId = ref(null)
const classOverview = ref({})
const classKeywords = ref([])
const currentClassName = computed(() => {
  const c = classList.value.find(item => item.id === selectedClassId.value)
  return c ? c.name : ''
})

onMounted(() => {
  fetchOverallData()
  fetchClassList()
})

function getDateParams() {
  const params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = formatDate(dateRange.value[0]) + ' 00:00:00'
    params.endDate = formatDate(dateRange.value[1]) + ' 23:59:59'
  }
  return params
}

function formatDate(date) {
  if (!date) return ''
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function handleDateChange() {
  if (activeTab.value === 'overall') {
    fetchOverallData()
  } else if (selectedClassId.value) {
    fetchClassData(selectedClassId.value)
  }
}

function handleTabChange(tab) {
  if (tab === 'overall') {
    fetchOverallData()
  }
}

async function fetchOverallData() {
  const params = getDateParams()
  try {
    const [statRes, kwRes] = await Promise.all([
      getQAStatistics(params),
      getHotKeywords({ ...params, limit: 30 })
    ])
    overview.value = statRes.data || {}
    keywords.value = kwRes.data || []
  } catch (e) {
    console.error('获取总体统计失败:', e)
  }
}

async function fetchClassList() {
  try {
    const res = await getClassList()
    classList.value = res.data || []
  } catch (e) {
    console.error('获取班级列表失败:', e)
  }
}

// 卡片点击直接进入班级统计
function startClassStatsById(classId) {
  if (!classId) return
  selectedClassId.value = classId
  fetchClassData(classId)
}

// 渐变色调色板
const GRADIENT_PALETTES = [
  ['#667eea', '#764ba2'],
  ['#f093fb', '#f5576c'],
  ['#4facfe', '#00f2fe'],
  ['#43e97b', '#38f9d7'],
  ['#fa709a', '#fee140'],
  ['#a8edea', '#fed6e3'],
  ['#ff9a9e', '#fecfef'],
  ['#ffecd2', '#fcb69f'],
  ['#a18cd1', '#fbc2eb'],
  ['#5ee7df', '#b490ca'],
]

function classCoverStyle(cls) {
  const idx = (cls.id || 0) % GRADIENT_PALETTES.length
  const [c1, c2] = GRADIENT_PALETTES[idx]
  return { background: `linear-gradient(135deg, ${c1}, ${c2})` }
}

function switchClass() {
  selectedClassId.value = null
  classOverview.value = {}
  classKeywords.value = []
}

async function fetchClassData(classId) {
  const params = getDateParams()
  try {
    const [statRes, kwRes] = await Promise.all([
      getClassOverview(classId, params),
      getClassHotKeywords(classId, { ...params, limit: 30 })
    ])
    classOverview.value = statRes.data || {}
    classKeywords.value = kwRes.data || []
  } catch (e) {
    console.error('获取班级统计失败:', e)
  }
}

function getTagSize(count) {
  if (count > 50) return 'large'
  if (count > 20) return 'default'
  return 'small'
}

function getTagType(count) {
  if (count > 50) return 'danger'
  if (count > 20) return 'warning'
  if (count > 10) return 'success'
  return 'info'
}
</script>

<style scoped lang="scss">
.qa-statistics {
  padding: 28px 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
  }
}

.stat-cards {
  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;

    .stat-info {
      display: flex;
      flex-direction: column;

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: var(--color-text-primary);
        line-height: 1.1;
      }

      .stat-label {
        font-size: 14px;
        color: var(--color-text-tertiary);
        margin-top: 2px;
      }
    }
  }
}

.keyword-cloud {
  min-height: 150px;
}

/* 班级 Grid 卡片 */
.class-grid-section {
  padding: 8px 0;
}

.grid-hint {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 20px;
}

.class-empty {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 40px 0;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 1000px) {
  .class-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .class-grid {
    grid-template-columns: 1fr;
  }
}

.class-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15);

    .class-cover-name {
      transform: scale(1.05);
    }

    .class-card-enter {
      gap: 8px;
    }
  }
}

.class-card-cover {
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

.class-cover-name {
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
  padding: 0 16px;
  text-align: center;
  line-height: 1.4;
  word-break: break-all;
  transition: transform 0.3s ease;
  position: relative;
  z-index: 1;
}

.class-card-info {
  padding: 16px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.class-card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.class-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-status {
  color: #f56c6c;
  font-size: 11px;
}

.class-card-enter {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
  transition: gap 0.2s ease;
}

.class-stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.class-stats-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
</style>
