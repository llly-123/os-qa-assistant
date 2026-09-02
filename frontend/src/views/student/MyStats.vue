<template>
  <div class="my-stats">
    <div class="page-header">
      <h2>我的学习统计</h2>
      <p v-if="currentClassName" class="page-sub">当前班级：{{ currentClassName }}</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: #eff6ff; color: #2563eb">
          <el-icon :size="28"><ChatDotRound /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.totalQuestions || 0 }}</span>
          <span class="stat-label">累计提问</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #eff6ff; color: #2563eb">
          <el-icon :size="28"><Document /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.citationRate || 0 }}%</span>
          <span class="stat-label">引用资料率</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fef3c7; color: #f59e0b">
          <el-icon :size="28"><Timer /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ studyTime.value }}</span>
          <span class="stat-label">{{ studyTime.label }}</span>
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

    <!-- 1. 提问趋势（次数） -->
    <div class="section-card">
      <div class="section-head">
        <div class="section-title">提问趋势</div>
        <div class="trend-switch">
          <button
            :class="['switch-btn', { active: trendGranularity === 'daily' }]"
            @click="switchTrend('daily')"
          >每日</button>
          <button
            :class="['switch-btn', { active: trendGranularity === 'weekly' }]"
            @click="switchTrend('weekly')"
          >每周</button>
        </div>
      </div>
      <div class="bar-chart">
        <div class="bar-track" :ref="setTrendBarRef">
          <div
            v-for="(item, idx) in displayTrendData"
            :key="idx"
            class="bar-col"
          >
            <div class="bar-wrap">
              <div
                class="bar-fill count-bar"
                :style="{ height: barHeight(item.count, trendMax) + '%' }"
              >
                <span class="bar-tip">{{ trendLabel(item) }}：{{ item.count }} 次</span>
              </div>
            </div>
            <span class="bar-x">{{ trendXLabel(item) }}</span>
          </div>
        </div>
        <div class="trend-footer">
          <div v-if="trendMax > 0" class="bar-y-hint">最多 {{ trendMax }} 次</div>
          <button
            v-if="trendGranularity === 'weekly' && trendData.length > 8"
            class="toggle-weeks-btn"
            @click="showAllWeeks = !showAllWeeks"
          >{{ showAllWeeks ? '收起' : `查看全部 ${trendData.length} 周` }}</button>
        </div>
      </div>
    </div>

    <!-- 1.1 学习时长趋势 -->
    <div class="section-card">
      <div class="section-head">
        <div class="section-title">学习时长趋势</div>
        <div class="trend-switch">
          <button
            :class="['switch-btn', { active: trendGranularity === 'daily' }]"
            @click="switchTrend('daily')"
          >每日</button>
          <button
            :class="['switch-btn', { active: trendGranularity === 'weekly' }]"
            @click="switchTrend('weekly')"
          >每周</button>
        </div>
      </div>
      <div class="bar-chart">
        <div class="bar-track" :ref="setTrendBarRef">
          <div
            v-for="(item, idx) in displayTrendData"
            :key="idx"
            class="bar-col"
          >
            <div class="bar-wrap">
              <div
                class="bar-fill study-bar"
                :style="{ height: barHeight(item.studySeconds, trendStudyMax) + '%' }"
              >
                <span class="bar-tip">{{ trendLabel(item) }}：学习 {{ formatStudy(item.studySeconds) }}</span>
              </div>
            </div>
            <span class="bar-x">{{ trendXLabel(item) }}</span>
          </div>
        </div>
        <div class="trend-footer">
          <div v-if="trendStudyMax > 0" class="bar-y-hint">最多 {{ formatStudy(trendStudyMax) }}</div>
          <button
            v-if="trendGranularity === 'weekly' && trendData.length > 8"
            class="toggle-weeks-btn"
            @click="showAllWeeks = !showAllWeeks"
          >{{ showAllWeeks ? '收起' : `查看全部 ${trendData.length} 周` }}</button>
        </div>
      </div>
    </div>

    <!-- 2. 会话轮次统计 -->
    <div class="section-card">
      <div class="section-title">会话轮次统计</div>
      <div class="mini-card-grid">
        <div class="mini-card">
          <span class="mini-value">{{ sessionData.avgRounds || 0 }}</span>
          <span class="mini-label">平均轮次</span>
        </div>
        <div class="mini-card">
          <span class="mini-value">{{ sessionData.totalSessions || 0 }}</span>
          <span class="mini-label">总会话数</span>
        </div>
      </div>
      <div v-if="sessionRows.length > 0" class="data-table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 60px">排名</th>
              <th style="width: 90px">轮次</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in sessionRows" :key="pickKey(row, 'sessionId', 'SESSIONID') || idx">
              <td>{{ idx + 1 }}</td>
              <td><span class="round-badge">{{ pickNum(row, 'rounds', 'ROUNDS') }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <el-empty v-else description="近期暂无会话记录" :image-size="60" />
    </div>

    <!-- 3. 提问来源分布 -->
    <div class="section-card">
      <div class="section-title">提问来源分布</div>
      <div v-if="sourceItems.length > 0" class="source-list">
        <div v-for="item in sourceItems" :key="item.source" class="source-row">
          <div class="source-meta">
            <span class="source-name">{{ sourceLabel(item.source) }}</span>
            <span class="source-count">{{ item.count }} 次 · {{ sourcePercent(item.count) }}%</span>
          </div>
          <div class="source-track">
            <div
              class="source-fill"
              :style="{
                width: sourcePercent(item.count) + '%',
                background: sourceColor(item.source)
              }"
            ></div>
          </div>
        </div>
      </div>
      <el-empty v-else description="近期暂无来源数据" :image-size="60" />
    </div>

    <!-- 3.1 提问时段分布 -->
    <div class="section-card">
      <div class="section-title">提问时段分布</div>
      <p v-if="hourlyMax === 0" class="hour-hint">近期暂无提问数据</p>
      <div v-else class="hour-chart">
        <div class="hour-track">
          <div v-for="(h, idx) in hourlyData" :key="idx" class="hour-col">
            <div class="hour-bar-wrap">
              <div
                class="hour-bar"
                :style="{ height: barHeight(h.count, hourlyMax) + '%' }"
              >
                <span class="hour-tip">{{ h.hour }} 点：{{ h.count }} 次</span>
              </div>
            </div>
            <span class="hour-x">{{ idx % 3 === 0 ? h.hour : '' }}</span>
          </div>
        </div>
        <div class="hour-y-hint">一天中提问次数最多的时段</div>
      </div>
    </div>

    <!-- 4. 活跃天数 / 连续学习 -->
    <div class="section-card">
      <div class="section-title">活跃天数 / 连续学习</div>
      <div class="mini-card-grid">
        <div class="mini-card">
          <span class="mini-value">{{ activeData.activeDays || 0 }}</span>
          <span class="mini-label">活跃天数</span>
        </div>
        <div class="mini-card">
          <span class="mini-value">{{ activeData.maxStreak || 0 }}</span>
          <span class="mini-label">最长连续学习天数</span>
        </div>
      </div>
      <div class="heatmap-wrap">
        <div class="heatmap-label">近 30 天活跃热力图（<span class="study-dot"></span> 表示该日有学习时长）</div>
        <div class="heatmap-grid">
          <div
            v-for="(day, idx) in heatmapDays"
            :key="idx"
            class="heat-cell"
            :class="{ active: day.active, studied: day.studySeconds > 0 }"
            :title="day.date + (day.active ? '（有提问' : '（无提问') + (day.studySeconds > 0 ? '，学习 ' + formatStudy(day.studySeconds) + '）' : '）')"
          >
            <span class="heat-day">{{ day.label }}</span>
            <span v-if="day.studySeconds > 0" class="study-marker"></span>
          </div>
        </div>
        <div class="heatmap-legend">
          <span class="legend-text">少</span>
          <span class="legend-cell"></span>
          <span class="legend-cell active"></span>
          <span class="legend-text">多</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import {
  getMyStats,
  getMyTrend,
  getMySessionRounds,
  getMySourceDistribution,
  getMyActiveDays,
  getMyHours,
  getMyStudyTime
} from '@/api/chat'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const stats = ref({})
const keywords = ref([])
const currentClassName = computed(() => {
  const c = chatStore.classes.find(c => c.id === chatStore.currentClassId)
  return c ? c.name : ''
})

