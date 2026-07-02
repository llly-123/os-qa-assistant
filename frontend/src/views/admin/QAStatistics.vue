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
        <div class="class-selector">
          <span class="selector-label">选择班级：</span>
          <el-select v-model="selectedClassId" placeholder="请选择班级" @change="handleClassChange" style="width: 280px">
            <el-option
              v-for="cls in classList"
              :key="cls.id"
              :value="cls.id"
              :label="cls.name + (cls.status === 1 ? '' : '（已解散）') + ' - ' + (cls.studentCount || 0) + '人'"
            />
          </el-select>
        </div>

        <template v-if="selectedClassId">
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

        <el-empty v-else description="请选择一个班级查看统计" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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

async function handleClassChange(classId) {
  if (!classId) return
  fetchClassData(classId)
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

.class-selector {
  margin-bottom: 20px;
  display: flex;
  align-items: center;

  .selector-label {
    font-size: 15px;
    color: var(--color-text-secondary);
    margin-right: 8px;
    white-space: nowrap;
  }
}
</style>
