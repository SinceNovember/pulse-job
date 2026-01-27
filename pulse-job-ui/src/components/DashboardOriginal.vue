<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon nodes">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <circle cx="4" cy="8" r="2"/>
            <circle cx="20" cy="8" r="2"/>
            <circle cx="4" cy="16" r="2"/>
            <circle cx="20" cy="16" r="2"/>
            <path d="M6 8h4M14 8h4M6 16h4M14 16h4"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.activeNodes }}</span>
          <span class="stat-label">活跃节点</span>
        </div>
        <div class="stat-trend up">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M7 17l5-5 5 5"/>
          </svg>
          <span>+3</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon tasks">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 11l3 3L22 4"/>
            <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.completedTasks }}</span>
          <span class="stat-label">已完成任务</span>
        </div>
        <div class="stat-trend up">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M7 17l5-5 5 5"/>
          </svg>
          <span>+127</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon success">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.successRate }}%</span>
          <span class="stat-label">成功率</span>
        </div>
        <div class="stat-trend up">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M7 17l5-5 5 5"/>
          </svg>
          <span>+2.5%</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon queue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2"/>
            <path d="M3 10h18"/>
            <path d="M8 2v4"/>
            <path d="M16 2v4"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.queuedJobs }}</span>
          <span class="stat-label">队列任务</span>
        </div>
        <div class="stat-trend down">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M7 7l5 5 5-5"/>
          </svg>
          <span>-15</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="dashboard-main">
      <!-- 左侧：图表和表格 -->
      <div class="dashboard-left">
        <!-- 任务执行趋势图 -->
        <div class="chart-card">
          <div class="card-header">
            <h3>任务执行趋势</h3>
            <div class="card-actions">
              <button 
                v-for="period in ['日', '周', '月']" 
                :key="period"
                :class="['period-btn', { active: activePeriod === period }]"
                @click="activePeriod = period"
              >
                {{ period }}
              </button>
            </div>
          </div>
          <div class="chart-container">
            <div class="chart-placeholder">
              <div class="chart-bars">
                <div v-for="(bar, index) in chartData" :key="index" class="chart-bar-wrapper">
                  <div class="chart-bar" :style="{ height: bar.value + '%' }">
                    <span class="bar-tooltip">{{ bar.count }}</span>
                  </div>
                  <span class="bar-label">{{ bar.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 最近任务表格 -->
        <div class="table-card">
          <div class="card-header">
            <h3>最近任务</h3>
            <a href="#" class="view-all">查看全部</a>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>执行节点</th>
                  <th>状态</th>
                  <th>耗时</th>
                  <th>完成时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in recentTasks" :key="task.id">
                  <td>
                    <div class="task-info">
                      <span class="task-name">{{ task.name }}</span>
                      <span class="task-id">{{ task.id }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="node-info">
                      <span class="node-name">{{ task.node }}</span>
                      <span class="node-ip">{{ task.nodeIp }}</span>
                    </div>
                  </td>
                  <td>
                    <span :class="['status-badge', task.status]">
                      {{ statusText[task.status] }}
                    </span>
                  </td>
                  <td class="duration">{{ task.duration }}</td>
                  <td class="time">{{ task.completedAt }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右侧：快捷操作和节点状态 -->
      <div class="dashboard-right">
        <!-- 快捷操作 -->
        <div class="quick-actions-card">
          <h3>快捷操作</h3>
          <div class="quick-actions-grid">
            <button class="quick-action">
              <div class="action-icon new-task">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 5v14M5 12h14"/>
                </svg>
              </div>
              <span>新建任务</span>
            </button>
            <button class="quick-action">
              <div class="action-icon new-node">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="2" width="20" height="8" rx="2"/>
                  <rect x="2" y="14" width="20" height="8" rx="2"/>
                  <circle cx="6" cy="6" r="1" fill="currentColor"/>
                  <circle cx="6" cy="18" r="1" fill="currentColor"/>
                </svg>
              </div>
              <span>添加节点</span>
            </button>
            <button class="quick-action">
              <div class="action-icon schedule">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 6v6l4 2"/>
                </svg>
              </div>
              <span>定时调度</span>
            </button>
            <button class="quick-action">
              <div class="action-icon logs">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                  <path d="M14 2v6h6"/>
                  <path d="M16 13H8"/>
                  <path d="M16 17H8"/>
                  <path d="M10 9H8"/>
                </svg>
              </div>
              <span>查看日志</span>
            </button>
          </div>
        </div>

        <!-- 节点状态 -->
        <div class="nodes-card">
          <div class="card-header">
            <h3>节点状态</h3>
            <a href="#" class="view-all">查看全部</a>
          </div>
          <div class="nodes-list">
            <div v-for="node in nodes" :key="node.id" class="node-item">
              <div class="node-avatar" :class="node.status">
                {{ node.name.charAt(0) }}
              </div>
              <div class="node-details">
                <span class="node-name">{{ node.name }}</span>
                <span class="node-meta">{{ node.ip }} · CPU {{ node.cpu }}%</span>
              </div>
              <div class="node-status-indicator" :class="node.status"></div>
            </div>
          </div>
        </div>

        <!-- 系统告警 -->
        <div class="alerts-card">
          <div class="card-header">
            <h3>系统告警</h3>
            <span class="alert-count">{{ alerts.length }}</span>
          </div>
          <div class="alerts-list">
            <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.level">
              <div class="alert-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              </div>
              <div class="alert-content">
                <span class="alert-message">{{ alert.message }}</span>
                <span class="alert-time">{{ alert.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const stats = reactive({
  activeNodes: 12,
  completedTasks: 2847,
  successRate: 98.5,
  queuedJobs: 43
})

const activePeriod = ref('周')

const chartData = ref([
  { label: '周一', value: 65, count: 234 },
  { label: '周二', value: 80, count: 312 },
  { label: '周三', value: 45, count: 178 },
  { label: '周四', value: 90, count: 356 },
  { label: '周五', value: 75, count: 289 },
  { label: '周六', value: 30, count: 112 },
  { label: '周日', value: 20, count: 78 }
])

const statusText = {
  success: '成功',
  running: '运行中',
  failed: '失败',
  pending: '等待中'
}

const recentTasks = ref([
  { id: 'JOB-2847', name: '数据同步任务', node: 'Worker-01', nodeIp: '192.168.1.101', status: 'success', duration: '2m 34s', completedAt: '2分钟前' },
  { id: 'JOB-2846', name: '日志清理任务', node: 'Worker-03', nodeIp: '192.168.1.103', status: 'success', duration: '45s', completedAt: '5分钟前' },
  { id: 'JOB-2845', name: '报表生成任务', node: 'Worker-02', nodeIp: '192.168.1.102', status: 'running', duration: '1m 12s', completedAt: '-' },
  { id: 'JOB-2844', name: '缓存刷新任务', node: 'Worker-01', nodeIp: '192.168.1.101', status: 'failed', duration: '3m 01s', completedAt: '12分钟前' },
  { id: 'JOB-2843', name: '邮件发送任务', node: 'Worker-04', nodeIp: '192.168.1.104', status: 'success', duration: '1m 56s', completedAt: '18分钟前' },
  { id: 'JOB-2842', name: '数据备份任务', node: 'Worker-02', nodeIp: '192.168.1.102', status: 'success', duration: '5m 23s', completedAt: '25分钟前' }
])

const nodes = ref([
  { id: 1, name: 'Worker-01', ip: '192.168.1.101', cpu: 45, status: 'online' },
  { id: 2, name: 'Worker-02', ip: '192.168.1.102', cpu: 78, status: 'online' },
  { id: 3, name: 'Worker-03', ip: '192.168.1.103', cpu: 23, status: 'online' },
  { id: 4, name: 'Worker-04', ip: '192.168.1.104', cpu: 0, status: 'offline' },
  { id: 5, name: 'Worker-05', ip: '192.168.1.105', cpu: 56, status: 'online' }
])

const alerts = ref([
  { id: 1, level: 'warning', message: 'Worker-04 节点离线超过 5 分钟', time: '3分钟前' },
  { id: 2, level: 'error', message: '任务 JOB-2844 执行失败', time: '12分钟前' },
  { id: 3, level: 'warning', message: 'Worker-02 CPU 使用率超过 75%', time: '15分钟前' }
])
</script>

<style scoped>
.dashboard {
  padding: 0;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-icon.nodes {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.stat-icon.tasks {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.stat-icon.success {
  background: rgba(155, 89, 182, 0.1);
  color: #9b59b6;
}

.stat-icon.queue {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}

.stat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.875rem;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
}

.stat-trend svg {
  width: 16px;
  height: 16px;
}

.stat-trend.up {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.stat-trend.down {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

/* 主内容区布局 */
.dashboard-main {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
}

.dashboard-left {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-right {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 卡片通用样式 */
.chart-card,
.table-card,
.quick-actions-card,
.nodes-card,
.alerts-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.view-all {
  font-size: 0.875rem;
  color: var(--primary-color);
  text-decoration: none;
}

.view-all:hover {
  text-decoration: underline;
}

/* 周期切换按钮 */
.card-actions {
  display: flex;
  gap: 4px;
  background: var(--bg-main);
  padding: 4px;
  border-radius: 8px;
}

.period-btn {
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.period-btn:hover {
  color: var(--text-primary);
}

.period-btn.active {
  background: var(--bg-card);
  color: var(--primary-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* 图表占位 */
.chart-container {
  height: 240px;
}

.chart-placeholder {
  height: 100%;
  display: flex;
  align-items: flex-end;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  width: 100%;
  height: 100%;
  padding-bottom: 30px;
}

.chart-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.chart-bar {
  width: 100%;
  max-width: 48px;
  background: linear-gradient(180deg, #5E81F4 0%, #7B9CF5 100%);
  border-radius: 6px 6px 0 0;
  position: relative;
  transition: height 0.3s ease;
  cursor: pointer;
}

.chart-bar:hover {
  background: linear-gradient(180deg, #4A6CD4 0%, #5E81F4 100%);
}

.bar-tooltip {
  position: absolute;
  top: -28px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--text-primary);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.15s ease;
  white-space: nowrap;
}

.chart-bar:hover .bar-tooltip {
  opacity: 1;
}

.bar-label {
  margin-top: 12px;
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* 表格样式 */
.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.data-table th {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.data-table td {
  font-size: 0.875rem;
  color: var(--text-primary);
}

.data-table tbody tr:hover {
  background: var(--bg-hover);
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.task-info,
.node-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.task-name,
.node-name {
  font-weight: 500;
  color: var(--text-primary);
}

.task-id,
.node-ip {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.status-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-badge.success {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.status-badge.running {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.status-badge.failed {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.status-badge.pending {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}

.duration,
.time {
  color: var(--text-secondary);
}

/* 快捷操作 */
.quick-actions-card h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background: var(--bg-main);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.quick-action:hover {
  background: var(--bg-hover);
  border-color: var(--primary-color);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon svg {
  width: 20px;
  height: 20px;
}

.action-icon.new-task {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.action-icon.new-node {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.action-icon.schedule {
  background: rgba(155, 89, 182, 0.1);
  color: #9b59b6;
}

.action-icon.logs {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}

.quick-action span {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.quick-action:hover span {
  color: var(--text-primary);
}

/* 节点列表 */
.nodes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-main);
  border-radius: 10px;
  transition: background 0.15s ease;
}

.node-item:hover {
  background: var(--bg-hover);
}

.node-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  color: white;
}

.node-avatar.online {
  background: linear-gradient(135deg, #2ecc71 0%, #27ae60 100%);
}

.node-avatar.offline {
  background: linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%);
}

.node-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-details .node-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
}

.node-meta {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.node-status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.node-status-indicator.online {
  background: #2ecc71;
  box-shadow: 0 0 8px rgba(46, 204, 113, 0.4);
}

.node-status-indicator.offline {
  background: #95a5a6;
}

/* 告警列表 */
.alert-count {
  background: #e74c3c;
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.alerts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  background: var(--bg-main);
}

.alert-item.warning {
  border-left: 3px solid #f1c40f;
}

.alert-item.error {
  border-left: 3px solid #e74c3c;
}

.alert-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-item.warning .alert-icon {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}

.alert-item.error .alert-icon {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.alert-icon svg {
  width: 16px;
  height: 16px;
}

.alert-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alert-message {
  font-size: 0.8125rem;
  color: var(--text-primary);
  line-height: 1.4;
}

.alert-time {
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dashboard-main {
    grid-template-columns: 1fr;
  }
  
  .dashboard-right {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
  }
  
  .quick-actions-card {
    grid-column: span 2;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .dashboard-right {
    grid-template-columns: 1fr;
  }
  
  .quick-actions-card {
    grid-column: span 1;
  }
}
</style>
