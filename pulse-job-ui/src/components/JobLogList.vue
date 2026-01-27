<template>
  <div class="log-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">执行日志</h2>
      <div class="header-actions">
        <n-input-group>
          <n-select 
            v-model:value="filterJobId" 
            :options="jobOptions" 
            placeholder="全部任务" 
            clearable 
            style="width: 160px"
          />
          <n-select 
            v-model:value="filterInstanceId" 
            :options="instanceOptions" 
            placeholder="全部实例" 
            clearable 
            style="width: 160px"
          />
          <n-select 
            v-model:value="filterLevel" 
            :options="levelOptions" 
            placeholder="全部级别" 
            clearable 
            style="width: 120px"
          />
        </n-input-group>
        <n-input 
          v-model:value="searchKeyword" 
          placeholder="搜索日志内容..." 
          clearable 
          style="width: 200px"
        >
          <template #prefix>
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
            </svg>
          </template>
        </n-input>
        <n-date-picker 
          v-model:value="filterDateRange" 
          type="datetimerange" 
          clearable
          :shortcuts="dateShortcuts"
        />
        <n-button @click="handleRefresh">
          <template #icon>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6"/><path d="M1 20v-6h6"/>
              <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
            </svg>
          </template>
          刷新
        </n-button>
        <n-button type="error" @click="handleClearLogs" :disabled="filteredLogs.length === 0">
          <template #icon>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
            </svg>
          </template>
          清空
        </n-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon blue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <path d="M14 2v6h6"/><path d="M16 13H8"/><path d="M16 17H8"/><path d="M10 9H8"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ logList.length }}</span>
          <span class="stat-label">总日志数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon cyan">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ logList.filter(l => l.logLevel === 'INFO').length }}</span>
          <span class="stat-label">INFO</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
            <path d="M12 9v4"/><path d="M12 17h.01"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ logList.filter(l => l.logLevel === 'WARN').length }}</span>
          <span class="stat-label">WARN</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ logList.filter(l => l.logLevel === 'ERROR').length }}</span>
          <span class="stat-label">ERROR</span>
        </div>
      </div>
    </div>

    <!-- 日志控制台 -->
    <div class="log-console">
      <div class="console-header">
        <div class="console-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/>
          </svg>
          <span>日志控制台</span>
          <n-tag size="small" :bordered="false">{{ filteredLogs.length }} 条记录</n-tag>
        </div>
        <div class="console-actions">
          <n-switch v-model:value="autoScroll" size="small">
            <template #checked>自动滚动</template>
            <template #unchecked>自动滚动</template>
          </n-switch>
          <n-button quaternary size="small" @click="handleExport">
            <template #icon>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
            </template>
            导出
          </n-button>
        </div>
      </div>
      <div class="console-body" ref="consoleRef">
        <div 
          v-for="log in filteredLogs" 
          :key="log.id" 
          class="log-entry"
          :class="log.logLevel.toLowerCase()"
        >
          <span class="log-time">{{ formatTime(log.createTime) }}</span>
          <span class="log-level" :class="log.logLevel.toLowerCase()">{{ log.logLevel }}</span>
          <span class="log-instance">[Job:{{ log.jobId }} / Instance:{{ log.instanceId }}]</span>
          <span class="log-content">{{ log.content }}</span>
        </div>
        <div v-if="filteredLogs.length === 0" class="no-logs">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <path d="M14 2v6h6"/>
          </svg>
          <span>暂无日志数据</span>
        </div>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <n-drawer v-model:show="showDetail" :width="600">
      <n-drawer-content :title="`日志详情 #${detailLog?.id}`">
        <template v-if="detailLog">
          <n-descriptions :column="1" bordered>
            <n-descriptions-item label="日志ID">{{ detailLog.id }}</n-descriptions-item>
            <n-descriptions-item label="任务ID">{{ detailLog.jobId }}</n-descriptions-item>
            <n-descriptions-item label="实例ID">{{ detailLog.instanceId }}</n-descriptions-item>
            <n-descriptions-item label="日志级别">
              <n-tag :type="getLevelType(detailLog.logLevel)">{{ detailLog.logLevel }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="创建时间">{{ detailLog.createTime }}</n-descriptions-item>
          </n-descriptions>

          <div class="content-section">
            <h4>日志内容</h4>
            <n-code :code="detailLog.content" language="text" :word-wrap="true" />
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useMessage } from 'naive-ui'

const message = useMessage()

// 筛选条件
const filterJobId = ref(null)
const filterInstanceId = ref(null)
const filterLevel = ref(null)
const filterDateRange = ref(null)
const searchKeyword = ref('')
const autoScroll = ref(true)

