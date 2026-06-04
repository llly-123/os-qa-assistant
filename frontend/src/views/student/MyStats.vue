<template>
  <div class="my-stats">
    <div class="stats-header">
      <h2>我的学习统计</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="40" color="#409eff"><ChatDotRound /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalQuestions || 0 }}</div>
              <div class="stat-label">累计提问</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="40" color="#67c23a"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.citationRate || 0 }}%</div>
              <div class="stat-label">引用教材率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>我的提问热词</span>
      </template>
      <div v-if="keywords.length > 0" class="keyword-cloud">
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
      <el-empty v-else description="暂无提问记录，快去提问吧！" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyStats } from '@/api/chat'

const stats = ref({})
const keywords = ref([])

onMounted(async () => {
  try {
    const res = await getMyStats()
    if (res.data) {
      stats.value = {
        totalQuestions: res.data.totalQuestions || 0,
        citationRate: res.data.citationRate || 0
      }
      keywords.value = res.data.keywords || []
    }
  } catch (error) {
    console.error('获取统计失败:', error)
  }
})

function getTagSize(count) {
  if (count >= 5) return 'large'
  if (count >= 3) return 'default'
  return 'small'
}

function getTagType(count) {
  if (count >= 5) return 'danger'
  if (count >= 3) return 'warning'
  return 'info'
}
</script>

<style scoped>
.my-stats {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
}

.stats-header {
  margin-bottom: 20px;
}

.stats-header h2 {
  margin: 0;
  color: #303133;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
