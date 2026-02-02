<template>
  <div class="log-management">
    <!-- 统计卡片 -->
    <div class="stats-card">
      <div class="stats-row">
        <div class="stat-item">
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
        <div class="stat-item">
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
        <div class="stat-item">
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
        <div class="stat-item">
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
    </div>

    <!-- 日志控制台 -->
    <div class="log-console" :class="{ 'light-theme': isLightTheme }">
      <!-- 控制台头部 -->
      <div class="console-header">
        <div class="header-left">
          <div class="window-dots">
            <div class="dot red"></div>
            <div class="dot yellow"></div>
            <div class="dot green"></div>
          </div>
          <div class="console-title">
            <span class="title-text">Job Instance #{{ currentInstanceId }}</span>
            <span class="title-separator">·</span>
            <span class="title-job">{{ currentJobName }}</span>
          </div>
        </div>
        <div class="header-actions">
          <button class="header-btn icon-btn" @click="isLightTheme = !isLightTheme" :title="isLightTheme ? '切换暗色' : '切换亮色'">
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
          <div class="header-divider"></div>
          <button class="header-btn" @click="handleExport">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            <span>下载</span>
          </button>
          <button class="header-btn" @click="handleRefresh">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6"/><path d="M1 20v-6h6"/>
              <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
            </svg>
            <span>刷新</span>
          </button>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="console-toolbar">
        <div class="filter-group">
          <button 
            class="filter-tab" 
            :class="{ active: filterLevel === null }"
            @click="filterLevel = null"
          >
            <span class="tab-label">ALL</span>
            <span class="tab-count">{{ logList.length }}</span>
          </button>
          <button 
            class="filter-tab info" 
            :class="{ active: filterLevel === 'INFO' }"
            @click="filterLevel = 'INFO'"
          >
            <span class="tab-label">INFO</span>
            <span class="tab-count">{{ logList.filter(l => l.logLevel === 'INFO').length }}</span>
          </button>
          <button 
            class="filter-tab warn" 
            :class="{ active: filterLevel === 'WARN' }"
            @click="filterLevel = 'WARN'"
          >
            <span class="tab-label">WARN</span>
            <span class="tab-count">{{ logList.filter(l => l.logLevel === 'WARN').length }}</span>
          </button>
          <button 
            class="filter-tab error" 
            :class="{ active: filterLevel === 'ERROR' }"
            @click="filterLevel = 'ERROR'"
          >
            <span class="tab-label">ERROR</span>
            <span class="tab-count">{{ logList.filter(l => l.logLevel === 'ERROR').length }}</span>
          </button>
        </div>
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input 
            v-model="searchKeyword" 
            type="text" 
            class="search-input" 
            placeholder="搜索日志内容..."
          />
          <button v-if="searchKeyword" class="search-clear" @click="searchKeyword = ''">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 日志内容 -->
      <div class="console-logs" ref="consoleRef">
        <div 
          v-for="(log, index) in filteredLogs" 
          :key="log.id" 
          class="log-row"
          :class="log.logLevel.toLowerCase()"
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
        <div v-if="filteredLogs.length === 0" class="no-logs">
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
            {{ filteredLogs.length }} 条记录
          </span>
        </div>
        <div class="footer-right">
          <span class="footer-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
            最后更新: {{ lastUpdateTime }}
          </span>
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
            <n-descriptions-item label="线程">{{ detailLog.thread }}</n-descriptions-item>
            <n-descriptions-item label="类名">{{ detailLog.logger }}</n-descriptions-item>
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
import { ref, computed } from 'vue'
import { useMessage } from 'naive-ui'

const message = useMessage()

// 主题切换
const isLightTheme = ref(false)

// 当前实例信息
const currentInstanceId = ref(12345)
const currentJobName = ref('TestJob1')
const lastUpdateTime = ref('13:26:49')

// 搜索关键词
const searchKeyword = ref('')

// 筛选条件
const filterLevel = ref(null)

// 详情抽屉
const showDetail = ref(false)
const detailLog = ref(null)
const consoleRef = ref(null)

