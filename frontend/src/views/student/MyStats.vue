<template>
  <div class="my-stats">
    <div class="page-header">
      <h2>我的学习统计</h2>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: #eef2ff; color: #6366f1">
          <el-icon :size="28"><ChatDotRound /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.totalQuestions || 0 }}</span>
          <span class="stat-label">累计提问</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #ecfdf5; color: #10b981">
          <el-icon :size="28"><Document /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.citationRate || 0 }}%</span>
          <span class="stat-label">引用教材率</span>
        </div>
      </div>
    </div>

    <div class="section-card">
      <div class="section-title">我的提问热词</div>
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
      <el-empty v-else description="暂无提问记录，快去提问吧！" :image-size="60" />
    </div>
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

<style scoped lang="scss">
.my-stats {
  padding: 28px 32px;
  height: 100%;
  overflow-y: auto;
}

.page-header {
  margin-bottom: 28px;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: var(--shadow-md);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.1;
}

.stat-label {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.section-card {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 16px;
  }
}

.keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
