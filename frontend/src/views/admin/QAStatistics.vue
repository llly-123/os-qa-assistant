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
        <!-- 未选班级：居中卡片 -->
        <div v-if="!selectedClassId" class="class-select-wrapper">
          <div class="class-select-card">
            <div class="card-icon-wrap">
              <el-icon :size="40" color="#6366f1"><DataAnalysis /></el-icon>
            </div>
            <h2>班级统计</h2>
            <p class="card-desc">请选择要查看统计的班级</p>
            <div class="card-divider"></div>
            <el-select v-model="classSelectTemp" placeholder="请选择班级" size="large" class="card-select">
              <el-option
                v-for="cls in classList"
                :key="cls.id"
                :value="cls.id"
                :label="cls.name + (cls.status === 1 ? '' : '（已解散）') + ' - ' + (cls.studentCount || 0) + '人'"
              />
            </el-select>
            <el-button type="primary" size="large" class="card-btn" :disabled="!classSelectTemp" @click="startClassStats">
              开始统计
            </el-button>
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
const classSelectTemp = ref(null)
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
    params.startDate = formatDate(dateRange.value[0])
    params.endDate = formatDate(dateRange.value[1])
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

function startClassStats() {
  if (!classSelectTemp.value) return
  selectedClassId.value = classSelectTemp.value
  fetchClassData(selectedClassId.value)
}

function switchClass() {
  selectedClassId.value = null
  classSelectTemp.value = null
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

.class-select-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
}

.class-select-card {
  background: #fff;
  border-radius: 20px;
  padding: 48px 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
  max-width: 420px;
  width: 100%;
  text-align: center;
}

.card-icon-wrap {
  margin-bottom: 20px;
  display: inline-flex;
  padding: 16px;
  background: rgba(99,102,241,0.08);
  border-radius: 16px;
}

.class-select-card h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
}

.card-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.card-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 24px 0;
}

.card-select {
  width: 100%;
}

:deep(.card-select .el-select__wrapper) {
  border-radius: 10px;
  min-height: 48px;
}

.card-btn {
  width: 100%;
  margin-top: 16px;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
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
