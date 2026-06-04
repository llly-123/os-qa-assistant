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
          @change="fetchAllData"
        />
      </div>
    </div>
    
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
          <template #header>
            <span>高频关键词</span>
          </template>
          <div class="keyword-cloud">
            <el-tag
              v-for="kw in keywords"
              :key="kw.word"
              :size="getTagSize(kw.count)"
              :type="getTagType(kw.count)"
              style="margin: 4px"
            >
              {{ kw.word }} ({{ kw.count }})
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card style="margin-top: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>最近提问记录</span>
          <el-button size="small" @click="fetchAllData">刷新</el-button>
        </div>
      </template>
      
      <el-table :data="recentQuestions" stripe>
        <el-table-column label="学号" width="130">
          <template #default="{ row }">
            {{ row.STUDENTNAME || row.studentName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="姓名" width="100">
          <template #default="{ row }">
            {{ row.STUDENTREALNAME || row.studentRealName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="问题" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.QUESTION || row.question || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="引用" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.CITATION || row.citation" color="#67c23a"><SuccessFilled /></el-icon>
            <el-icon v-else color="#f56c6c"><CircleClose /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.CREATE_TIME || row.create_time) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getQAStatistics, getHotKeywords, getRecentQuestions } from '@/api/statistics'

const dateRange = ref([])
const overview = ref({})
const keywords = ref([])
const recentQuestions = ref([])

onMounted(() => {
  fetchAllData()
})

function getDateParams() {
  if (dateRange.value && dateRange.value.length === 2) {
    return {
      startDate: new Date(dateRange.value[0]).toISOString().slice(0, 19).replace('T', ' '),
      endDate: new Date(dateRange.value[1]).toISOString().slice(0, 19).replace('T', ' ')
    }
  }
  return {}
}

async function fetchAllData() {
  const params = getDateParams()
  await Promise.all([
    fetchStatistics(params),
    fetchKeywords(params),
    fetchRecentQuestions(params)
  ])
}

async function fetchStatistics(params) {
  try {
    const res = await getQAStatistics(params)
    overview.value = res.data || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

async function fetchKeywords(params) {
  try {
    const res = await getHotKeywords({ ...params, limit: 30 })
    keywords.value = res.data || []
  } catch (error) {
    console.error('获取关键词失败:', error)
  }
}

async function fetchRecentQuestions(params) {
  try {
    const res = await getRecentQuestions({ ...params, limit: 20 })
    recentQuestions.value = res.data || []
  } catch (error) {
    console.error('获取最近提问失败:', error)
  }
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
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
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h2 {
    margin: 0;
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
        font-weight: 600;
        color: #303133;
      }
      
      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }
}

.keyword-cloud {
  min-height: 150px;
}
</style>
