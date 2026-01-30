<template>
  <div class="instance-management">
    <!-- 统计卡片 -->
    <div class="stats-card">
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-icon blue">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.length }}</span>
            <span class="stat-label">总执行次数</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon green">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><path d="M22 4L12 14.01l-3-3"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.filter(i => i.status === 4).length }}</span>
            <span class="stat-label">执行成功</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon red">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.filter(i => i.status === 5).length }}</span>
            <span class="stat-label">执行失败</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon orange">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4l2 2"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.filter(i => i.status === 3).length }}</span>
            <span class="stat-label">执行中</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表卡片 -->
    <div class="list-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="search-box">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input 
              v-model="searchKeyword" 
              type="text" 
              class="search-input" 
              placeholder="搜索实例ID或执行器地址..."
            />
          </div>
          <button class="filter-btn" :class="{ active: showAdvancedFilter }" @click="showAdvancedFilter = !showAdvancedFilter">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
            </svg>
            <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
          </button>
        </div>
        
        <div class="toolbar-right">
          <n-tooltip trigger="hover">
            <template #trigger>
              <button class="tool-btn" @click="handleRefresh">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 4v6h-6"/><path d="M1 20v-6h6"/>
                  <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
                </svg>
              </button>
            </template>
            刷新
          </n-tooltip>
        </div>
      </div>

      <!-- 高级筛选面板 -->
      <transition name="slide-down">
        <div v-if="showAdvancedFilter" class="advanced-filter">
          <div class="filter-row">
            <div class="filter-field">
              <label>任务</label>
              <n-select v-model:value="filters.jobId" :options="jobOptions" placeholder="全部任务" clearable size="small" />
            </div>
            <div class="filter-field">
              <label>状态</label>
              <n-select v-model:value="filters.status" :options="statusOptions" placeholder="全部状态" clearable size="small" />
            </div>
            <div class="filter-field">
              <label>触发类型</label>
              <n-select v-model:value="filters.triggerType" :options="triggerTypeOptions" placeholder="全部类型" clearable size="small" />
            </div>
            <div class="filter-field" style="min-width: 240px; max-width: 280px;">
              <label>时间范围</label>
              <n-date-picker 
                v-model:value="filters.dateRange" 
                type="daterange" 
                clearable
                size="small"
                :shortcuts="dateShortcuts"
              />
            </div>
            <div class="filter-field filter-actions">
              <n-button size="small" @click="handleReset">重置</n-button>
              <n-button size="small" type="primary" @click="handleSearch">查询</n-button>
            </div>
          </div>
        </div>
      </transition>

      <!-- 执行记录表格 -->
      <div class="table-section">
        <n-data-table
          :columns="columns"
          :data="filteredInstances"
          :pagination="pagination"
          :row-key="row => row.id"
          class="instance-table"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <n-drawer v-model:show="showDetail" :width="500">
      <n-drawer-content :title="`执行详情 #${detailInstance?.id}`">
        <template v-if="detailInstance">
          <n-descriptions :column="1" bordered>
            <n-descriptions-item label="实例ID">{{ detailInstance.id }}</n-descriptions-item>
            <n-descriptions-item label="任务ID">{{ detailInstance.jobId }}</n-descriptions-item>
            <n-descriptions-item label="执行器ID">{{ detailInstance.executorId }}</n-descriptions-item>
            <n-descriptions-item label="执行器地址">
              <n-tag size="small">{{ detailInstance.executorAddress || '-' }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="触发类型">
              <n-tag size="small" :type="getTriggerTypeColor(detailInstance.triggerType)">
                {{ getTriggerTypeLabel(detailInstance.triggerType) }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="执行状态">
              <n-tag :type="getStatusType(detailInstance.status)">
                {{ getStatusLabel(detailInstance.status) }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="触发时间">{{ detailInstance.triggerTime }}</n-descriptions-item>
            <n-descriptions-item label="开始时间">{{ detailInstance.startTime || '-' }}</n-descriptions-item>
            <n-descriptions-item label="结束时间">{{ detailInstance.endTime || '-' }}</n-descriptions-item>
            <n-descriptions-item label="重试次数">{{ detailInstance.retryCount }}</n-descriptions-item>
          </n-descriptions>

          <div v-if="detailInstance.result" class="result-section">
            <h4>执行结果</h4>
            <n-code :code="formatJson(detailInstance.result)" language="json" />
          </div>

          <div v-if="detailInstance.errorMessage" class="error-section">
            <h4>错误信息</h4>
            <n-alert type="error" :title="detailInstance.errorMessage" />
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 日志弹窗 -->
    <transition name="modal">
      <div v-if="showLogModal" class="log-modal-overlay" @click.self="showLogModal = false">
        <div class="log-modal" :class="{ fullscreen: isLogFullscreen }">
          <!-- 日志控制台 -->
          <div class="log-console" :class="{ 'light-theme': isLightTheme }">
            <!-- 控制台头部 -->
            <div class="console-header">
              <div class="header-left">
                <div class="header-logo">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                    <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                    <path d="M7 8l4 4-4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <line x1="13" y1="16" x2="17" y2="16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </div>
                <div class="header-title">
                  <span class="title-main">执行日志</span>
                  <span class="title-separator">-</span>
                  <span class="title-sub">{{ getJobName(currentLogInstance?.jobId) }} #{{ currentLogInstance?.id }}</span>
                </div>
              </div>
              <div class="header-actions">
                <button class="action-btn" :class="{ active: showLogHighlight }" @click="showLogHighlight = !showLogHighlight" title="日志高亮">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                    <path d="M2 17l10 5 10-5"/>
                    <path d="M2 12l10 5 10-5"/>
                  </svg>
                </button>
                <button class="action-btn" @click="isLightTheme = !isLightTheme" :title="isLightTheme ? '切换暗色' : '切换亮色'">
                  <svg v-if="isLightTheme" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                    <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
                  </svg>
                </button>
                <button class="action-btn" @click="isLogFullscreen = !isLogFullscreen" :title="isLogFullscreen ? '退出全屏' : '全屏'">
                  <svg v-if="isLogFullscreen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/>
                    <line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/>
                    <line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/>
                  </svg>
                </button>
                <button class="action-btn" @click="handleExportLog" title="下载">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                </button>
                <button class="action-btn" @click="handleRefreshLog" title="刷新">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M23 4v6h-6"/><path d="M1 20v-6h6"/>
                    <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
                  </svg>
                </button>
                <button class="close-btn" @click="showLogModal = false">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 工具栏 -->
            <div class="console-toolbar">
              <div class="filter-group">
                <button 
                  class="filter-tab" 
                  :class="{ active: logFilterLevel === null }"
                  @click="logFilterLevel = null"
                >
                  <span class="tab-label">ALL</span>
                  <span class="tab-count">{{ currentLogList.length }}</span>
                </button>
                <button 
                  class="filter-tab info" 
                  :class="{ active: logFilterLevel === 'INFO' }"
                  @click="logFilterLevel = 'INFO'"
                >
                  <span class="tab-label">INFO</span>
                  <span class="tab-count">{{ currentLogList.filter(l => l.logLevel === 'INFO').length }}</span>
                </button>
                <button 
                  class="filter-tab warn" 
                  :class="{ active: logFilterLevel === 'WARN' }"
                  @click="logFilterLevel = 'WARN'"
                >
                  <span class="tab-label">WARN</span>
                  <span class="tab-count">{{ currentLogList.filter(l => l.logLevel === 'WARN').length }}</span>
                </button>
                <button 
                  class="filter-tab error" 
                  :class="{ active: logFilterLevel === 'ERROR' }"
                  @click="logFilterLevel = 'ERROR'"
                >
                  <span class="tab-label">ERROR</span>
                  <span class="tab-count">{{ currentLogList.filter(l => l.logLevel === 'ERROR').length }}</span>
                </button>
              </div>
              <div class="search-box-log">
                <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input 
                  v-model="logSearchKeyword" 
                  type="text" 
                  class="search-input" 
                  placeholder="搜索日志内容..."
                />
                <button v-if="logSearchKeyword" class="search-clear" @click="logSearchKeyword = ''">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 日志内容 -->
            <div class="console-logs">
              <div 
                v-for="(log, index) in filteredLogList" 
                :key="log.id" 
                class="log-row"
                :class="[log.logLevel.toLowerCase(), { 'no-highlight': !showLogHighlight }]"
              >
                <span class="log-line">{{ String(index + 1).padStart(3, ' ') }}</span>
                <span class="log-time">{{ log.createTime }}</span>
<span class="log-level" :class="log.logLevel.toLowerCase()">
                              <span class="level-dot"></span>
                              {{ log.logLevel }}
                            </span>
                <span class="log-thread">[{{ log.thread }}]</span>
                <span class="log-logger">{{ log.logger }}</span>
                <span class="log-msg">{{ log.content }}</span>
              </div>
              <div v-if="filteredLogList.length === 0" class="no-logs">
                <div class="empty-illustration">
                  <svg viewBox="0 0 120 120" fill="none">
                    <circle cx="60" cy="60" r="50" stroke="currentColor" stroke-width="2" opacity="0.2"/>
                    <path d="M40 45h40M40 60h30M40 75h35" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.3"/>
                    <circle cx="85" cy="85" r="20" stroke="currentColor" stroke-width="3" opacity="0.4"/>
                    <line x1="100" y1="100" x2="115" y2="115" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.4"/>
                  </svg>
                </div>
                <span class="empty-title">暂无日志数据</span>
                <span class="empty-desc">日志将在任务执行时自动产生</span>
              </div>
            </div>

            <!-- 底部状态栏 -->
            <div class="console-footer">
              <div class="footer-left">
                <span class="footer-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                    <path d="M14 2v6h6"/>
                  </svg>
                  {{ filteredLogList.length }} 条记录
                </span>
              </div>
              <div class="footer-right">
                <span class="footer-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                  </svg>
                  最后更新: {{ lastLogUpdateTime }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h } from 'vue'
import { NButton, NTag, NTooltip, useMessage } from 'naive-ui'

const message = useMessage()

// 搜索关键词
const searchKeyword = ref('')

// 高级筛选
const showAdvancedFilter = ref(false)
const filters = reactive({
  jobId: null,
  status: null,
  triggerType: null,
  dateRange: null
})

// 详情抽屉
const showDetail = ref(false)
const detailInstance = ref(null)

// 日志弹窗
const showLogModal = ref(false)
const isLogFullscreen = ref(false)
const isLightTheme = ref(true)
const showLogHighlight = ref(true)
const currentLogInstance = ref(null)
const logFilterLevel = ref(null)
const logSearchKeyword = ref('')
const lastLogUpdateTime = ref('13:26:49')

// 计算激活的筛选条件数量
const activeFilterCount = computed(() => {
  let count = 0
  if (filters.jobId) count++
  if (filters.status !== null) count++
  if (filters.triggerType) count++
  if (filters.dateRange) count++
  return count
})

// 选项
const jobOptions = [
  { label: 'dataSyncHandler', value: 1 },
  { label: 'reportGenHandler', value: 2 },
  { label: 'emailSendHandler', value: 3 }
]

const statusOptions = [
  { label: '待执行', value: 0 },
  { label: '已发送', value: 1 },
  { label: '发送失败', value: 2 },
  { label: '执行中', value: 3 },
  { label: '成功', value: 4 },
  { label: '失败', value: 5 },
  { label: '超时', value: 6 }
]

const triggerTypeOptions = [
  { label: '自动调度', value: 'auto' },
  { label: '手动触发', value: 'manual' },
  { label: 'API调用', value: 'api' }
]

const dateShortcuts = {
  '今天': () => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return [today.getTime(), Date.now()]
  },
  '最近7天': () => {
    const end = Date.now()
    const start = end - 7 * 24 * 60 * 60 * 1000
    return [start, end]
  },
  '最近30天': () => {
    const end = Date.now()
    const start = end - 30 * 24 * 60 * 60 * 1000
    return [start, end]
  }
}

// 执行记录列表
const instanceList = ref([
  {
    id: 10001,
    jobId: 1,
    executorId: 1,
    triggerTime: '2026-01-26 02:00:00',
    startTime: '2026-01-26 02:00:01',
    endTime: '2026-01-26 02:00:05',
    status: 4,
    retryCount: 0,
    result: '{"synced": 1024, "duration": "4s"}',
    errorMessage: null,
    triggerType: 'auto',
    executorAddress: '192.168.1.101:9999'
  },
  {
    id: 10002,
    jobId: 2,
    executorId: 1,
    triggerTime: '2026-01-26 08:00:00',
    startTime: '2026-01-26 08:00:01',
    endTime: '2026-01-26 08:00:12',
    status: 4,
    retryCount: 0,
    result: '{"reportId": "RPT-20260126", "size": "2.3MB"}',
    errorMessage: null,
    triggerType: 'auto',
    executorAddress: '192.168.1.102:9999'
  },
  {
    id: 10003,
    jobId: 3,
    executorId: 2,
    triggerTime: '2026-01-26 09:30:00',
    startTime: '2026-01-26 09:30:01',
    endTime: '2026-01-26 09:30:03',
    status: 5,
    retryCount: 2,
    result: null,
    errorMessage: 'SMTP connection timeout',
    triggerType: 'auto',
    executorAddress: '192.168.1.110:9999'
  },
  {
    id: 10004,
    jobId: 1,
    executorId: 1,
    triggerTime: '2026-01-26 11:00:00',
    startTime: '2026-01-26 11:00:01',
    endTime: null,
    status: 3,
    retryCount: 0,
    result: null,
    errorMessage: null,
    triggerType: 'manual',
    executorAddress: '192.168.1.101:9999'
  },
  {
    id: 10005,
    jobId: 1,
    executorId: 1,
    triggerTime: '2026-01-26 10:30:00',
    startTime: '2026-01-26 10:30:01',
    endTime: '2026-01-26 10:35:00',
    status: 6,
    retryCount: 0,
    result: null,
    errorMessage: '任务执行超时',
    triggerType: 'api',
    executorAddress: '192.168.1.103:9999'
  }
])

// 模拟日志数据
const currentLogList = computed(() => {
  if (!currentLogInstance.value) return []
  const baseTime = currentLogInstance.value.startTime || '2026-01-26 00:00:00'
  return [
    {
      id: 1,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.i.LoggingJobInterceptor',
      content: `[Job-${currentLogInstance.value.jobId}] 开始执行: handler=${getJobName(currentLogInstance.value.jobId)}#execute()`,
      createTime: baseTime + '.001'
    },
    {
      id: 2,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '正在处理数据，共 1000 条记录',
      createTime: baseTime + '.125'
    },
    {
      id: 3,
      logLevel: 'DEBUG',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '连接数据库: jdbc:mysql://localhost:3306/pulse_job',
      createTime: baseTime + '.230'
    },
    {
      id: 4,
      logLevel: 'WARN',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '发现 3 条数据格式异常，已跳过处理',
      createTime: baseTime + '.350'
    },
    {
      id: 5,
      logLevel: 'ERROR',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.DataProcessor',
      content: 'java.lang.NullPointerException: Cannot invoke method on null object at line 128',
      createTime: baseTime + '.380'
    },
    {
      id: 6,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '数据处理完成: 成功 996 条, 失败 1 条, 跳过 3 条',
      createTime: baseTime + '.456'
    },
    {
      id: 7,
      logLevel: 'WARN',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.scheduler.RetryHandler',
      content: '部分任务需要重试，已加入重试队列',
      createTime: baseTime + '.520'
    },
    {
      id: 8,
      logLevel: 'ERROR',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.scheduler.RetryHandler',
      content: '重试失败: 达到最大重试次数 3 次，放弃处理',
      createTime: baseTime + '.600'
    },
    {
      id: 9,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '所有数据处理完毕',
      createTime: baseTime + '.650'
    },
    {
      id: 10,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.i.LoggingJobInterceptor',
      content: `[Job-${currentLogInstance.value.jobId}] 执行完成，耗时: 649ms`,
      createTime: (currentLogInstance.value.endTime || baseTime) + '.789'
    }
  ]
})

// 过滤后的日志列表
const filteredLogList = computed(() => {
  return currentLogList.value.filter(log => {
    if (logSearchKeyword.value) {
      const keyword = logSearchKeyword.value.toLowerCase()
      if (!log.content.toLowerCase().includes(keyword) && 
          !log.logger.toLowerCase().includes(keyword)) return false
    }
    if (logFilterLevel.value && log.logLevel !== logFilterLevel.value) {
      return false
    }
    return true
  })
})

// 过滤后的列表
const filteredInstances = computed(() => {
  return instanceList.value.filter(instance => {
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      const matchKeyword = String(instance.id).includes(keyword) ||
             (instance.executorAddress && instance.executorAddress.toLowerCase().includes(keyword))
      if (!matchKeyword) return false
    }
    if (filters.jobId && instance.jobId !== filters.jobId) {
      return false
    }
    if (filters.status !== null && instance.status !== filters.status) {
      return false
    }
    if (filters.triggerType && instance.triggerType !== filters.triggerType) {
      return false
    }
    return true
  })
})

// 分页配置
const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  showQuickJumper: true,
  pageSizes: [10, 20, 50, 100],
  prefix: ({ itemCount }) => `共 ${itemCount} 条`,
  onChange: (page) => {
    pagination.page = page
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
  }
})