// ===== 学习时长 =====
const studyTime = ref({ value: 0, label: '学习时长(分钟)' })

// ===== 新增维度数据 =====
const trendGranularity = ref('daily')
const trendData = ref([])
const showAllWeeks = ref(false)
// 提问趋势/学习时长趋势横向滚动容器
const trendBarRefs = ref([])
// 同一 ref 名复用在多个元素上会得到单个元素而非数组，这里用函数 ref 收集成数组
function setTrendBarRef(el) {
  if (el && !trendBarRefs.value.includes(el)) {
    trendBarRefs.value.push(el)
  }
}
// 趋势数据更新或展开/收起周数后，自动滚动到最右侧（最新日期）
watch([trendData, showAllWeeks], () => {
  nextTick(() => {
    trendBarRefs.value.forEach((el) => {
      if (el && el.scrollWidth > el.clientWidth) {
        el.scrollLeft = el.scrollWidth
      }
    })
  })
})
const sessionData = ref({})
const sessionRows = ref([])
const sourceItems = ref([])
const activeData = ref({ activeDays: 0, maxStreak: 0, dates: [] })
const hourlyData = ref([])
const hourlyMax = computed(() => Math.max(0, ...hourlyData.value.map(h => h.count || 0)))

// 来源类型配置
const SOURCE_CONFIG = {
  textbook: { label: '教材', color: 'linear-gradient(90deg, #2563eb, #3b82f6)' },
  web: { label: '网络', color: 'linear-gradient(90deg, #0ea5e9, #38bdf8)' },
  no_class: { label: '未进班级', color: 'linear-gradient(90deg, #f59e0b, #fbbf24)' },
  unknown: { label: '未知', color: 'linear-gradient(90deg, #94a3b8, #cbd5e1)' }
}

