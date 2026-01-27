<template>
  <div class="job-info-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">任务管理</h2>
      <div class="header-actions">
        <n-input v-model:value="searchText" placeholder="搜索任务..." clearable style="width: 240px">
          <template #prefix>
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
            </svg>
          </template>
        </n-input>
        <n-button type="primary" @click="showCreateModal = true">
          <template #icon>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
          </template>
          新建任务
        </n-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon blue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ jobList.length }}</span>
          <span class="stat-label">全部任务</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ jobList.filter(j => j.status === 1).length }}</span>
          <span class="stat-label">运行中</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ jobList.filter(j => j.status === 0).length }}</span>
          <span class="stat-label">已停用</span>
        </div>
      </div>
    </div>

    <!-- 任务表格 -->
    <div class="table-card">
      <n-data-table
        :columns="columns"
        :data="filteredJobs"
        :pagination="pagination"
        :row-key="row => row.id"
        striped
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" :title="editingJob ? '编辑任务' : '新建任务'" style="width: 600px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100px">
        <n-form-item label="任务Handler" path="jobHandler">
          <n-input v-model:value="formData.jobHandler" placeholder="如: dataSyncHandler" />
        </n-form-item>
        <n-form-item label="执行器" path="executorId">
          <n-select v-model:value="formData.executorId" :options="executorOptions" placeholder="选择执行器" />
        </n-form-item>
        <n-form-item label="调度类型" path="scheduleType">
          <n-select v-model:value="formData.scheduleType" :options="scheduleTypeOptions" />
        </n-form-item>
        <n-form-item label="CRON表达式" path="scheduleRate">
          <n-input v-model:value="formData.scheduleRate" placeholder="0 0 * * * ?" />
        </n-form-item>
        <n-form-item label="分发类型" path="dispatchType">
          <n-select v-model:value="formData.dispatchType" :options="dispatchTypeOptions" />
        </n-form-item>
        <n-form-item label="负载均衡" path="loadBalanceType">
          <n-select v-model:value="formData.loadBalanceType" :options="loadBalanceOptions" />
        </n-form-item>
        <n-form-item label="超时时间(秒)" path="timeoutSeconds">
          <n-input-number v-model:value="formData.timeoutSeconds" :min="1" :max="3600" />
        </n-form-item>
        <n-form-item label="最大重试" path="maxRetryTimes">
          <n-input-number v-model:value="formData.maxRetryTimes" :min="0" :max="10" />
        </n-form-item>
        <n-form-item label="任务参数" path="jobParams">
          <n-input v-model:value="formData.jobParams" type="textarea" placeholder='{"key": "value"}' :rows="3" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" :rows="2" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit">确定</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, h } from 'vue'
import { NButton, NTag, NSwitch, NSpace, useMessage } from 'naive-ui'

const message = useMessage()

// 搜索
const searchText = ref('')

// 弹窗控制
const showCreateModal = ref(false)
const editingJob = ref(null)

// 表单数据
const formData = ref({
  jobHandler: '',
  executorId: null,
  scheduleType: 'CRON',
  scheduleRate: '',
  dispatchType: 'ROUND',
  loadBalanceType: 'ROUND',
  serializerType: 'KRYO',
  timeoutSeconds: 60,
  maxRetryTimes: 3,
  jobParams: '',
  description: ''
})

const formRules = {
  jobHandler: { required: true, message: '请输入Handler名称' },
  executorId: { required: true, message: '请选择执行器', type: 'number' },
  scheduleRate: { required: true, message: '请输入CRON表达式' }
}

// 选项配置
const executorOptions = [
  { label: 'executor-default', value: 1 },
  { label: 'executor-report', value: 2 }
]

const scheduleTypeOptions = [
  { label: 'CRON表达式', value: 'CRON' },
  { label: '固定频率', value: 'FIXED_RATE' },
  { label: '固定延迟', value: 'FIXED_DELAY' },
  { label: '手动触发', value: 'API' }
]

const dispatchTypeOptions = [
  { label: '单播(轮询)', value: 'ROUND' },
  { label: '广播', value: 'BROADCAST' }
]

const loadBalanceOptions = [
  { label: '轮询', value: 'ROUND' },
  { label: '随机', value: 'RANDOM' },
  { label: '一致性哈希', value: 'CONSISTENT_HASH' },
  { label: '最少活跃', value: 'LEAST_ACTIVE' }
]