// 日志列表
const logList = ref([
  {
    id: 1,
    jobId: 32,
    instanceId: 12345,
    logLevel: 'INFO',
    thread: 'job-worker-1',
    logger: 'c.s.p.c.i.LoggingJobInterceptor',
    content: '[Job-32] 开始执行: handler=TestJob1#testJob1()',
    createTime: '2026-01-09 13:26:48.493'
  },
  {
    id: 2,
    jobId: 32,
    instanceId: 12345,
    logLevel: 'INFO',
    thread: 'job-worker-1',
    logger: 'c.e.demo.job.TestJob1',
    content: '正在处理订单数据，共 1000 条记录',
    createTime: '2026-01-09 13:26:48.512'
  },
  {
    id: 3,
    jobId: 32,
    instanceId: 12345,
    logLevel: 'WARN',
    thread: 'job-worker-1',
    logger: 'c.e.demo.job.TestJob1',
    content: '发现 3 条数据格式异常，已跳过处理',
    createTime: '2026-01-09 13:26:48.623'
  },
  {
    id: 4,
    jobId: 32,
    instanceId: 12345,
    logLevel: 'ERROR',
    thread: 'job-worker-1',
    logger: 'c.e.demo.job.TestJob1',
    content: '数据库连接超时: Connection timed out after 30000ms',
    createTime: '2026-01-09 13:26:49.156'
  },
  {
    id: 5,
    jobId: 32,
    instanceId: 12345,
    logLevel: 'INFO',
    thread: 'job-worker-1',
    logger: 'c.s.p.c.i.LoggingJobInterceptor',
    content: '[Job-32] 执行完成，耗时: 741ms，结果: PARTIAL_SUCCESS',
    createTime: '2026-01-09 13:26:49.234'
  },
  {
    id: 6,
    jobId: 32,
    instanceId: 12345,
    logLevel: 'DEBUG',
    thread: 'job-worker-1',
    logger: 'c.s.p.c.i.LoggingJobInterceptor',
    content: '清理任务上下文资源...',
    createTime: '2026-01-09 13:26:49.240'
  },
  {
    id: 7,
    jobId: 32,
    instanceId: 12345,
    logLevel: 'INFO',
    thread: 'job-worker-2',
    logger: 'c.s.p.c.scheduler.TaskScheduler',
    content: '任务调度完成, 下次执行时间: 2026-01-09 14:00:00',
    createTime: '2026-01-09 13:26:49.256'
  }
])

// 过滤后的列表
const filteredLogs = computed(() => {
  return logList.value.filter(log => {
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      if (!log.content.toLowerCase().includes(keyword) && 
          !log.logger.toLowerCase().includes(keyword)) return false
    }
    if (filterLevel.value && log.logLevel !== filterLevel.value) {
      return false
    }
    return true
  })
})

// 工具方法
const getLevelType = (level) => {
  const map = { DEBUG: 'default', INFO: 'info', WARN: 'warning', ERROR: 'error' }
  return map[level] || 'default'
}

// 事件处理
const handleRefresh = () => {
  message.success('刷新成功')
}

const handleExport = () => {
  const content = filteredLogs.value.map(log => 
    `${log.createTime} ${log.logLevel.padEnd(5)} [${log.thread}] ${log.logger} ${log.content}`
  ).join('\n')
  
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `pulse-job-logs-${currentInstanceId.value}-${new Date().toISOString().slice(0, 10)}.log`
  a.click()
  URL.revokeObjectURL(url)
  
  message.success('导出成功')
}
</script>

