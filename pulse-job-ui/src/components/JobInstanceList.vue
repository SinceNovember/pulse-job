<template>
  <div class="instance-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">执行记录</h2>
      <div class="header-actions">
        <n-select 
          v-model:value="filterJobId" 
          :options="jobOptions" 
          placeholder="全部任务" 
          clearable 
          style="width: 180px"
        />
        <n-select 
          v-model:value="filterStatus" 
          :options="statusOptions" 
          placeholder="全部状态" 
          clearable 
          style="width: 140px"
        />
        <n-date-picker 
          v-model:value="filterDateRange" 
          type="daterange" 
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
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
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
      <div class="stat-card">
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
      <div class="stat-card">
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
      <div class="stat-card">
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

    <!-- 执行记录表格 -->
    <div class="table-card">
      <n-data-table
        :columns="columns"
        :data="filteredInstances"
        :pagination="pagination"
        :row-key="row => row.id"
        striped
      />
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
  </div>
</template>

<script setup>
import { ref, computed, h } from 'vue'
import { NButton, NTag, NSpace, useMessage } from 'naive-ui'

const message = useMessage()

// 筛选条件
const filterJobId = ref(null)
const filterStatus = ref(null)
const filterDateRange = ref(null)

// 详情抽屉
const showDetail = ref(false)
const detailInstance = ref(null)

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

// 过滤后的列表
const filteredInstances = computed(() => {
  let list = instanceList.value
  if (filterJobId.value) {
    list = list.filter(i => i.jobId === filterJobId.value)
  }
  if (filterStatus.value !== null) {
    list = list.filter(i => i.status === filterStatus.value)
  }
  return list
})

// 分页
const pagination = { pageSize: 10 }

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
    render: (row) => h(NTag, { type: getStatusType(row.status) }, () => getStatusLabel(row.status))
  },
  { title: '执行器地址', key: 'executorAddress', width: 160 },
  { title: '触发时间', key: 'triggerTime', width: 160 },
  { title: '开始时间', key: 'startTime', width: 160 },
  { title: '结束时间', key: 'endTime', width: 160 },
  { title: '重试', key: 'retryCount', width: 60 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row) => h(NSpace, {}, () => [
      h(NButton, { size: 'small', quaternary: true, onClick: () => handleViewDetail(row) }, () => '详情'),
      h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => handleViewLog(row) }, () => '日志')
    ])
  }
]

// 事件处理
const handleRefresh = () => {
  message.success('刷新成功')
}

const handleViewDetail = (row) => {
  detailInstance.value = row
  showDetail.value = true
}

const handleViewLog = (row) => {
  message.info(`查看实例 ${row.id} 的日志`)
}
</script>

<style scoped>
.instance-page {
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

.table-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

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

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>