// 详情抽屉
const showDetail = ref(false)
const detailLog = ref(null)
const consoleRef = ref(null)

// 选项
const jobOptions = [
  { label: 'dataSyncHandler', value: 1 },
  { label: 'reportGenHandler', value: 2 },
  { label: 'emailSendHandler', value: 3 },
  { label: 'cacheRefreshHandler', value: 4 }
]

const instanceOptions = [
  { label: '实例 #10001', value: 10001 },
  { label: '实例 #10002', value: 10002 },
  { label: '实例 #10003', value: 10003 },
  { label: '实例 #10004', value: 10004 }
]

const levelOptions = [
  { label: 'DEBUG', value: 'DEBUG' },
  { label: 'INFO', value: 'INFO' },
  { label: 'WARN', value: 'WARN' },
  { label: 'ERROR', value: 'ERROR' }
]

const dateShortcuts = {
  '最近1小时': () => {
    const end = Date.now()
    const start = end - 60 * 60 * 1000
    return [start, end]
  },
  '最近6小时': () => {
    const end = Date.now()
    const start = end - 6 * 60 * 60 * 1000
    return [start, end]
  },
  '今天': () => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return [today.getTime(), Date.now()]
  },
  '最近7天': () => {
    const end = Date.now()
    const start = end - 7 * 24 * 60 * 60 * 1000
    return [start, end]
  }
}

// 日志列表
const logList = ref([
  {
    id: 1,
    jobId: 1,
    instanceId: 10001,
    logLevel: 'INFO',
    content: '任务开始执行: dataSyncHandler',
    createTime: '2026-01-26 02:00:01'
  },
  {
    id: 2,
    jobId: 1,
    instanceId: 10001,
    logLevel: 'DEBUG',
    content: '连接数据库: jdbc:mysql://localhost:3306/pulse_job',
    createTime: '2026-01-26 02:00:02'
  },
  {
    id: 3,
    jobId: 1,
    instanceId: 10001,
    logLevel: 'INFO',
    content: '开始同步数据表: user_info, 预计同步 1024 条记录',
    createTime: '2026-01-26 02:00:02'
  },
  {
    id: 4,
    jobId: 1,
    instanceId: 10001,
    logLevel: 'INFO',
    content: '数据同步完成: 成功 1020 条, 跳过 4 条',
    createTime: '2026-01-26 02:00:04'
  },
  {
    id: 5,
    jobId: 1,
    instanceId: 10001,
    logLevel: 'INFO',
    content: '任务执行完成, 耗时: 3.2s',
    createTime: '2026-01-26 02:00:05'
  },
  {
    id: 6,
    jobId: 2,
    instanceId: 10002,
    logLevel: 'INFO',
    content: '任务开始执行: reportGenHandler',
    createTime: '2026-01-26 08:00:01'
  },
  {
    id: 7,
    jobId: 2,
    instanceId: 10002,
    logLevel: 'INFO',
    content: '正在生成日报表: report_2026-01-26.pdf',
    createTime: '2026-01-26 08:00:05'
  },
  {
    id: 8,
    jobId: 2,
    instanceId: 10002,
    logLevel: 'WARN',
    content: '部分数据缺失, 已使用默认值填充: region=华东',
    createTime: '2026-01-26 08:00:08'
  },
  {
    id: 9,
    jobId: 2,
    instanceId: 10002,
    logLevel: 'INFO',
    content: '报表生成完成, 文件大小: 2.3MB',
    createTime: '2026-01-26 08:00:12'
  },
  {
    id: 10,
    jobId: 3,
    instanceId: 10003,
    logLevel: 'INFO',
    content: '任务开始执行: emailSendHandler',
    createTime: '2026-01-26 09:30:01'
  },
  {
    id: 11,
    jobId: 3,
    instanceId: 10003,
    logLevel: 'WARN',
    content: 'SMTP服务器响应缓慢, 正在重试连接...',
    createTime: '2026-01-26 09:30:02'
  },
  {
    id: 12,
    jobId: 3,
    instanceId: 10003,
    logLevel: 'ERROR',
    content: 'SMTP连接超时: Connection timed out after 30000ms. Host: smtp.example.com:465',
    createTime: '2026-01-26 09:30:03'
  },
  {
    id: 13,
    jobId: 4,
    instanceId: 10004,
    logLevel: 'INFO',
    content: '任务开始执行: cacheRefreshHandler',
    createTime: '2026-01-26 11:00:01'
  },
  {
    id: 14,
    jobId: 4,
    instanceId: 10004,
    logLevel: 'DEBUG',
    content: '正在刷新缓存键: user:*, product:*, order:recent',
    createTime: '2026-01-26 11:00:02'
  },
  {
    id: 15,
    jobId: 4,
    instanceId: 10004,
    logLevel: 'INFO',
    content: '缓存刷新完成: 清理 256 个键, 预热 128 个键',
    createTime: '2026-01-26 11:00:03'
  }
])