<style scoped>
.log-management {
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

/* ==================== 日志控制台 ==================== */
.log-console {
  background: #0c1018;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 
    0 0 0 1px rgba(255, 255, 255, 0.05),
    0 20px 50px -10px rgba(0, 0, 0, 0.5);
  transition: all 0.3s ease;
}

/* 控制台头部 */
.console-header {
  background: linear-gradient(180deg, #161b26 0%, #12161f 100%);
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.window-dots {
  display: flex;
  gap: 8px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  transition: transform 0.15s ease;
}

.dot:hover {
  transform: scale(1.1);
}

.dot.red { background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%); box-shadow: 0 0 8px rgba(255, 90, 90, 0.3); }
.dot.yellow { background: linear-gradient(135deg, #ffd93d 0%, #f0c419 100%); box-shadow: 0 0 8px rgba(255, 217, 61, 0.3); }
.dot.green { background: linear-gradient(135deg, #6bcb77 0%, #4ade80 100%); box-shadow: 0 0 8px rgba(74, 222, 128, 0.3); }

.console-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.title-text {
  color: #8b949e;
  font-weight: 500;
}

.title-separator {
  color: #484f58;
}

.title-job {
  color: #58a6ff;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0 4px;
}

.header-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #8b949e;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.header-btn svg {
  width: 14px;
  height: 14px;
}

.header-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  color: #c9d1d9;
}

.header-btn.icon-btn {
  padding: 7px 10px;
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

.filter-tab.info.active {
  background: #238636;
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

.search-box {
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

.search-box:focus-within {
  border-color: #58a6ff;
  box-shadow: 0 0 0 3px rgba(88, 166, 255, 0.15);
}

.search-icon {
  width: 16px;
  height: 16px;
  color: #484f58;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #c9d1d9;
  font-size: 13px;
  padding: 10px 0;
}

.search-input::placeholder {
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
  min-height: 450px;
  max-height: calc(100vh - 420px);
  overflow-y: auto;
  font-family: 'JetBrains Mono', 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.log-row {
  padding: 6px 20px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border-left: 3px solid transparent;
  transition: all 0.15s ease;
}

.log-row:hover {
  background: rgba(255, 255, 255, 0.02);
}

.log-row.info { border-left-color: transparent; }
.log-row.warn { border-left-color: #d29922; background: rgba(210, 153, 34, 0.05); }
.log-row.error { border-left-color: #f85149; background: rgba(248, 81, 73, 0.08); }
.log-row.debug { border-left-color: transparent; opacity: 0.7; }

.log-line {
  color: #484f58;
  min-width: 28px;
  text-align: right;
  font-size: 11px;
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
}

.log-level.info { color: #3fb950; }
.log-level.info .level-dot { background: #3fb950; box-shadow: 0 0 6px rgba(63, 185, 80, 0.5); }
.log-level.warn { color: #d29922; }
.log-level.warn .level-dot { background: #d29922; box-shadow: 0 0 6px rgba(210, 153, 34, 0.5); }
.log-level.error { color: #f85149; }
.log-level.error .level-dot { background: #f85149; box-shadow: 0 0 6px rgba(248, 81, 73, 0.5); }
.log-level.debug { color: #8b949e; }
.log-level.debug .level-dot { background: #8b949e; }

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
  color: #ffa198;
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

/* ==================== 白色主题 ==================== */
.log-console.light-theme {
  background: #ffffff;
  box-shadow: 
    0 0 0 1px rgba(0, 0, 0, 0.05),
    0 10px 40px -10px rgba(0, 0, 0, 0.1);
}

.log-console.light-theme .console-header {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}

.log-console.light-theme .title-text { color: #64748b; }
.log-console.light-theme .title-separator { color: #cbd5e1; }
.log-console.light-theme .title-job { color: #2563eb; }

.log-console.light-theme .header-btn {
  background: white;
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.log-console.light-theme .header-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #334155;
}

.log-console.light-theme .header-divider {
  background: #e2e8f0;
}

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

.log-console.light-theme .search-box {
  background: white;
  border: 1px solid #e2e8f0;
}

.log-console.light-theme .search-box:focus-within {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.log-console.light-theme .search-icon { color: #94a3b8; }
.log-console.light-theme .search-input { color: #1e293b; }
.log-console.light-theme .search-input::placeholder { color: #94a3b8; }

.log-console.light-theme .console-logs { background: #ffffff; }

.log-console.light-theme .log-row:hover { background: #f8fafc; }
.log-console.light-theme .log-row.warn { background: rgba(245, 158, 11, 0.06); }
.log-console.light-theme .log-row.error { background: rgba(239, 68, 68, 0.06); }

.log-console.light-theme .log-line { color: #cbd5e1; }
.log-console.light-theme .log-time { color: #94a3b8; }
.log-console.light-theme .log-level.info { color: #059669; }
.log-console.light-theme .log-level.info .level-dot { background: #059669; }
.log-console.light-theme .log-level.warn { color: #d97706; }
.log-console.light-theme .log-level.warn .level-dot { background: #d97706; }
.log-console.light-theme .log-level.error { color: #dc2626; }
.log-console.light-theme .log-level.error .level-dot { background: #dc2626; }
.log-console.light-theme .log-level.debug { color: #94a3b8; }
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

/* 白色主题滚动条 */
.log-console.light-theme .console-logs::-webkit-scrollbar-thumb {
  background: #cbd5e1;
}

.log-console.light-theme .console-logs::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ==================== 详情部分 ==================== */
.content-section {
  margin-top: 20px;
}

.content-section h4 {
  margin: 0 0 12px 0;
  font-size: 0.875rem;
  color: var(--text-primary);
}

/* ==================== 滚动条样式 ==================== */
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
  
  .log-time {
    min-width: 160px;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .console-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-box {
    max-width: none;
  }
  
  .log-row {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .log-time {
    min-width: auto;
  }
  
  .log-thread {
    min-width: auto;
  }
  
  .log-msg {
    width: 100%;
  }
}
</style>