// 状态相关
const getStatusLabel = (status) => {
  const map = { 0: '待执行', 1: '已发送', 2: '发送失败', 3: '执行中', 4: '成功', 5: '失败', 6: '超时', 7: '已取消' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 0: 'default', 1: 'info', 2: 'error', 3: 'warning', 4: 'success', 5: 'error', 6: 'warning', 7: 'default' }
  return map[status] || 'default'
}

const getTriggerTypeLabel = (type) => {
  const map = { auto: '自动调度', manual: '手动触发', api: 'API调用' }
  return map[type] || type
}

const getTriggerTypeColor = (type) => {
  const map = { auto: 'success', manual: 'info', api: 'warning' }
  return map[type] || 'default'
}

const getJobName = (jobId) => {
  const map = { 1: 'dataSyncHandler', 2: 'reportGenHandler', 3: 'emailSendHandler' }
  return map[jobId] || `Job-${jobId}`
}

const formatJson = (str) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

// 表格列
const columns = [
  { title: 'ID', key: 'id', width: 90 },
  { title: '任务ID', key: 'jobId', width: 80 },
  { 
    title: '触发类型', 
    key: 'triggerType',
    width: 100,
    render: (row) => h(NTag, { size: 'small', type: getTriggerTypeColor(row.triggerType) }, () => getTriggerTypeLabel(row.triggerType))
  },
  { 
    title: '状态', 
    key: 'status',
    width: 90,
    render: (row) => h(NTag, { type: getStatusType(row.status), size: 'small' }, () => getStatusLabel(row.status))
  },
  { 
    title: '执行器地址', 
    key: 'executorAddress', 
    width: 160,
    render: (row) => h('code', { class: 'address-cell' }, row.executorAddress || '-')
  },
  { title: '触发时间', key: 'triggerTime', width: 160 },
  { title: '开始时间', key: 'startTime', width: 160 },
  { title: '结束时间', key: 'endTime', width: 160 },
  { title: '重试', key: 'retryCount', width: 60 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row) => h('div', { class: 'action-buttons' }, [
      h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h('button', {
          class: 'action-btn action-btn-info',
          onClick: () => handleViewDetail(row)
        }, [
          h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('circle', { cx: '11', cy: '11', r: '8' }),
            h('line', { x1: '21', y1: '21', x2: '16.65', y2: '16.65' })
          ])
        ]),
        default: () => '详情'
      }),
      h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h('button', {
          class: 'action-btn action-btn-success',
          onClick: () => handleViewLog(row)
        }, [
          h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
            h('polyline', { points: '14 2 14 8 20 8' }),
            h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
            h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
            h('polyline', { points: '10 9 9 9 8 9' })
          ])
        ]),
        default: () => '日志'
      })
    ])
  }
]