// 过滤后的列表
const filteredLogs = computed(() => {
  let list = logList.value
  
  if (filterJobId.value) {
    list = list.filter(l => l.jobId === filterJobId.value)
  }
  if (filterInstanceId.value) {
    list = list.filter(l => l.instanceId === filterInstanceId.value)
  }
  if (filterLevel.value) {
    list = list.filter(l => l.logLevel === filterLevel.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(l => l.content.toLowerCase().includes(keyword))
  }
  
  return list
})

// 监听日志变化，自动滚动
watch(filteredLogs, () => {
  if (autoScroll.value) {
    nextTick(() => {
      if (consoleRef.value) {
        consoleRef.value.scrollTop = consoleRef.value.scrollHeight
      }
    })
  }
})

// 工具方法
const getLevelType = (level) => {
  const map = { DEBUG: 'default', INFO: 'info', WARN: 'warning', ERROR: 'error' }
  return map[level] || 'default'
}

const formatTime = (time) => {
  if (!time) return '-'
  // 只显示时间部分
  const date = new Date(time.replace(' ', 'T'))
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit',
    hour12: false
  })
}

// 事件处理
const handleRefresh = () => {
  message.success('刷新成功')
}

const handleClearLogs = () => {
  logList.value = []
  message.success('日志已清空')
}

const handleExport = () => {
  const content = filteredLogs.value.map(log => 
    `[${log.createTime}] [${log.logLevel}] [Job:${log.jobId}/Instance:${log.instanceId}] ${log.content}`
  ).join('\n')
  
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `pulse-job-logs-${new Date().toISOString().slice(0, 10)}.log`
  a.click()
  URL.revokeObjectURL(url)
  
  message.success('导出成功')
}

const handleViewDetail = (log) => {
  detailLog.value = log
  showDetail.value = true
}
</script>

<style scoped>
.log-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.search-icon {
  width: 16px;
  height: 16px;
  color: var(--text-muted);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--border-color);
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
.stat-icon.cyan { background: rgba(6, 182, 212, 0.1); color: #06b6d4; }
.stat-icon.orange { background: rgba(249, 115, 22, 0.1); color: #f97316; }
.stat-icon.red { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

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

/* 日志控制台 */
.log-console {
  background: #1e1e2e;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.console-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #181825;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.console-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #cdd6f4;
  font-size: 0.875rem;
  font-weight: 500;
}

.console-title svg {
  width: 16px;
  height: 16px;
  color: #89b4fa;
}

.console-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.console-body {
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', 'Fira Code', 'Monaco', 'Menlo', monospace;
  font-size: 0.8125rem;
  line-height: 1.6;
}

.log-entry {
  display: flex;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  margin-bottom: 2px;
  transition: background 0.15s ease;
}

.log-entry:hover {
  background: rgba(255, 255, 255, 0.03);
}

.log-entry.debug { border-left: 2px solid #6c7086; }
.log-entry.info { border-left: 2px solid #89b4fa; }
.log-entry.warn { border-left: 2px solid #f9e2af; }
.log-entry.error { border-left: 2px solid #f38ba8; }

.log-time {
  color: #6c7086;
  flex-shrink: 0;
  font-size: 0.75rem;
}

.log-level {
  font-weight: 600;
  flex-shrink: 0;
  padding: 0 4px;
  border-radius: 2px;
  font-size: 0.6875rem;
  text-transform: uppercase;
}

.log-level.debug { color: #6c7086; background: rgba(108, 112, 134, 0.15); }
.log-level.info { color: #89b4fa; background: rgba(137, 180, 250, 0.15); }
.log-level.warn { color: #f9e2af; background: rgba(249, 226, 175, 0.15); }
.log-level.error { color: #f38ba8; background: rgba(243, 139, 168, 0.15); }

.log-instance {
  color: #a6adc8;
  flex-shrink: 0;
  font-size: 0.75rem;
}

.log-content {
  color: #cdd6f4;
  word-break: break-all;
}

.no-logs {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #6c7086;
  gap: 12px;
}

.no-logs svg {
  width: 48px;
  height: 48px;
  opacity: 0.5;
}

/* 详情部分 */
.content-section {
  margin-top: 20px;
}

.content-section h4 {
  margin: 0 0 12px 0;
  font-size: 0.875rem;
  color: var(--text-primary);
}

/* 滚动条样式 */
.console-body::-webkit-scrollbar {
  width: 8px;
}

.console-body::-webkit-scrollbar-track {
  background: transparent;
}

.console-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.console-body::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>