// 取值容错：H2 可能返回大写键，也可能返回小写别名
function pickKey(obj, ...keys) {
  if (!obj) return undefined
  for (const k of keys) {
    if (obj[k] !== undefined && obj[k] !== null) return obj[k]
  }
  return undefined
}
function pickNum(obj, ...keys) {
  const v = pickKey(obj, ...keys)
  return v === undefined ? 0 : Number(v)
}

async function loadStats() {
  try {
    const res = await getMyStats(chatStore.currentClassId)
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
  // 新维度与班级联动：切换班级时一并刷新
  await Promise.all([
    loadTrend(),
    loadSessionRounds(),
    loadSourceDistribution(),
    loadHourly(),
    loadActiveDays(),
    loadStudyTime()
  ])
}

// 提问时段分布（按小时 0-23）
async function loadHourly() {
  try {
    const res = await getMyHours({ classId: chatStore.currentClassId })
    const dist = (res.data && res.data.distribution) || []
    const map = {}
    for (const it of dist) {
      map[Number(it.hour || it.HOUR)] = Number(it.count || it.COUNT) || 0
    }
    hourlyData.value = Array.from({ length: 24 }, (_, h) => ({ hour: h, count: map[h] || 0 }))
  } catch (e) {
    console.error('获取时段分布失败:', e)
    hourlyData.value = Array.from({ length: 24 }, (_, h) => ({ hour: h, count: 0 }))
  }
}

// 学习时长（当前班级累计）
async function loadStudyTime() {
  try {
    const res = await getMyStudyTime({ classId: chatStore.currentClassId })
    const totalSeconds = Number((res.data && res.data.totalSeconds) || 0)
    const hours = Math.floor(totalSeconds / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    if (hours > 0) {
      studyTime.value = { value: `${hours}小时${minutes}分钟`, label: '学习时长' }
    } else {
      studyTime.value = { value: `${minutes}`, label: '学习时长(分钟)' }
    }
  } catch (e) {
    console.error('获取学习时长失败:', e)
    studyTime.value = { value: 0, label: '学习时长(分钟)' }
  }
}

// 提问趋势
async function loadTrend() {
  try {
    const res = await getMyTrend({ granularity: trendGranularity.value, classId: chatStore.currentClassId })
    const raw = (res.data && res.data.trend) || []
    const studyRaw = (res.data && res.data.studyTrend) || []
    // 获取当前班级创建时间作为起始日期
    const currentClass = chatStore.classes.find(c => c.id === chatStore.currentClassId)
    let startDate = null
    if (currentClass && currentClass.startTime) {
      startDate = new Date(currentClass.startTime)
    } else {
      // 没有班级信息时默认30天前
      startDate = new Date()
      startDate.setDate(startDate.getDate() - 29)
    }
    const today = new Date()
    today.setHours(23, 59, 59, 999)

    const studyMap = buildStudyMap(studyRaw, trendGranularity.value)
    if (trendGranularity.value === 'daily') {
      trendData.value = fillDailyGaps(raw, startDate, today).map(it => ({
        ...it,
        studySeconds: studyMap[it.date] || 0
      }))
    } else {
      trendData.value = fillWeeklyGaps(raw, startDate, today).map(it => ({
        ...it,
        studySeconds: studyMap[it.week] || 0
      }))
    }
  } catch (e) {
    console.error('获取提问趋势失败:', e)
    trendData.value = []
  }
}

// 将学习时长趋势原始数据（date/week + studySeconds）转为 {归一化日期: 秒数}
function buildStudyMap(studyRaw, granularity) {
  const map = {}
  for (const item of studyRaw) {
    const rawKey = pickKey(item, 'week', 'WEEK') || pickKey(item, 'date', 'DATE')
    if (!rawKey) continue
    const d = new Date(rawKey)
    if (isNaN(d.getTime())) continue
    const key = granularity === 'weekly'
      ? fmtDateLocal(toMondayLocal(d))
      : fmtDateLocal(d)
    map[key] = pickNum(item, 'studySeconds', 'STUDYSECONDS')
  }
  return map
}

function toMondayLocal(date) {
  const d = new Date(date)
  const dayOfWeek = (d.getDay() + 6) % 7 // 周一=0, 周日=6
  d.setDate(d.getDate() - dayOfWeek)
  d.setHours(0, 0, 0, 0)
  return d
}

function fmtDateLocal(d) {
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

// 秒数格式化为 "X小时Y分钟" / "Y分钟" / "小于1分钟"
function formatStudy(seconds) {
  const s = Number(seconds) || 0
  if (s <= 0) return '0分钟'
  if (s < 60) return '小于1分钟'
  const hours = Math.floor(s / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  if (hours > 0) {
    return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`
  }
  return `${minutes}分钟`
}

// 补全每日缺失日期
function fillDailyGaps(raw, startDate, endDate) {
  const map = {}
  for (const item of raw) {
    const d = pickKey(item, 'date', 'DATE')
    if (d) map[d] = pickNum(item, 'count', 'COUNT')
  }
  const result = []
  const cur = new Date(startDate)
  cur.setHours(0, 0, 0, 0)
  const fmt = (d) => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
  while (cur <= endDate) {
    const key = fmt(cur)
    result.push({ date: key, count: map[key] || 0 })
    cur.setDate(cur.getDate() + 1)
  }
  return result
}

// 补全每周缺失周
function fillWeeklyGaps(raw, startDate, endDate) {
  // 将任意日期归一化到所在周的周一
  function toMonday(date) {
    const d = new Date(date)
    const dayOfWeek = (d.getDay() + 6) % 7 // 周一=0, 周日=6
    d.setDate(d.getDate() - dayOfWeek)
    d.setHours(0, 0, 0, 0)
    return d
  }
  const map = {}
  for (const item of raw) {
    const w = pickKey(item, 'week', 'WEEK')
    if (w) {
      const monday = toMonday(new Date(w))
      const key = monday.getFullYear() + '-' + String(monday.getMonth() + 1).padStart(2, '0') + '-' + String(monday.getDate()).padStart(2, '0')
      map[key] = pickNum(item, 'count', 'COUNT')
    }
  }
  const result = []
  const cur = toMonday(startDate)
  const fmt = (d) => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
  while (cur <= endDate) {
    const key = fmt(cur)
    result.push({ week: key, count: map[key] || 0 })
    cur.setDate(cur.getDate() + 7)
  }
  return result
}

function switchTrend(g) {
  if (trendGranularity.value === g) return
  trendGranularity.value = g
  showAllWeeks.value = false
  loadTrend()
}

const trendMax = computed(() => {
  let m = 0
  for (const item of trendData.value) {
    const c = pickNum(item, 'count', 'COUNT')
    if (c > m) m = c
  }
  return m
})

// 学习时长趋势最大值（柱状图高度比例）
const trendStudyMax = computed(() => {
  let m = 0
  for (const item of trendData.value) {
    const c = Number(item.studySeconds || 0)
    if (c > m) m = c
  }
  return m
})

function barHeight(value, max) {
  const c = Number(value) || 0
  const m = Number(max) || 0
  if (m <= 0) return 4 // 全部为0时给最小高度
  if (c <= 0) return 4 // 0值也给最小高度
  return Math.max(6, Math.round((c / m) * 88)) // 最高88%留空间给tooltip
}

function trendLabel(item) {
  if (trendGranularity.value === 'weekly') {
    // week 是周一日期 yyyy-MM-dd，计算周一~周日
    const weekStart = pickKey(item, 'week', 'WEEK')
    if (!weekStart) return ''
    const d = new Date(weekStart)
    const end = new Date(d)
    end.setDate(end.getDate() + 6)
    const fmt = (dt) => String(dt.getMonth() + 1).padStart(2, '0') + '/' + String(dt.getDate()).padStart(2, '0')
    return fmt(d) + ' - ' + fmt(end)
  }
  return pickKey(item, 'date', 'DATE') || ''
}
function trendXLabel(item) {
  const full = trendLabel(item)
  if (!full) return ''
  if (trendGranularity.value === 'daily') {
    return full.length >= 10 ? full.slice(5) : full
  }
  // 每周显示 MM/DD-MM/DD
  return full
}

// 每周模式默认只显示最近8周，可展开查看全部
const displayTrendData = computed(() => {
  if (trendGranularity.value === 'weekly' && !showAllWeeks.value && trendData.value.length > 8) {
    return trendData.value.slice(-8)
  }
  return trendData.value
})

// 会话轮次
async function loadSessionRounds() {
  try {
    const res = await getMySessionRounds({ classId: chatStore.currentClassId })
    const data = res.data || {}
    sessionData.value = {
      avgRounds: data.avgRounds || 0,
      totalSessions: data.totalSessions || 0
    }
    sessionRows.value = (data.rounds || []).slice(0, 10)
  } catch (e) {
    console.error('获取会话轮次失败:', e)
    sessionData.value = {}
    sessionRows.value = []
  }
}

// 来源分布
async function loadSourceDistribution() {
  try {
    const res = await getMySourceDistribution({ classId: chatStore.currentClassId })
    const dist = (res.data && res.data.distribution) || []
    // 按固定顺序排列，缺失的来源补 0
    const ordered = Object.keys(SOURCE_CONFIG).map(src => {
      const found = dist.find(d => (pickKey(d, 'source', 'SOURCE') || '').toLowerCase() === src)
      return { source: src, count: found ? pickNum(found, 'count', 'COUNT') : 0 }
    })
    // 过滤掉全部 0 的情况交给模板空态判断；这里保留全部来源以便展示完整图例
    sourceItems.value = ordered.filter(it => it.count > 0)
    if (sourceItems.value.length === 0) sourceItems.value = []
  } catch (e) {
    console.error('获取来源分布失败:', e)
    sourceItems.value = []
  }
}

const sourceTotal = computed(() => sourceItems.value.reduce((s, it) => s + it.count, 0))

function sourceLabel(src) {
  return (SOURCE_CONFIG[src] && SOURCE_CONFIG[src].label) || src
}
function sourceColor(src) {
  return (SOURCE_CONFIG[src] && SOURCE_CONFIG[src].color) || SOURCE_CONFIG.unknown.color
}
function sourcePercent(count) {
  if (sourceTotal.value <= 0) return 0
  return Math.round((Number(count) / sourceTotal.value) * 100)
}

// 活跃天数
async function loadActiveDays() {
  try {
    const res = await getMyActiveDays({ classId: chatStore.currentClassId })
    const data = res.data || {}
    activeData.value = {
      activeDays: data.activeDays || 0,
      maxStreak: data.maxStreak || 0,
      dates: data.dates || [],
      studyByDate: data.studyByDate || {}
    }
  } catch (e) {
    console.error('获取活跃天数失败:', e)
    activeData.value = { activeDays: 0, maxStreak: 0, dates: [], studyByDate: {} }
  }
}

const heatmapDays = computed(() => {
  const dates = (activeData.value.dates || []).map(d => d.slice(0, 10))
  const set = new Set(dates)
  const studyMap = activeData.value.studyByDate || {}
  // 近30天，按时间正序展示
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const days = []
  for (let i = 29; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const dateStr = `${y}-${m}-${day}`
    days.push({
      date: dateStr,
      label: `${m}-${day}`,
      active: set.has(dateStr),
      studySeconds: Number(studyMap[dateStr] || 0)
    })
  }
  return days
})

onMounted(async () => {
  if (!chatStore.classes.length) await chatStore.fetchClasses()
  await loadStats()
})

// 切换班级后刷新统计
watch(() => chatStore.currentClassId, loadStats)

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
    margin: 0 0 4px;
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .page-sub {
    margin: 0;
    font-size: 13px;
    color: var(--color-text-tertiary);
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  border: 1px solid var(--color-border);
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    background: #eff6ff;
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(37, 99, 235, 0.1);
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
  background: #ffffff;
  border-radius: 14px;
  padding: 24px;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  margin-bottom: 20px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 16px;
  }
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .section-title {
    margin-bottom: 0;
  }
}

.keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-height: 240px;
  overflow-y: auto;
  padding: 4px 2px;
}

/* ===== 提问趋势柱状图 ===== */
.trend-switch {
  display: inline-flex;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 3px;

  .switch-btn {
    border: none;
    background: transparent;
    color: var(--color-text-secondary);
    font-size: 12px;
    padding: 5px 14px;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s ease;
    font-weight: 500;

    &:hover {
      color: #2563eb;
    }

    &.active {
      background: #2563eb;
      color: #fff;
      box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
    }
  }
}

.bar-chart {
  width: 100%;
}

.bar-track {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 200px;
  padding: 24px 4px 0;
  overflow-x: auto;
  overflow-y: visible;
}

.bar-col {
  flex: 1 1 0;
  min-width: 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.bar-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
}

.bar-fill {
  width: 55%;
  min-height: 2px;
  border-radius: 6px 6px 0 0;
  position: relative;
  transition: height 0.3s ease, filter 0.2s ease;

  &:hover {
    filter: brightness(1.12);
  }

  .bar-tip {
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    margin-bottom: 6px;
    background: rgba(15, 23, 42, 0.92);
    color: #fff;
    font-size: 11px;
    padding: 4px 8px;
    border-radius: 6px;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease;
    z-index: 5;
  }

  &:hover .bar-tip {
    opacity: 1;
  }
}

.count-bar {
  background: linear-gradient(180deg, #3b82f6, #2563eb);
}

.study-bar {
  background: linear-gradient(180deg, #fbbf24, #f59e0b);
}

.bar-x {
  margin-top: 6px;
  font-size: 10px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

/* 提问时段分布 */
.hour-hint {
  font-size: 13px;
  color: var(--color-text-tertiary);
  padding: 12px 0;
}

.hour-chart {
  width: 100%;
}

.hour-track {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 160px;
  padding: 20px 4px 0;
  overflow-x: auto;
  overflow-y: visible;
}

.hour-col {
  flex: 1 1 0;
  min-width: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.hour-bar-wrap {
  width: 100%;
  height: calc(100% - 18px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  position: relative;
}

.hour-bar {
  width: 72%;
  min-height: 2px;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #3b82f6, #2563eb);
  position: relative;
}

.hour-tip {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 50%;
  transform: translateX(-50%);
  background: #1e293b;
  color: #fff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s;
  z-index: 5;
}

.hour-bar:hover .hour-tip {
  opacity: 1;
}

.hour-x {
  font-size: 10px;
  color: var(--color-text-tertiary);
  height: 16px;
  line-height: 16px;
}

.hour-y-hint {
  text-align: right;
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 6px;
}

.bar-y-hint {
  text-align: right;
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 6px;
}

.trend-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.toggle-weeks-btn {
  background: #ffffff;
  border: 1px solid #bfdbfe;
  color: #2563eb;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.toggle-weeks-btn:hover {
  background: #eff6ff;
  color: #1d4ed8;
}

/* ===== 小卡片（轮次/活跃天数） ===== */
.mini-card-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 18px;
}

.mini-card {
  background: #ffffff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: var(--shadow-sm);

  .mini-value {
    font-size: 26px;
    font-weight: 700;
    color: #2563eb;
    line-height: 1.1;
  }

  .mini-label {
    font-size: 13px;
    color: var(--color-text-tertiary);
  }
}

/* ===== 数据表格（会话轮次） ===== */
.data-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;

  thead th {
    text-align: left;
    padding: 10px 12px;
    font-weight: 600;
    color: var(--color-text-secondary);
    background: #eff6ff;
    border-bottom: 1px solid var(--color-border);
    white-space: nowrap;

    &:first-child { border-top-left-radius: 10px; }
    &:last-child { border-top-right-radius: 10px; }
  }

  tbody td {
    padding: 11px 12px;
    color: var(--color-text-primary);
    border-bottom: 1px solid var(--color-border-light);
    white-space: nowrap;
  }

  tbody tr {
    transition: background 0.2s ease;

    &:hover {
      background: #eff6ff;
    }

    &:last-child td {
      border-bottom: none;
    }
  }
}

.round-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
  font-size: 12px;
}

/* ===== 来源分布 ===== */
.source-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.source-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.source-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .source-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .source-count {
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}

.source-track {
  width: 100%;
  height: 10px;
  background: var(--color-bg-secondary);
  border-radius: 999px;
  overflow: hidden;
}

.source-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
  min-width: 2px;
}

/* ===== 热力图 ===== */
.heatmap-wrap {
  margin-top: 4px;
}

.heatmap-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 10px;

  .study-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
    margin: 0 2px;
    vertical-align: middle;
  }
}

.heatmap-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 6px;
}

.heat-cell {
  aspect-ratio: 1 / 1;
  border-radius: 6px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: transform 0.15s ease, background 0.2s ease;

  .heat-day {
    font-size: 9px;
    color: var(--color-text-tertiary);
    opacity: 0.7;
  }

  &.active {
    background: linear-gradient(135deg, #3b82f6, #2563eb);
    border-color: #bfdbfe;
    box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);

    .heat-day {
      color: rgba(255, 255, 255, 0.92);
      opacity: 1;
      font-weight: 600;
    }
  }

  // 会话时间标志：该日有学习时长时显示琥珀色圆点
  .study-marker {
    position: absolute;
    top: 3px;
    right: 3px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
    box-shadow: 0 0 4px rgba(245, 158, 11, 0.8);
  }

  &.studied:not(.active) {
    background: #fff7ed;
    border-color: #fdba74;

    .heat-day {
      color: #b45309;
      opacity: 0.9;
    }
  }

  &:hover {
    transform: scale(1.12);
  }
}

@media (max-width: 640px) {
  .heatmap-grid {
    grid-template-columns: repeat(6, 1fr);
  }
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  justify-content: flex-end;

  .legend-text {
    font-size: 11px;
    color: var(--color-text-tertiary);
  }

  .legend-cell {
    width: 14px;
    height: 14px;
    border-radius: 4px;
    background: var(--color-bg-secondary);
    border: 1px solid var(--color-border);

    &.active {
      background: linear-gradient(135deg, #3b82f6, #2563eb);
      border-color: #bfdbfe;
    }
  }
}
</style>