// 事件处理
const handleSearch = () => {
  pagination.page = 1
  message.success('查询完成')
}

const handleReset = () => {
  searchKeyword.value = ''
  filters.jobId = null
  filters.status = null
  filters.triggerType = null
  filters.dateRange = null
  pagination.page = 1
}

const handleRefresh = () => {
  message.success('刷新成功')
}

const handleViewDetail = (row) => {
  detailInstance.value = row
  showDetail.value = true
}

const handleViewLog = (row) => {
  currentLogInstance.value = row
  logFilterLevel.value = null
  logSearchKeyword.value = ''
  showLogModal.value = true
}

const handleRefreshLog = () => {
  message.success('日志刷新成功')
}

const handleExportLog = () => {
  const content = filteredLogList.value.map(log => 
    `${log.createTime} ${log.logLevel.padEnd(5)} [${log.thread}] ${log.logger} ${log.content}`
  ).join('\n')
  
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `pulse-job-logs-${currentLogInstance.value?.id}-${new Date().toISOString().slice(0, 10)}.log`
  a.click()
  URL.revokeObjectURL(url)
  
  message.success('导出成功')
}
</script>

<style scoped>
.instance-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ==================== 统计卡片 ==================== */
.stats-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-icon.blue { background: rgba(94, 129, 244, 0.1); color: #5E81F4; }
.stat-icon.green { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
.stat-icon.red { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.stat-icon.orange { background: rgba(249, 115, 22, 0.1); color: #f97316; }

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-top: 4px;
}

/* ==================== 列表卡片 ==================== */
.list-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

/* ==================== 工具栏样式 ==================== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box {
  display: flex;
  align-items: center;
  width: 260px;
  height: 36px;
  padding: 0 12px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.search-box:focus-within {
  background: #fff;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(94, 129, 244, 0.1);
}

.search-icon {
  width: 16px;
  height: 16px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.875rem;
  color: var(--text-primary);
  margin-left: 8px;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.filter-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: var(--primary-color);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: background 0.15s ease;
}

.filter-btn svg {
  width: 16px;
  height: 16px;
  color: #fff;
}

.filter-btn:hover {
  background: #7d9df7;
}

.filter-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 18px;
  height: 18px;
  padding: 0;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: #ef4444;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  text-align: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: var(--text-secondary);
}

.tool-btn svg {
  width: 18px;
  height: 18px;
}

.tool-btn:hover {
  background: var(--bg-hover);
  color: var(--primary-color);
}

/* ==================== 高级筛选面板 ==================== */
.advanced-filter {
  background: #fff;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 140px;
  flex: 1;
  max-width: 180px;
}

.filter-field label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-muted);
}

.filter-field.filter-actions {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  max-width: none;
  flex: none;
  margin-left: auto;
}

.filter-field :deep(.n-base-selection:focus),
.filter-field :deep(.n-base-selection--focus) {
  box-shadow: none !important;
}

.filter-field :deep(.n-base-selection--active) {
  box-shadow: none !important;
}

/* 动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.slide-down-enter-to,
.slide-down-leave-from {
  opacity: 1;
  max-height: 100px;
}

/* ==================== 表格区域 ==================== */
.table-section {
  background: #fff;
  padding: 16px 20px;
}

.instance-table :deep(.n-data-table__pagination) {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.instance-table :deep(.n-pagination-prefix) {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.instance-table :deep(.address-cell) {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  background: transparent;
}

.instance-table :deep(.action-buttons) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.instance-table :deep(.action-btn) {
  background: transparent;
  border: none;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.instance-table :deep(.action-btn svg) {
  width: 16px;
  height: 16px;
}

.instance-table :deep(.action-btn-info) {
  color: var(--text-secondary);
}

.instance-table :deep(.action-btn-info:hover) {
  color: #3b82f6;
}

.instance-table :deep(.action-btn-success) {
  color: var(--text-secondary);
}

.instance-table :deep(.action-btn-success:hover) {
  color: #10b981;
}

/* ==================== 详情抽屉 ==================== */
.result-section,
.error-section {
  margin-top: 20px;
}

.result-section h4,
.error-section h4 {
  margin: 0 0 12px 0;
  font-size: 0.875rem;
  color: var(--text-primary);
}

/* ==================== 日志弹窗 ==================== */
.log-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 40px;
}

.log-modal-overlay:has(.log-modal.fullscreen) {
  padding: 0;
}

.log-modal {
  width: 100%;
  max-width: 1200px;
  height: auto;
  max-height: calc(100vh - 80px);
  transition: all 0.3s ease;
}

.log-modal.fullscreen {
  max-width: 100vw;
  max-height: 100vh;
  width: 100vw;
  height: 100vh;
}

.log-modal.fullscreen .log-console {
  border-radius: 0;
  height: 100%;
  max-height: 100vh;
}

.log-modal.fullscreen .console-logs {
  max-height: calc(100vh - 140px);
  min-height: calc(100vh - 140px);
}

/* 弹窗动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .log-modal,
.modal-leave-to .log-modal {
  transform: scale(0.9);
  opacity: 0;
}

/* ==================== 日志控制台样式 ==================== */
.log-console {
  background: #0c1018;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 
    0 0 0 1px rgba(255, 255, 255, 0.05),
    0 25px 80px -10px rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: calc(100vh - 80px);
}

.console-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(to bottom, #161b26, #12161f);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #58a6ff;
  opacity: 0.9;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.title-main {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #c9d1d9;
  letter-spacing: 0.01em;
}

.title-separator {
  color: #484f58;
  font-weight: 400;
}

.title-sub {
  font-size: 0.875rem;
  color: #8b949e;
  font-weight: 400;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #8b949e;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #c9d1d9;
}

.action-btn.active {
  background: rgba(88, 166, 255, 0.15);
  color: #58a6ff;
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #8b949e;
  margin-left: 8px;
}

.close-btn svg {
  width: 16px;
  height: 16px;
}

.close-btn:hover {
  background: rgba(248, 81, 73, 0.15);
  color: #f85149;
}

/* 工具栏 */
.console-toolbar {
  background: rgba(22, 27, 34, 0.8);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.2);
  padding: 4px;
  border-radius: 10px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: #6e7681;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tab:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #8b949e;
}

.filter-tab.active {
  background: #238636;
  color: white;
  box-shadow: 0 2px 8px rgba(35, 134, 54, 0.3);
}

.filter-tab.warn.active {
  background: #9e6a03;
  box-shadow: 0 2px 8px rgba(158, 106, 3, 0.3);
}

.filter-tab.error.active {
  background: #da3633;
  box-shadow: 0 2px 8px rgba(218, 54, 51, 0.3);
}

.tab-count {
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.filter-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
}

.search-box-log {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  max-width: 320px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 0 14px;
  transition: all 0.2s ease;
}

.search-box-log:focus-within {
  border-color: rgba(255, 255, 255, 0.1);
}

.search-box-log .search-icon {
  color: #484f58;
}

.search-box-log .search-input {
  color: #c9d1d9;
  padding: 10px 0;
  margin-left: 0;
}

.search-box-log .search-input::placeholder {
  color: #484f58;
}

.search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 4px;
  color: #6e7681;
  cursor: pointer;
  transition: all 0.15s ease;
}

.search-clear svg {
  width: 12px;
  height: 12px;
}

.search-clear:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #c9d1d9;
}

/* 日志内容 */
.console-logs {
  padding: 16px 0;
  min-height: 300px;
  max-height: 400px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  flex: 1;
  background: #0c1018;
  color: #c9d1d9;
}

.log-row {
  padding: 10px 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border-left: 3px solid transparent;
  transition: all 0.15s ease;
}

.log-row:hover {
  background: rgba(255, 255, 255, 0.02);
}

.log-row.info { border-left-color: transparent; }
.log-row.warn { border-left-color: #d29922; background: rgba(210, 153, 34, 0.12); }
.log-row.error { border-left-color: #f85149; background: rgba(248, 81, 73, 0.08); }
.log-row.debug { border-left-color: transparent; }

.log-line {
  color: #484f58;
  min-width: 32px;
  text-align: right;
  font-size: 12px;
  user-select: none;
}

.log-time {
  color: #6e7681;
  min-width: 190px;
  flex-shrink: 0;
}

.log-level {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 60px;
  font-weight: 600;
  flex-shrink: 0;
}

.level-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.log-level.info { color: #3fb950; }
.log-level.info .level-dot { background: #3fb950; box-shadow: 0 0 6px rgba(63, 185, 80, 0.5); }
.log-level.warn { color: #d29922; }
.log-level.warn .level-dot { background: #d29922; box-shadow: 0 0 6px rgba(210, 153, 34, 0.5); }
.log-level.error { color: #f85149; }
.log-level.error .level-dot { background: #f85149; box-shadow: 0 0 6px rgba(248, 81, 73, 0.5); }
.log-level.debug { color: #8b949e; }
.log-level.debug .level-dot { background: #8b949e; }

/* 关闭高亮时移除背景色 */
.log-row.no-highlight.warn,
.log-row.no-highlight.error {
  background: transparent !important;
  border-left-color: transparent !important;
}

.log-row.no-highlight.error .log-msg {
  color: #c9d1d9 !important;
}

.log-console.light-theme .log-row.no-highlight.error .log-msg {
  color: #334155 !important;
}

.log-thread {
  color: #a371f7;
  min-width: 130px;
  flex-shrink: 0;
}

.log-logger {
  color: #58a6ff;
  min-width: 260px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-msg {
  color: #c9d1d9;
  flex: 1;
}

.log-row.error .log-msg {
  color: #f85149;
}

/* 底部状态栏 */
.console-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: rgba(22, 27, 34, 0.6);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6e7681;
  font-size: 12px;
}

.footer-item svg {
  width: 14px;
  height: 14px;
}

/* 空状态 */
.no-logs {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  color: #30363d;
}

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #8b949e;
}

.empty-desc {
  font-size: 13px;
  color: #6e7681;
}

/* 滚动条样式 */
.console-logs::-webkit-scrollbar {
  width: 10px;
}

.console-logs::-webkit-scrollbar-track {
  background: transparent;
}

.console-logs::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 5px;
  border: 2px solid transparent;
  background-clip: content-box;
}

.console-logs::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
  background-clip: content-box;
}

/* ==================== 白色主题 ==================== */
/* 亮色主题 - 容器 */
.log-console.light-theme {
  background: #fff;
  box-shadow: 
    0 0 0 1px rgba(0, 0, 0, 0.08),
    0 25px 80px -10px rgba(0, 0, 0, 0.15);
}

/* 亮色主题 - 头部 */
.log-console.light-theme .console-header {
  background: linear-gradient(to bottom, #fafbfc, #fff);
  border-bottom: 1px solid #f0f0f0;
}

.log-console.light-theme .header-logo { color: #5E81F4; }
.log-console.light-theme .title-main { color: #555; }
.log-console.light-theme .title-separator { color: #ccc; }
.log-console.light-theme .title-sub { color: #888; }

.log-console.light-theme .action-btn { color: #999; }
.log-console.light-theme .action-btn:hover { background: #f5f5f5; color: #666; }
.log-console.light-theme .action-btn.active { background: rgba(94, 129, 244, 0.1); color: #5E81F4; }

.log-console.light-theme .close-btn { color: #999; }
.log-console.light-theme .close-btn:hover { background: #f5f5f5; color: #666; }

.log-console.light-theme .console-toolbar {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.log-console.light-theme .filter-group {
  background: white;
  border: 1px solid #e2e8f0;
}

.log-console.light-theme .filter-tab {
  color: #64748b;
}

.log-console.light-theme .filter-tab:hover {
  background: #f1f5f9;
  color: #334155;
}

.log-console.light-theme .filter-tab.active {
  background: #10b981;
}

.log-console.light-theme .filter-tab.warn.active {
  background: #f59e0b;
}

.log-console.light-theme .filter-tab.error.active {
  background: #ef4444;
}

.log-console.light-theme .tab-count {
  background: rgba(0, 0, 0, 0.06);
}

.log-console.light-theme .search-box-log {
  background: white;
  border: 1px solid #e2e8f0;
}

.log-console.light-theme .search-box-log:focus-within {
  border-color: #cbd5e1;
}

.log-console.light-theme .search-box-log .search-icon { color: #94a3b8; }
.log-console.light-theme .search-box-log .search-input { color: #1e293b; }
.log-console.light-theme .search-box-log .search-input::placeholder { color: #94a3b8; }

.log-console.light-theme .console-logs { background: #ffffff; }

.log-console.light-theme .log-row:hover { background: #f8fafc; }
.log-console.light-theme .log-row.warn { background: rgba(245, 158, 11, 0.06); }
.log-console.light-theme .log-row.error { background: rgba(239, 68, 68, 0.06); }

.log-console.light-theme .log-line { color: #cbd5e1; }
.log-console.light-theme .log-time { color: #94a3b8; }
.log-console.light-theme .log-level.info { color: #059669; }
.log-console.light-theme .log-level.info .level-dot { background: #059669; box-shadow: 0 0 6px rgba(5, 150, 105, 0.4); }
.log-console.light-theme .log-level.warn { color: #d97706; }
.log-console.light-theme .log-level.warn .level-dot { background: #d97706; box-shadow: 0 0 6px rgba(217, 119, 6, 0.4); }
.log-console.light-theme .log-level.error { color: #dc2626; }
.log-console.light-theme .log-level.error .level-dot { background: #dc2626; box-shadow: 0 0 6px rgba(220, 38, 38, 0.4); }
.log-console.light-theme .log-level.debug { color: #94a3b8; }
.log-console.light-theme .log-level.debug .level-dot { background: #94a3b8; }

.log-console.light-theme .log-thread { color: #7c3aed; }
.log-console.light-theme .log-logger { color: #2563eb; }
.log-console.light-theme .log-msg { color: #334155; }
.log-console.light-theme .log-row.error .log-msg { color: #dc2626; }

.log-console.light-theme .console-footer {
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.log-console.light-theme .footer-item { color: #64748b; }

.log-console.light-theme .empty-illustration { color: #e2e8f0; }
.log-console.light-theme .empty-title { color: #64748b; }
.log-console.light-theme .empty-desc { color: #94a3b8; }

.log-console.light-theme .console-logs::-webkit-scrollbar-thumb {
  background: #cbd5e1;
}

.log-console.light-theme .console-logs::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stat-item {
    padding: 12px;
    background: #fafbfc;
    border-radius: 10px;
  }
  
  .log-logger {
    display: none;
  }
}

@media (max-width: 640px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-field {
    max-width: none;
  }
  
  .filter-field.filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
  
  .log-modal-overlay {
    padding: 20px;
  }
}
</style>