// 任务列表
const jobList = ref([
  {
    id: 1,
    jobHandler: 'dataSyncHandler',
    executorId: 1,
    scheduleType: 'CRON',
    scheduleRate: '0 0 2 * * ?',
    dispatchType: 'ROUND',
    loadBalanceType: 'ROUND',
    status: 1,
    lastExecuteTime: '2026-01-26 02:00:00',
    nextExecuteTime: '2026-01-27 02:00:00',
    description: '数据同步任务'
  },
  {
    id: 2,
    jobHandler: 'reportGenHandler',
    executorId: 1,
    scheduleType: 'CRON',
    scheduleRate: '0 0 8 * * ?',
    dispatchType: 'ROUND',
    loadBalanceType: 'ROUND',
    status: 1,
    lastExecuteTime: '2026-01-26 08:00:00',
    nextExecuteTime: '2026-01-27 08:00:00',
    description: '报表生成任务'
  },
  {
    id: 3,
    jobHandler: 'emailSendHandler',
    executorId: 2,
    scheduleType: 'CRON',
    scheduleRate: '0 30 9 * * ?',
    dispatchType: 'BROADCAST',
    loadBalanceType: 'RANDOM',
    status: 0,
    lastExecuteTime: '2026-01-25 09:30:00',
    nextExecuteTime: null,
    description: '邮件推送任务'
  },
  {
    id: 4,
    jobHandler: 'cacheRefreshHandler',
    executorId: 1,
    scheduleType: 'FIXED_RATE',
    scheduleRate: '300000',
    dispatchType: 'ROUND',
    loadBalanceType: 'LEAST_ACTIVE',
    status: 1,
    lastExecuteTime: '2026-01-26 11:25:00',
    nextExecuteTime: '2026-01-26 11:30:00',
    description: '缓存刷新任务'
  }
])

// 过滤后的列表
const filteredJobs = computed(() => {
  if (!searchText.value) return jobList.value
  const keyword = searchText.value.toLowerCase()
  return jobList.value.filter(job => 
    job.jobHandler.toLowerCase().includes(keyword) ||
    job.description?.toLowerCase().includes(keyword)
  )
})

// 分页
const pagination = { pageSize: 10 }

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { 
    title: 'Handler', 
    key: 'jobHandler',
    render: (row) => h('code', { class: 'handler-code' }, row.jobHandler)
  },
  { 
    title: 'CRON', 
    key: 'scheduleRate',
    render: (row) => h(NTag, { size: 'small', type: 'info' }, () => row.scheduleRate)
  },
  { 
    title: '调度类型', 
    key: 'scheduleType',
    render: (row) => h(NTag, { size: 'small' }, () => row.scheduleType)
  },
  { 
    title: '分发方式', 
    key: 'dispatchType',
    render: (row) => h(NTag, { size: 'small', type: row.dispatchType === 'BROADCAST' ? 'warning' : 'default' }, () => row.dispatchType)
  },
  { 
    title: '状态', 
    key: 'status',
    width: 100,
    render: (row) => h(NSwitch, { 
      value: row.status === 1,
      onUpdateValue: (val) => handleStatusChange(row, val)
    })
  },
  { title: '上次执行', key: 'lastExecuteTime', width: 160 },
  { title: '下次执行', key: 'nextExecuteTime', width: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) => h(NSpace, {}, () => [
      h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => handleTrigger(row) }, () => '执行'),
      h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) }, () => '编辑'),
      h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => '删除')
    ])
  }
]

// 事件处理
const handleStatusChange = (row, value) => {
  row.status = value ? 1 : 0
  message.success(value ? '任务已启用' : '任务已停用')
}

const handleTrigger = (row) => {
  message.loading(`正在触发任务: ${row.jobHandler}`)
  setTimeout(() => {
    message.success('任务触发成功')
  }, 1000)
}

const handleEdit = (row) => {
  editingJob.value = row
  Object.assign(formData.value, row)
  showCreateModal.value = true
}

const handleDelete = (row) => {
  const index = jobList.value.findIndex(j => j.id === row.id)
  if (index > -1) {
    jobList.value.splice(index, 1)
    message.success('删除成功')
  }
}

const handleSubmit = () => {
  if (editingJob.value) {
    Object.assign(editingJob.value, formData.value)
    message.success('更新成功')
  } else {
    const newJob = {
      ...formData.value,
      id: Date.now(),
      status: 1,
      lastExecuteTime: null,
      nextExecuteTime: null
    }
    jobList.value.unshift(newJob)
    message.success('创建成功')
  }
  showCreateModal.value = false
  editingJob.value = null
}
</script>

<style scoped>
.job-info-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
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
  align-items: center;
}

.search-icon {
  width: 16px;
  height: 16px;
  color: var(--text-muted);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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

.stat-icon.blue {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.stat-icon.green {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.stat-icon.orange {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

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

.handler-code {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  background: var(--bg-hover);
  padding: 2px 8px;
  border-radius: 4px;
  color: var(--primary-color);
}
</style>


