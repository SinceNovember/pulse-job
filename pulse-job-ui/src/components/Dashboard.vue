<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <StatsCard
        :value="stats.activeNodes"
        label="活跃节点"
        icon-type="nodes"
        :trend="{ type: 'up', value: '+3' }"
      >
        <template #icon>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <circle cx="4" cy="8" r="2"/>
            <circle cx="20" cy="8" r="2"/>
            <circle cx="4" cy="16" r="2"/>
            <circle cx="20" cy="16" r="2"/>
            <path d="M6 8h4M14 8h4M6 16h4M14 16h4"/>
          </svg>
        </template>
      </StatsCard>

      <StatsCard
        :value="stats.completedTasks"
        label="已完成任务"
        icon-type="tasks"
        :trend="{ type: 'up', value: '+127' }"
      >
        <template #icon>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 11l3 3L22 4"/>
            <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
          </svg>
        </template>
      </StatsCard>

      <StatsCard
        :value="stats.successRate"
        label="成功率"
        suffix="%"
        icon-type="success"
        :trend="{ type: 'up', value: '+2.5%' }"
      >
        <template #icon>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </template>
      </StatsCard>

      <StatsCard
        :value="stats.queuedJobs"
        label="队列任务"
        icon-type="queue"
        :trend="{ type: 'down', value: '-15' }"
      >
        <template #icon>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2"/>
            <path d="M3 10h18"/>
            <path d="M8 2v4"/>
            <path d="M16 2v4"/>
          </svg>
        </template>
      </StatsCard>
    </div>

    <!-- 主内容区 -->
    <div class="dashboard-main">
      <!-- 左侧：图表和表格 -->
      <div class="dashboard-left">
        <!-- 任务执行趋势图 -->
        <ChartCard
          title="任务执行趋势"
          :periods="['日', '周', '月']"
          :active-period="activePeriod"
          :data="chartData"
          @period-change="activePeriod = $event"
        />

        <!-- 最近任务表格 -->
        <DataCard title="最近任务" show-view-all @view-all="handleViewAllTasks">
          <RecentTasksTable :tasks="recentTasks" />
        </DataCard>
      </div>

      <!-- 右侧：快捷操作和节点状态 -->
      <div class="dashboard-right">
        <!-- 快捷操作 -->
        <QuickActions :actions="quickActions" @action="handleQuickAction" />

        <!-- 节点状态 -->
        <DataCard title="节点状态" show-view-all @view-all="handleViewAllNodes">
          <NodeStatusList :nodes="nodes" />
        </DataCard>

        <!-- 系统告警 -->
        <DataCard title="系统告警">
          <template #extra>
            <n-badge :value="alerts.length" type="error" />
          </template>
          <AlertList :alerts="alerts" />
        </DataCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, h } from 'vue'
import { NBadge } from 'naive-ui'
import { 
  StatsCard, 
  DataCard, 
  ChartCard, 
  RecentTasksTable, 
  QuickActions, 
  NodeStatusList, 
  AlertList 
} from './dashboard'

// 统计数据
const stats = reactive({
  activeNodes: 12,
  completedTasks: 2847,
  successRate: 98.5,
  queuedJobs: 43
})

// 图表周期
const activePeriod = ref('周')

// 图表数据
const chartData = ref([
  { label: '周一', value: 65, count: 234 },
  { label: '周二', value: 80, count: 312 },
  { label: '周三', value: 45, count: 178 },
  { label: '周四', value: 90, count: 356 },
  { label: '周五', value: 75, count: 289 },
  { label: '周六', value: 30, count: 112 },
  { label: '周日', value: 20, count: 78 }
])

// 最近任务
const recentTasks = ref([
  { id: 'JOB-2847', name: '数据同步任务', node: 'Worker-01', nodeIp: '192.168.1.101', status: 'success', duration: '2m 34s', completedAt: '2分钟前' },
  { id: 'JOB-2846', name: '日志清理任务', node: 'Worker-03', nodeIp: '192.168.1.103', status: 'success', duration: '45s', completedAt: '5分钟前' },
  { id: 'JOB-2845', name: '报表生成任务', node: 'Worker-02', nodeIp: '192.168.1.102', status: 'running', duration: '1m 12s', completedAt: '-' },
  { id: 'JOB-2844', name: '缓存刷新任务', node: 'Worker-01', nodeIp: '192.168.1.101', status: 'failed', duration: '3m 01s', completedAt: '12分钟前' },
  { id: 'JOB-2843', name: '邮件发送任务', node: 'Worker-04', nodeIp: '192.168.1.104', status: 'success', duration: '1m 56s', completedAt: '18分钟前' },
  { id: 'JOB-2842', name: '数据备份任务', node: 'Worker-02', nodeIp: '192.168.1.102', status: 'success', duration: '5m 23s', completedAt: '25分钟前' }
])

// 快捷操作配置
const quickActions = ref([
  { 
    key: 'new-task', 
    label: '新建任务', 
    iconType: 'new-task',
    icon: h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M12 5v14M5 12h14' })
    ])
  },
  { 
    key: 'new-node', 
    label: '添加节点', 
    iconType: 'new-node',
    icon: h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('rect', { x: '2', y: '2', width: '20', height: '8', rx: '2' }),
      h('rect', { x: '2', y: '14', width: '20', height: '8', rx: '2' }),
      h('circle', { cx: '6', cy: '6', r: '1', fill: 'currentColor' }),
      h('circle', { cx: '6', cy: '18', r: '1', fill: 'currentColor' })
    ])
  },
  { 
    key: 'schedule', 
    label: '定时调度', 
    iconType: 'schedule',
    icon: h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('path', { d: 'M12 6v6l4 2' })
    ])
  },
  { 
    key: 'logs', 
    label: '查看日志', 
    iconType: 'logs',
    icon: h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
      h('path', { d: 'M14 2v6h6' }),
      h('path', { d: 'M16 13H8' }),
      h('path', { d: 'M16 17H8' }),
      h('path', { d: 'M10 9H8' })
    ])
  }
])

// 节点列表
const nodes = ref([
  { id: 1, name: 'Worker-01', ip: '192.168.1.101', cpu: 45, status: 'online' },
  { id: 2, name: 'Worker-02', ip: '192.168.1.102', cpu: 78, status: 'online' },
  { id: 3, name: 'Worker-03', ip: '192.168.1.103', cpu: 23, status: 'online' },
  { id: 4, name: 'Worker-04', ip: '192.168.1.104', cpu: 0, status: 'offline' },
  { id: 5, name: 'Worker-05', ip: '192.168.1.105', cpu: 56, status: 'online' }
])

// 告警列表
const alerts = ref([
  { id: 1, level: 'warning', message: 'Worker-04 节点离线超过 5 分钟', time: '3分钟前' },
  { id: 2, level: 'error', message: '任务 JOB-2844 执行失败', time: '12分钟前' },
  { id: 3, level: 'warning', message: 'Worker-02 CPU 使用率超过 75%', time: '15分钟前' }
])

// 事件处理
const handleViewAllTasks = () => {
  console.log('查看全部任务')
}

const handleViewAllNodes = () => {
  console.log('查看全部节点')
}

const handleQuickAction = (actionKey) => {
  console.log('快捷操作:', actionKey)
}
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
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .dashboard-right {
    grid-template-columns: 1fr;
  }
}
</style>
