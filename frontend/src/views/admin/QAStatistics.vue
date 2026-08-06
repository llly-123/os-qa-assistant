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
                <el-icon :size="32" color="#2563eb"><ChatDotRound /></el-icon>
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
                <el-icon :size="32" color="#2563eb"><User /></el-icon>
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
                <el-icon :size="32" color="#f59e0b"><Timer /></el-icon>
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

        <!-- 分区一：提问概况 -->
        <div class="stat-section">
          <div class="stat-section-title"><span class="section-bar"></span>提问概况</div>
          <el-row :gutter="20">
            <!-- 提问趋势（次数） -->
            <el-col :span="12">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">提问趋势</span>
                  <div class="granularity-switch">
                    <button
                      :class="['granularity-btn', { active: trendGranularity === 'daily' }]"
                      @click="handleTrendGranularityChange('daily')"
                    >每日</button>
                    <button
                      :class="['granularity-btn', { active: trendGranularity === 'weekly' }]"
                      @click="handleTrendGranularityChange('weekly')"
                    >每周</button>
                  </div>
                </div>
                <div class="trend-chart">
                  <div class="trend-bars">
                    <div
                      v-for="(item, idx) in displayTrendData"
                      :key="idx"
                      class="trend-bar-item"
                    >
                      <div class="trend-bar-track">
                        <div
                          class="trend-bar count-bar"
                          :style="{ height: barHeight(item.count, trendMax) + '%' }"
                        >
                          <span class="trend-bar-tooltip">{{ trendLabel(item, trendGranularity) }}：{{ item.count || 0 }} 次</span>
                        </div>
                      </div>
                      <span class="trend-bar-label">{{ trendXLabel(item, trendGranularity) }}</span>
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
            </el-col>
            <!-- 学习时长趋势 -->
            <el-col :span="12">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">学习时长趋势</span>
                  <div class="granularity-switch">
                    <button
                      :class="['granularity-btn', { active: trendGranularity === 'daily' }]"
                      @click="handleTrendGranularityChange('daily')"
                    >每日</button>
                    <button
                      :class="['granularity-btn', { active: trendGranularity === 'weekly' }]"
                      @click="handleTrendGranularityChange('weekly')"
                    >每周</button>
                  </div>
                </div>
                <div class="trend-chart">
                  <div class="trend-bars">
                    <div
                      v-for="(item, idx) in displayTrendData"
                      :key="idx"
                      class="trend-bar-item"
                    >
                      <div class="trend-bar-track">
                        <div
                          class="trend-bar study-bar"
                          :style="{ height: barHeight(item.studySeconds, trendStudyMax) + '%' }"
                        >
                          <span class="trend-bar-tooltip">{{ trendLabel(item, trendGranularity) }}：学习 {{ formatStudy(item.studySeconds) }}</span>
                        </div>
                      </div>
                      <span class="trend-bar-label">{{ trendXLabel(item, trendGranularity) }}</span>
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
            </el-col>
          </el-row>

          <el-row :gutter="20" class="stat-row">
            <!-- 高频关键词 -->
            <el-col :span="12">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">高频关键词</span>
                </div>
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
              </div>
            </el-col>
            <!-- 提问来源分布 -->
            <el-col :span="12">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">提问来源分布</span>
                </div>
                <div class="dim-table-wrap">
                  <div v-if="sourceDistribution.length === 0" class="dim-empty">暂无来源数据</div>
                  <div v-else class="source-list">
                    <div v-for="(item, idx) in sourceDistribution" :key="idx" class="source-row">
                      <div class="source-row-head">
                        <span class="source-row-label">{{ getSourceLabel(item.source) }}</span>
                        <span class="source-row-value">{{ item.count || 0 }} ({{ getSourcePercent(item, sourceTotal) }}%)</span>
                      </div>
                      <div class="source-bar-track">
                        <div
                          class="source-bar"
                          :style="{
                            width: getSourcePercent(item, sourceTotal) + '%',
                            background: getSourceColor(item.source)
                          }"
                        ></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="20" class="stat-row">
            <!-- 提问时段分布 -->
            <el-col :span="24">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">提问时段分布</span>
                </div>
                <p v-if="hourlyMax === 0" class="dim-empty">暂无时段数据</p>
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
            </el-col>
          </el-row>
        </div>

        <!-- 分区二：学习与活跃 -->
        <div class="stat-section">
          <div class="stat-section-title"><span class="section-bar"></span>学习与活跃</div>
          <el-row :gutter="20">
            <!-- 学习时长统计 -->
            <el-col :span="24">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">学习时长统计</span>
                </div>
                <div class="mini-cards">
                  <div class="mini-card">
                    <span class="mini-card-value">{{ formatDuration(studyTimeStats.totalSeconds) }}</span>
                    <span class="mini-card-label">总学习时长</span>
                  </div>
                  <div class="mini-card">
                    <span class="mini-card-value">{{ studyTimeStats.students.length || 0 }}</span>
                    <span class="mini-card-label">学习学生数</span>
                  </div>
                </div>
                <div class="dim-table-wrap">
                  <div v-if="studyTimeStats.students.length === 0" class="dim-empty">暂无学习时长数据</div>
                  <table v-else class="dim-table">
                    <thead>
                      <tr>
                        <th style="width: 60px">排名</th>
                        <th>学生姓名</th>
                        <th>班级</th>
                        <th>学习时长</th>
                        <th>最后学习时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, idx) in studyTimeStats.students" :key="idx">
                        <td>{{ idx + 1 }}</td>
                        <td>{{ row.realName || row.username || '-' }}</td>
                        <td>{{ row.className || '-' }}</td>
                        <td><span class="round-badge">{{ formatDuration(row.totalSeconds) }}</span></td>
                        <td>{{ formatDateTime(row.lastStudy) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="20" class="stat-row">
            <!-- 会话轮次统计 -->
            <el-col :span="12">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">会话轮次统计</span>
                </div>
                <div class="mini-cards">
                  <div class="mini-card">
                    <span class="mini-card-value">{{ sessionRounds.avgRounds || 0 }}</span>
                    <span class="mini-card-label">平均轮次</span>
                  </div>
                  <div class="mini-card">
                    <span class="mini-card-value">{{ sessionRounds.totalSessions || 0 }}</span>
                    <span class="mini-card-label">总会话数</span>
                  </div>
                </div>
                <div class="dim-table-wrap">
                  <div v-if="topSessions.length === 0" class="dim-empty">暂无会话数据</div>
                  <table v-else class="dim-table">
                    <thead>
                      <tr>
                        <th style="width: 60px">排名</th>
                        <th>学生姓名</th>
                        <th style="width: 90px">轮次</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, idx) in topSessions" :key="idx">
                        <td>{{ idx + 1 }}</td>
                        <td>{{ row.realName || row.username || '-' }}</td>
                        <td><span class="round-badge">{{ row.rounds || 0 }}</span></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </el-col>
            <!-- 活跃天数统计 -->
            <el-col :span="12">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">活跃天数统计</span>
                </div>
                <div class="mini-cards">
                  <div class="mini-card">
                    <span class="mini-card-value">{{ activeDaysStats.avgActiveDays || 0 }}</span>
                    <span class="mini-card-label">平均活跃天数</span>
                  </div>
                </div>
                <div class="dim-table-wrap">
                  <div v-if="sortedActiveStudents.length === 0" class="dim-empty">暂无活跃数据</div>
                  <table v-else class="dim-table">
                    <thead>
                      <tr>
                        <th>学生姓名</th>
                        <th>活跃天数</th>
                        <th>最后活跃时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, idx) in sortedActiveStudents" :key="idx">
                        <td>{{ row.realName || row.username || '-' }}</td>
                        <td>{{ row.activeDays || 0 }}</td>
                        <td>{{ formatDateTime(row.lastActive) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 分区三：学生分析 -->
        <div class="stat-section">
          <div class="stat-section-title"><span class="section-bar"></span>学生分析</div>
          <el-row :gutter="20">
            <!-- 学生个体排行 -->
            <el-col :span="24">
              <div class="dim-card">
                <div class="dim-card-header">
                  <span class="dim-card-title">学生个体排行</span>
                </div>
                <div class="dim-table-wrap">
                  <div v-if="studentRanking.length === 0" class="dim-empty">暂无排行数据</div>
                  <table v-else class="dim-table">
                    <thead>
                      <tr>
                        <th style="width: 60px">排名</th>
                        <th>学生姓名</th>
                        <th>提问数</th>
                        <th>活跃天数</th>
                        <th>引用率</th>
                        <th>学习时长</th>
                        <th>最后活跃</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, idx) in studentRanking" :key="idx">
                        <td>{{ idx + 1 }}</td>
                        <td>{{ row.realName || row.username || '-' }}</td>
                        <td>{{ row.questionCount || 0 }}</td>
                        <td>{{ row.activeDays || 0 }}</td>
                        <td>{{ row.citationRate || 0 }}%</td>
                        <td><span class="round-badge">{{ formatDuration(row.totalSeconds) }}</span></td>
                        <td>{{ formatDateTime(row.lastActive) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
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
                  <el-icon :size="32" color="#2563eb"><ChatDotRound /></el-icon>
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
                  <el-icon :size="32" color="#2563eb"><User /></el-icon>
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
                  <el-icon :size="32" color="#f59e0b"><Timer /></el-icon>
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

          <!-- 分区一：提问概况 -->
          <div class="stat-section">
            <div class="stat-section-title"><span class="section-bar"></span>提问概况</div>
            <el-row :gutter="20">
              <!-- 提问趋势（次数） -->
              <el-col :span="12">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">提问趋势</span>
                    <div class="granularity-switch">
                      <button
                        :class="['granularity-btn', { active: classTrendGranularity === 'daily' }]"
                        @click="handleClassTrendGranularityChange('daily')"
                      >每日</button>
                      <button
                        :class="['granularity-btn', { active: classTrendGranularity === 'weekly' }]"
                        @click="handleClassTrendGranularityChange('weekly')"
                      >每周</button>
                    </div>
                  </div>
                  <div class="trend-chart">
                    <div class="trend-bars">
                      <div
                        v-for="(item, idx) in classDisplayTrendData"
                        :key="idx"
                        class="trend-bar-item"
                      >
                        <div class="trend-bar-track">
                          <div
                            class="trend-bar count-bar"
                            :style="{ height: barHeight(item.count, classTrendMax) + '%' }"
                          >
                            <span class="trend-bar-tooltip">{{ trendLabel(item, classTrendGranularity) }}：{{ item.count || 0 }} 次</span>
                          </div>
                        </div>
                        <span class="trend-bar-label">{{ trendXLabel(item, classTrendGranularity) }}</span>
                      </div>
                    </div>
                    <div class="trend-footer">
                      <div v-if="classTrendMax > 0" class="bar-y-hint">最多 {{ classTrendMax }} 次</div>
                      <button
                        v-if="classTrendGranularity === 'weekly' && classTrendData.length > 8"
                        class="toggle-weeks-btn"
                        @click="classShowAllWeeks = !classShowAllWeeks"
                      >{{ classShowAllWeeks ? '收起' : `查看全部 ${classTrendData.length} 周` }}</button>
                    </div>
                  </div>
                </div>
              </el-col>
              <!-- 学习时长趋势 -->
              <el-col :span="12">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">学习时长趋势</span>
                    <div class="granularity-switch">
                      <button
                        :class="['granularity-btn', { active: classTrendGranularity === 'daily' }]"
                        @click="handleClassTrendGranularityChange('daily')"
                      >每日</button>
                      <button
                        :class="['granularity-btn', { active: classTrendGranularity === 'weekly' }]"
                        @click="handleClassTrendGranularityChange('weekly')"
                      >每周</button>
                    </div>
                  </div>
                  <div class="trend-chart">
                    <div class="trend-bars">
                      <div
                        v-for="(item, idx) in classDisplayTrendData"
                        :key="idx"
                        class="trend-bar-item"
                      >
                        <div class="trend-bar-track">
                          <div
                            class="trend-bar study-bar"
                            :style="{ height: barHeight(item.studySeconds, classTrendStudyMax) + '%' }"
                          >
                            <span class="trend-bar-tooltip">{{ trendLabel(item, classTrendGranularity) }}：学习 {{ formatStudy(item.studySeconds) }}</span>
                          </div>
                        </div>
                        <span class="trend-bar-label">{{ trendXLabel(item, classTrendGranularity) }}</span>
                      </div>
                    </div>
                    <div class="trend-footer">
                      <div v-if="classTrendStudyMax > 0" class="bar-y-hint">最多 {{ formatStudy(classTrendStudyMax) }}</div>
                      <button
                        v-if="classTrendGranularity === 'weekly' && classTrendData.length > 8"
                        class="toggle-weeks-btn"
                        @click="classShowAllWeeks = !classShowAllWeeks"
                      >{{ classShowAllWeeks ? '收起' : `查看全部 ${classTrendData.length} 周` }}</button>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="20" class="stat-row">
              <!-- 高频关键词 -->
              <el-col :span="12">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">高频关键词</span>
                  </div>
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
                </div>
              </el-col>
              <!-- 提问来源分布 -->
              <el-col :span="12">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">提问来源分布</span>
                  </div>
                  <div class="dim-table-wrap">
                    <div v-if="classSourceDistribution.length === 0" class="dim-empty">暂无来源数据</div>
                    <div v-else class="source-list">
                      <div v-for="(item, idx) in classSourceDistribution" :key="idx" class="source-row">
                        <div class="source-row-head">
                          <span class="source-row-label">{{ getSourceLabel(item.source) }}</span>
                          <span class="source-row-value">{{ item.count || 0 }} ({{ getSourcePercent(item, classSourceTotal) }}%)</span>
                        </div>
                        <div class="source-bar-track">
                          <div
                            class="source-bar"
                            :style="{
                              width: getSourcePercent(item, classSourceTotal) + '%',
                              background: getSourceColor(item.source)
                            }"
                          ></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="20" class="stat-row">
              <!-- 提问时段分布 -->
              <el-col :span="24">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">提问时段分布</span>
                  </div>
                  <p v-if="classHourlyMax === 0" class="dim-empty">暂无时段数据</p>
                  <div v-else class="hour-chart">
                    <div class="hour-track">
                      <div v-for="(h, idx) in classHourlyData" :key="idx" class="hour-col">
                        <div class="hour-bar-wrap">
                          <div
                            class="hour-bar"
                            :style="{ height: barHeight(h.count, classHourlyMax) + '%' }"
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
              </el-col>
            </el-row>
          </div>

          <!-- 分区二：学习与活跃 -->
          <div class="stat-section">
            <div class="stat-section-title"><span class="section-bar"></span>学习与活跃</div>
            <el-row :gutter="20">
              <!-- 学习时长统计 -->
              <el-col :span="24">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">学习时长统计</span>
                  </div>
                  <div class="mini-cards">
                    <div class="mini-card">
                      <span class="mini-card-value">{{ formatDuration(classStudyTimeStats.totalSeconds) }}</span>
                      <span class="mini-card-label">总学习时长</span>
                    </div>
                    <div class="mini-card">
                      <span class="mini-card-value">{{ classStudyTimeStats.students.length || 0 }}</span>
                      <span class="mini-card-label">学习学生数</span>
                    </div>
                  </div>
                  <div class="dim-table-wrap">
                    <div v-if="classStudyTimeStats.students.length === 0" class="dim-empty">暂无学习时长数据</div>
                    <table v-else class="dim-table">
                      <thead>
                        <tr>
                          <th style="width: 60px">排名</th>
                          <th>学生姓名</th>
                          <th>学习时长</th>
                          <th>最后学习时间</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, idx) in classStudyTimeStats.students" :key="idx">
                          <td>{{ idx + 1 }}</td>
                          <td>{{ row.realName || row.username || '-' }}</td>
                          <td><span class="round-badge">{{ formatDuration(row.totalSeconds) }}</span></td>
                          <td>{{ formatDateTime(row.lastStudy) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="20" class="stat-row">
              <!-- 会话轮次统计 -->
              <el-col :span="12">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">会话轮次统计</span>
                  </div>
                  <div class="mini-cards">
                    <div class="mini-card">
                      <span class="mini-card-value">{{ classSessionRounds.avgRounds || 0 }}</span>
                      <span class="mini-card-label">平均轮次</span>
                    </div>
                    <div class="mini-card">
                      <span class="mini-card-value">{{ classSessionRounds.totalSessions || 0 }}</span>
                      <span class="mini-card-label">总会话数</span>
                    </div>
                  </div>
                  <div class="dim-table-wrap">
                    <div v-if="classTopSessions.length === 0" class="dim-empty">暂无会话数据</div>
                    <table v-else class="dim-table">
                      <thead>
                        <tr>
                          <th style="width: 60px">排名</th>
                          <th>学生姓名</th>
                          <th style="width: 90px">轮次</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, idx) in classTopSessions" :key="idx">
                          <td>{{ idx + 1 }}</td>
                          <td>{{ row.realName || row.username || '-' }}</td>
                          <td><span class="round-badge">{{ row.rounds || 0 }}</span></td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </el-col>
              <!-- 活跃天数统计 -->
              <el-col :span="12">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">活跃天数统计</span>
                  </div>
                  <div class="mini-cards">
                    <div class="mini-card">
                      <span class="mini-card-value">{{ classActiveDaysStats.avgActiveDays || 0 }}</span>
                      <span class="mini-card-label">平均活跃天数</span>
                    </div>
                  </div>
                  <div class="dim-table-wrap">
                    <div v-if="classSortedActiveStudents.length === 0" class="dim-empty">暂无活跃数据</div>
                    <table v-else class="dim-table">
                      <thead>
                        <tr>
                          <th>学生姓名</th>
                          <th>活跃天数</th>
                          <th>最后活跃时间</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, idx) in classSortedActiveStudents" :key="idx">
                          <td>{{ row.realName || row.username || '-' }}</td>
                          <td>{{ row.activeDays || 0 }}</td>
                          <td>{{ formatDateTime(row.lastActive) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 分区三：学生分析 -->
          <div class="stat-section">
            <div class="stat-section-title"><span class="section-bar"></span>学生分析</div>
            <el-row :gutter="20">
              <!-- 学生个体排行 -->
              <el-col :span="24">
                <div class="dim-card">
                  <div class="dim-card-header">
                    <span class="dim-card-title">学生个体排行</span>
                  </div>
                  <div class="dim-table-wrap">
                    <div v-if="classStudentRanking.length === 0" class="dim-empty">暂无排行数据</div>
                    <table v-else class="dim-table">
                      <thead>
                        <tr>
                          <th style="width: 60px">排名</th>
                          <th>学生姓名</th>
                          <th>提问数</th>
                          <th>活跃天数</th>
                          <th>引用率</th>
                          <th>学习时长</th>
                          <th>最后活跃</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, idx) in classStudentRanking" :key="idx">
                          <td>{{ idx + 1 }}</td>
                          <td>{{ row.realName || row.username || '-' }}</td>
                          <td>{{ row.questionCount || 0 }}</td>
                          <td>{{ row.activeDays || 0 }}</td>
                          <td>{{ row.citationRate || 0 }}%</td>
                          <td><span class="round-badge">{{ formatDuration(row.totalSeconds) }}</span></td>
                          <td>{{ formatDateTime(row.lastActive) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  getQAStatistics, getHotKeywords, getClassList, getClassOverview, getClassHotKeywords,
  getQuestionTrend, getSessionRounds, getSourceDistribution, getActiveDaysStats,
  getClassQuestionTrend, getClassSessionRounds, getClassSourceDistribution, getClassActiveDaysStats,
  getStudyTimeStats, getClassStudyTimeStats,
  getHourlyDistribution, getClassHourlyDistribution, getStudentRanking, getClassStudentRanking
} from '@/api/statistics'

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

// 提问趋势（每日/每周切换）
const trendGranularity = ref('daily')
const classTrendGranularity = ref('daily')
// 提问趋势数据（已补全缺失日期/周）
const trendData = ref([])
const classTrendData = ref([])
// 每周模式默认只显示最近8周，可展开查看全部
const showAllWeeks = ref(false)
const classShowAllWeeks = ref(false)
// 趋势最大值（柱状图高度比例）
const trendMax = computed(() => {
  let m = 0
  for (const item of trendData.value) {
    const c = Number(item.count || 0)
    if (c > m) m = c
  }
  return m
})
const classTrendMax = computed(() => {
  let m = 0
  for (const item of classTrendData.value) {
    const c = Number(item.count || 0)
    if (c > m) m = c
  }
  return m
})
// 学习时长趋势最大值
const trendStudyMax = computed(() => {
  let m = 0
  for (const item of trendData.value) {
    const c = Number(item.studySeconds || 0)
    if (c > m) m = c
  }
  return m
})
const classTrendStudyMax = computed(() => {
  let m = 0
  for (const item of classTrendData.value) {
    const c = Number(item.studySeconds || 0)
    if (c > m) m = c
  }
  return m
})
// 每周模式默认只显示最近8周，可展开查看全部
const displayTrendData = computed(() => {
  if (trendGranularity.value === 'weekly' && !showAllWeeks.value && trendData.value.length > 8) {
    return trendData.value.slice(-8)
  }
  return trendData.value
})
const classDisplayTrendData = computed(() => {
  if (classTrendGranularity.value === 'weekly' && !classShowAllWeeks.value && classTrendData.value.length > 8) {
    return classTrendData.value.slice(-8)
  }
  return classTrendData.value
})

// 会话轮次
const sessionRounds = ref({ rounds: [], avgRounds: 0, totalSessions: 0 })
const classSessionRounds = ref({ rounds: [], avgRounds: 0, totalSessions: 0 })
// Top10 会话（按轮次降序）
const topSessions = computed(() =>
  [...sessionRounds.value.rounds].sort((a, b) => (b.rounds || 0) - (a.rounds || 0)).slice(0, 10)
)
const classTopSessions = computed(() =>
  [...classSessionRounds.value.rounds].sort((a, b) => (b.rounds || 0) - (a.rounds || 0)).slice(0, 10)
)

// 来源分布
const sourceDistribution = ref([])
const classSourceDistribution = ref([])
const sourceTotal = computed(() => sourceDistribution.value.reduce((s, i) => s + (i.count || 0), 0))
const classSourceTotal = computed(() => classSourceDistribution.value.reduce((s, i) => s + (i.count || 0), 0))

// 提问时段分布（24 小时）
const hourlyData = ref([])
const classHourlyData = ref([])
const hourlyMax = computed(() => Math.max(0, ...hourlyData.value.map(h => h.count || 0)))
const classHourlyMax = computed(() => Math.max(0, ...classHourlyData.value.map(h => h.count || 0)))

// 学生个体排行
const studentRanking = ref([])
const classStudentRanking = ref([])

// 活跃天数
const activeDaysStats = ref({ students: [], avgActiveDays: 0 })
const classActiveDaysStats = ref({ students: [], avgActiveDays: 0 })
// 按活跃天数降序
const sortedActiveStudents = computed(() =>
  [...activeDaysStats.value.students].sort((a, b) => (b.activeDays || 0) - (a.activeDays || 0))
)
const classSortedActiveStudents = computed(() =>
  [...classActiveDaysStats.value.students].sort((a, b) => (b.activeDays || 0) - (a.activeDays || 0))
)

// 学习时长
const studyTimeStats = ref({ totalSeconds: 0, students: [] })
const classStudyTimeStats = ref({ totalSeconds: 0, students: [] })

// 秒数格式化为 "X小时Y分钟" / "Y分钟"
function formatDuration(seconds) {
  const s = Number(seconds) || 0
  if (s <= 0) return '0分钟'
  const hours = Math.floor(s / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  const secs = s % 60
  if (hours > 0) {
    return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`
  }
  if (minutes > 0) {
    return secs > 0 ? `${minutes}分钟${secs}秒` : `${minutes}分钟`
  }
  return `${secs}秒`
}

// 秒数格式化为 "X小时Y分钟" / "Y分钟" / "小于1分钟"（会话时长用）
function formatStudy(seconds) {
  const s = Number(seconds) || 0
  if (s <= 0) return '0分钟'
  if (s < 60) return '小于1分钟'
  return formatDuration(s)
}

// 来源类型配置：不同颜色（与学生端一致的渐变色）
const SOURCE_CONFIG = {
  textbook: { label: '教材', color: 'linear-gradient(90deg, #2563eb, #3b82f6)' },
  web: { label: '网络', color: 'linear-gradient(90deg, #0ea5e9, #38bdf8)' },
  no_class: { label: '未进班级', color: 'linear-gradient(90deg, #f59e0b, #fbbf24)' },
  unknown: { label: '未知', color: 'linear-gradient(90deg, #94a3b8, #cbd5e1)' }
}

function getSourceLabel(source) {
  return (SOURCE_CONFIG[source] && SOURCE_CONFIG[source].label) || source || '未知'
}

function getSourceColor(source) {
  return (SOURCE_CONFIG[source] && SOURCE_CONFIG[source].color) || '#94a3b8'
}

function getSourcePercent(item, total) {
  if (!total) return 0
  return Math.round(((item.count || 0) / total) * 100)
}

// 取值容错：兼容大小写键
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

// 将任意日期归一化到所在周的周一
function toMonday(date) {
  const d = new Date(date)
  const dayOfWeek = (d.getDay() + 6) % 7 // 周一=0, 周日=6
  d.setDate(d.getDate() - dayOfWeek)
  d.setHours(0, 0, 0, 0)
  return d
}

function fmtDate(d) {
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

// 补全每日缺失日期（从起始到今天，无数据天显示为0）
function fillDailyGaps(raw, startDate, endDate) {
  const map = {}
  for (const item of raw) {
    const d = pickKey(item, 'date', 'DATE')
    if (d) map[d] = pickNum(item, 'count', 'COUNT')
  }
  const result = []
  const cur = new Date(startDate)
  cur.setHours(0, 0, 0, 0)
  while (cur <= endDate) {
    const key = fmtDate(cur)
    result.push({ date: key, count: map[key] || 0 })
    cur.setDate(cur.getDate() + 1)
  }
  return result
}

// 补全每周缺失周（按周一归一化）
function fillWeeklyGaps(raw, startDate, endDate) {
  const map = {}
  for (const item of raw) {
    const w = pickKey(item, 'week', 'WEEK')
    if (w) {
      const monday = toMonday(new Date(w))
      const key = fmtDate(monday)
      map[key] = pickNum(item, 'count', 'COUNT')
    }
  }
  const result = []
  const cur = toMonday(startDate)
  while (cur <= endDate) {
    const key = fmtDate(cur)
    result.push({ week: key, count: map[key] || 0 })
    cur.setDate(cur.getDate() + 7)
  }
  return result
}

// 根据粒度补全趋势数据（叠加学习时长趋势）
function processTrendData(raw, studyRaw, granularity, startDate) {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  const start = startDate || (() => {
    const d = new Date()
    d.setDate(d.getDate() - 29)
    return d
  })()
  const studyMap = buildStudyMap(studyRaw || [], granularity)
  if (granularity === 'daily') {
    return fillDailyGaps(raw, start, today).map(it => ({
      ...it,
      studySeconds: studyMap[it.date] || 0
    }))
  }
  return fillWeeklyGaps(raw, start, today).map(it => ({
    ...it,
    studySeconds: studyMap[it.week] || 0
  }))
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
      ? fmtDate(toMonday(d))
      : fmtDate(d)
    map[key] = pickNum(item, 'studySeconds', 'STUDYSECONDS')
  }
  return map
}

// 将时段分布原始数据补全为 24 小时数组（0-23 点，缺省补 0）
function buildHourly(dist) {
  const map = {}
  for (const it of dist || []) {
    map[Number(pickKey(it, 'hour', 'HOUR'))] = pickNum(it, 'count', 'COUNT')
  }
  return Array.from({ length: 24 }, (_, h) => ({ hour: h, count: map[h] || 0 }))
}

// 趋势柱状图高度：最高88%留空间给tooltip，0值给最小高度
function barHeight(count, max) {
  const c = Number(count) || 0
  const m = max || 0
  if (m <= 0) return 4
  if (c <= 0) return 4
  return Math.max(6, Math.round((c / m) * 88))
}

function trendLabel(item, granularity) {
  if (granularity === 'weekly') {
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

function trendXLabel(item, granularity) {
  const full = trendLabel(item, granularity)
  if (!full) return ''
  if (granularity === 'daily') {
    return full.length >= 10 ? full.slice(5) : full
  }
  return full
}

function formatDateTime(time) {
  if (!time) return '-'
  const d = new Date(time)
  if (isNaN(d.getTime())) return time
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}

onMounted(async () => {
  // 先获取班级列表，以便总体Tab能从教师最早班级创建时间开始补全
  await fetchClassList()
  fetchOverallData()
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
  // 总体Tab：从教师最早班级创建时间开始，否则默认30天前
  let startDate = null
  if (classList.value.length > 0) {
    const earliest = classList.value
      .map(c => c.startTime && new Date(c.startTime))
      .filter(d => d && !isNaN(d.getTime()))
      .sort((a, b) => a - b)[0]
    if (earliest) startDate = earliest
  }
  if (!startDate) {
    startDate = new Date()
    startDate.setDate(startDate.getDate() - 29)
  }
  try {
    // 趋势请求：未选日期时把最早班级创建时间作为起始日传给后端，避免后端 30 天默认截断漏数据
    const trendParams = { ...params, granularity: trendGranularity.value }
    if (!params.startDate) {
      trendParams.startDate = formatDate(startDate) + ' 00:00:00'
      trendParams.endDate = formatDate(new Date()) + ' 23:59:59'
    }
    const [statRes, kwRes, trendRes, sessionRes, sourceRes, activeRes, studyRes, hourlyRes, rankingRes] = await Promise.all([
      getQAStatistics(params),
      getHotKeywords({ ...params, limit: 30 }),
      getQuestionTrend(trendParams),
      getSessionRounds(params),
      getSourceDistribution(params),
      getActiveDaysStats(params),
      getStudyTimeStats(params),
      getHourlyDistribution(params),
      getStudentRanking(params)
    ])
    overview.value = statRes.data || {}
    keywords.value = kwRes.data || []
    const rawTrend = (trendRes.data && trendRes.data.trend) || []
    const rawStudyTrend = (trendRes.data && trendRes.data.studyTrend) || []
    trendData.value = processTrendData(rawTrend, rawStudyTrend, trendGranularity.value, startDate)
    sessionRounds.value = {
      rounds: (sessionRes.data && sessionRes.data.rounds) || [],
      avgRounds: (sessionRes.data && sessionRes.data.avgRounds) || 0,
      totalSessions: (sessionRes.data && sessionRes.data.totalSessions) || 0
    }
    sourceDistribution.value = (sourceRes.data && sourceRes.data.distribution) || []
    activeDaysStats.value = {
      students: (activeRes.data && activeRes.data.students) || [],
      avgActiveDays: (activeRes.data && activeRes.data.avgActiveDays) || 0
    }
    studyTimeStats.value = {
      totalSeconds: (studyRes.data && studyRes.data.totalSeconds) || 0,
      students: (studyRes.data && studyRes.data.students) || []
    }
    hourlyData.value = buildHourly(hourlyRes.data && hourlyRes.data.distribution)
    studentRanking.value = (rankingRes.data && rankingRes.data.students) || []
  } catch (e) {
    console.error('获取总体统计失败:', e)
  }
}

// 总体趋势切换每日/每周
async function handleTrendGranularityChange(g) {
  if (trendGranularity.value === g) return
  trendGranularity.value = g
  showAllWeeks.value = false
  const params = getDateParams()
  // 总体Tab：从教师最早班级创建时间开始，否则默认30天前
  let startDate = null
  if (classList.value.length > 0) {
    const earliest = classList.value
      .map(c => c.startTime && new Date(c.startTime))
      .filter(d => d && !isNaN(d.getTime()))
      .sort((a, b) => a - b)[0]
    if (earliest) startDate = earliest
  }
  if (!startDate) {
    startDate = new Date()
    startDate.setDate(startDate.getDate() - 29)
  }
  try {
    // 趋势请求：未选日期时把最早班级创建时间作为起始日传给后端，避免后端 30 天默认截断漏数据
    const trendParams = { ...params, granularity: g }
    if (!params.startDate) {
      trendParams.startDate = formatDate(startDate) + ' 00:00:00'
      trendParams.endDate = formatDate(new Date()) + ' 23:59:59'
    }
    const res = await getQuestionTrend(trendParams)
    const rawTrend = (res.data && res.data.trend) || []
    const rawStudyTrend = (res.data && res.data.studyTrend) || []
    trendData.value = processTrendData(rawTrend, rawStudyTrend, g, startDate)
  } catch (e) {
    console.error('获取提问趋势失败:', e)
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
  classTrendData.value = []
  classShowAllWeeks.value = false
  classSessionRounds.value = { rounds: [], avgRounds: 0, totalSessions: 0 }
  classSourceDistribution.value = []
  classHourlyData.value = []
  classStudentRanking.value = []
  classActiveDaysStats.value = { students: [], avgActiveDays: 0 }
  classStudyTimeStats.value = { totalSeconds: 0, students: [] }
}

async function fetchClassData(classId) {
  const params = getDateParams()
  // 班级Tab：从选中班级的创建时间开始，否则默认30天前
  const cls = classList.value.find(item => item.id === classId)
  let startDate = null
  if (cls && cls.startTime) {
    const d = new Date(cls.startTime)
    if (!isNaN(d.getTime())) startDate = d
  }
  if (!startDate) {
    startDate = new Date()
    startDate.setDate(startDate.getDate() - 29)
  }
  try {
    // 班级趋势请求：未选日期时把班级创建时间作为起始日传给后端，避免后端 30 天默认截断漏数据
    const trendParams = { ...params, granularity: classTrendGranularity.value }
    if (!params.startDate) {
      trendParams.startDate = formatDate(startDate) + ' 00:00:00'
      trendParams.endDate = formatDate(new Date()) + ' 23:59:59'
    }
    const [statRes, kwRes, trendRes, sessionRes, sourceRes, activeRes, studyRes, hourlyRes, rankingRes] = await Promise.all([
      getClassOverview(classId, params),
      getClassHotKeywords(classId, { ...params, limit: 30 }),
      getClassQuestionTrend(classId, trendParams),
      getClassSessionRounds(classId, params),
      getClassSourceDistribution(classId, params),
      getClassActiveDaysStats(classId, params),
      getClassStudyTimeStats(classId, params),
      getClassHourlyDistribution(classId, params),
      getClassStudentRanking(classId, params)
    ])
    classOverview.value = statRes.data || {}
    classKeywords.value = kwRes.data || []
    const rawTrend = (trendRes.data && trendRes.data.trend) || []
    const rawStudyTrend = (trendRes.data && trendRes.data.studyTrend) || []
    classTrendData.value = processTrendData(rawTrend, rawStudyTrend, classTrendGranularity.value, startDate)
    classSessionRounds.value = {
      rounds: (sessionRes.data && sessionRes.data.rounds) || [],
      avgRounds: (sessionRes.data && sessionRes.data.avgRounds) || 0,
      totalSessions: (sessionRes.data && sessionRes.data.totalSessions) || 0
    }
    classSourceDistribution.value = (sourceRes.data && sourceRes.data.distribution) || []
    classActiveDaysStats.value = {
      students: (activeRes.data && activeRes.data.students) || [],
      avgActiveDays: (activeRes.data && activeRes.data.avgActiveDays) || 0
    }
    classStudyTimeStats.value = {
      totalSeconds: (studyRes.data && studyRes.data.totalSeconds) || 0,
      students: (studyRes.data && studyRes.data.students) || []
    }
    classHourlyData.value = buildHourly(hourlyRes.data && hourlyRes.data.distribution)
    classStudentRanking.value = (rankingRes.data && rankingRes.data.students) || []
  } catch (e) {
    console.error('获取班级统计失败:', e)
  }
}

// 班级趋势切换每日/每周
async function handleClassTrendGranularityChange(g) {
  if (classTrendGranularity.value === g) return
  classTrendGranularity.value = g
  classShowAllWeeks.value = false
  if (!selectedClassId.value) return
  const params = getDateParams()
  // 班级Tab：从选中班级的创建时间开始，否则默认30天前
  const cls = classList.value.find(item => item.id === selectedClassId.value)
  let startDate = null
  if (cls && cls.startTime) {
    const d = new Date(cls.startTime)
    if (!isNaN(d.getTime())) startDate = d
  }
  if (!startDate) {
    startDate = new Date()
    startDate.setDate(startDate.getDate() - 29)
  }
  try {
    // 班级趋势请求：未选日期时把班级创建时间作为起始日传给后端，避免后端 30 天默认截断漏数据
    const trendParams = { ...params, granularity: g }
    if (!params.startDate) {
      trendParams.startDate = formatDate(startDate) + ' 00:00:00'
      trendParams.endDate = formatDate(new Date()) + ' 23:59:59'
    }
    const res = await getClassQuestionTrend(selectedClassId.value, trendParams)
    const rawTrend = (res.data && res.data.trend) || []
    const rawStudyTrend = (res.data && res.data.studyTrend) || []
    classTrendData.value = processTrendData(rawTrend, rawStudyTrend, g, startDate)
  } catch (e) {
    console.error('获取班级提问趋势失败:', e)
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
  max-height: 240px;
  overflow-y: auto;
  padding: 4px 2px;
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
  color: #2563eb;
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

/* ===== 统计分区（分区标题 + 两列网格） ===== */
.stat-section {
  margin-top: 24px;
}

.stat-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.section-bar {
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, #3b82f6, #2563eb);
}

.stat-row {
  margin-top: 20px;
}

/* 两列并排时卡片等高 */
.stat-section .el-col {
  display: flex;
  flex-direction: column;
}

.stat-section .dim-card {
  width: 100%;
  flex: 1;
  margin-top: 0;
}

/* ===== 新增维度展示区域（浅色白底 + 蓝色主色） ===== */
.dim-card {
  margin-top: 20px;
  padding: 22px 24px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  color: var(--color-text-primary);
}

.dim-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.dim-card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.dim-empty {
  padding: 28px 0;
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

/* 提问时段分布 */
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

/* 每日/每周切换 */
.granularity-switch {
  display: inline-flex;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 2px;
}

.granularity-btn {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: #2563eb;
  }

  &.active {
    background: linear-gradient(135deg, #3b82f6, #2563eb);
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
  }
}

/* 提问趋势柱状图 */
.trend-chart {
  min-height: 200px;
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 200px;
  overflow-x: auto;
  overflow-y: visible;
  padding: 24px 0 4px;
}

.trend-bar-item {
  flex: 1 0 48px;
  min-width: 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.trend-bar-track {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
}

.trend-bar {
  width: 60%;
  min-height: 3px;
  border-radius: 6px 6px 0 0;
  position: relative;
  transition: filter 0.2s ease, transform 0.2s ease;

  &:hover {
    filter: brightness(1.15);
    transform: translateY(-2px);
  }
}

.count-bar {
  background: linear-gradient(180deg, #3b82f6, #2563eb);
}

.study-bar {
  background: linear-gradient(180deg, #fbbf24, #f59e0b);
}

.trend-bar-tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(-6px);
  background: rgba(15, 23, 42, 0.92);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 5px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

.trend-bar:hover .trend-bar-tooltip {
  opacity: 1;
}

.trend-bar-label {
  margin-top: 8px;
  font-size: 11px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* 小卡片 */
.mini-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.mini-card {
  flex: 1 1 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  box-shadow: var(--shadow-sm);
}

.mini-card-value {
  font-size: 26px;
  font-weight: 700;
  color: #2563eb;
  line-height: 1.1;
}

.mini-card-label {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 表格 */
.dim-table-wrap {
  overflow-x: auto;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid var(--color-border);
}

.dim-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  thead th {
    text-align: left;
    padding: 12px 16px;
    font-weight: 600;
    color: var(--color-text-secondary);
    background: #eff6ff;
    border-bottom: 1px solid var(--color-border);
    white-space: nowrap;
  }

  tbody td {
    padding: 11px 16px;
    color: var(--color-text-primary);
    border-bottom: 1px solid var(--color-border-light);
    white-space: nowrap;
  }

  tbody tr:last-child td {
    border-bottom: none;
  }

  tbody tr:hover td {
    background: #eff6ff;
  }
}

/* 来源分布横向进度条 */
.source-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0;
}

.source-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.source-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.source-row-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.source-row-value {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.source-bar-track {
  width: 100%;
  height: 10px;
  border-radius: 6px;
  background: var(--color-bg-secondary);
  overflow: hidden;
}

.source-bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.4s ease;
  min-width: 2px;
}

/* 趋势图底部信息栏 + 查看全部按钮 */
.trend-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.bar-y-hint {
  text-align: right;
  font-size: 11px;
  color: var(--color-text-tertiary);
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
  transition: all 0.2s ease;

  &:hover {
    background: #eff6ff;
    color: #1d4ed8;
  }
}

/* 会话轮次徽标 */
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
</style>
