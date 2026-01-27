<template>
  <div class="task-management">
    <!-- 筛选区域 -->
    <n-card class="filter-card" :bordered="false">
      <div class="filter-row">
        <div class="filter-item">
          <label>任务ID</label>
          <n-input v-model:value="filters.taskId" placeholder="请输入任务ID" clearable size="small" />
        </div>
        <div class="filter-item">
          <label>任务描述</label>
          <n-input v-model:value="filters.description" placeholder="请输入任务描述" clearable size="small" />
        </div>
        <div class="filter-item">
          <label>调度类型</label>
          <n-select 
            v-model:value="filters.scheduleType" 
            :options="scheduleTypeOptions" 
            placeholder="请选择"
            clearable
            size="small"
          />
        </div>
        <div class="filter-item">
          <label>运行模式</label>
          <n-select 
            v-model:value="filters.runMode" 
            :options="runModeOptions" 
            placeholder="请选择"
            clearable
            size="small"
          />
        </div>
        <div class="filter-item">
          <label>负责人</label>
          <n-input v-model:value="filters.owner" placeholder="请输入负责人" clearable size="small" />
        </div>
        <div class="filter-item">
          <label>状态</label>
          <n-select 
            v-model:value="filters.status" 
            :options="statusOptions" 
            placeholder="请选择"
            clearable
            size="small"
          />
        </div>
        <div class="filter-actions">
          <n-button type="primary" @click="handleSearch">
            <template #icon>
              <n-icon><SearchIcon /></n-icon>
            </template>
            搜索
          </n-button>
          <n-button @click="handleReset">重置</n-button>
        </div>
      </div>
    </n-card>

    <!-- 表格卡片 -->
    <n-card class="table-card" :bordered="false">
      <!-- 操作栏 -->
      <div class="action-bar">
        <n-button type="primary" @click="handleCreate">
          <template #icon>
            <n-icon><AddIcon /></n-icon>
          </template>
          新建任务
        </n-button>
        <n-button @click="handleBatchDelete" :disabled="checkedRowKeys.length === 0">
          批量删除
        </n-button>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="filteredTasks"
        :pagination="pagination"
        :row-key="row => row.id"
        :checked-row-keys="checkedRowKeys"
        @update:checked-row-keys="handleCheck"
        class="task-table"
      />
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h } from 'vue'
import { 
  NCard, NInput, NSelect, NButton, NIcon, NDataTable, NTag, NSpace, NPopconfirm, NTooltip,
  useMessage
} from 'naive-ui'

const message = useMessage()

// 图标组件
const SearchIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '11', cy: '11', r: '8' }),
      h('line', { x1: '21', y1: '21', x2: '16.65', y2: '16.65' })
    ])
  }
}

const AddIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('line', { x1: '12', y1: '5', x2: '12', y2: '19' }),
      h('line', { x1: '5', y1: '12', x2: '19', y2: '12' })
    ])
  }
}

// 筛选条件
const filters = reactive({
  taskId: '',
  description: '',
  scheduleType: null,
  runMode: null,
  owner: '',
  status: null
})

// 下拉选项
const scheduleTypeOptions = [
  { label: 'Cron', value: 'cron' },
  { label: '固定频率', value: 'fixed_rate' },
  { label: '固定延迟', value: 'fixed_delay' },
  { label: '一次性', value: 'once' }
]

const runModeOptions = [
  { label: '单机', value: 'standalone' },
  { label: '广播', value: 'broadcast' },
  { label: '分片', value: 'sharding' },
  { label: 'MapReduce', value: 'map_reduce' }
]

const statusOptions = [
  { label: '运行中', value: 'running' },
  { label: '已暂停', value: 'paused' },
  { label: '已停止', value: 'stopped' },
  { label: '异常', value: 'error' }
]

// 状态配置
const statusConfig = {
  running: { label: '运行中', type: 'success' },
  paused: { label: '已暂停', type: 'warning' },
  stopped: { label: '已停止', type: 'default' },
  error: { label: '异常', type: 'error' }
}

// 选中的行
const checkedRowKeys = ref([])

// 模拟数据
const tasks = ref([
  { id: 'JOB-001', description: '数据同步任务', scheduleType: 'cron', scheduleExpr: '0 0 * * * ?', runMode: 'standalone', owner: '张三', status: 'running' },
  { id: 'JOB-002', description: '日志清理任务', scheduleType: 'fixed_rate', scheduleExpr: '每30分钟', runMode: 'broadcast', owner: '李四', status: 'running' },
  { id: 'JOB-003', description: '报表生成任务', scheduleType: 'cron', scheduleExpr: '0 0 8 * * ?', runMode: 'standalone', owner: '王五', status: 'paused' },
  { id: 'JOB-004', description: '缓存刷新任务', scheduleType: 'fixed_delay', scheduleExpr: '每5分钟', runMode: 'standalone', owner: '张三', status: 'running' },
  { id: 'JOB-005', description: '邮件发送任务', scheduleType: 'cron', scheduleExpr: '0 0 9 * * ?', runMode: 'standalone', owner: '赵六', status: 'error' },
  { id: 'JOB-006', description: '数据备份任务', scheduleType: 'cron', scheduleExpr: '0 0 2 * * ?', runMode: 'standalone', owner: '李四', status: 'running' },
  { id: 'JOB-007', description: '订单统计任务', scheduleType: 'cron', scheduleExpr: '0 0 0 * * ?', runMode: 'sharding', owner: '王五', status: 'running' },
  { id: 'JOB-008', description: '用户画像计算', scheduleType: 'once', scheduleExpr: '手动触发', runMode: 'map_reduce', owner: '张三', status: 'stopped' },
  { id: 'JOB-009', description: '库存同步任务', scheduleType: 'fixed_rate', scheduleExpr: '每10分钟', runMode: 'standalone', owner: '赵六', status: 'running' },
  { id: 'JOB-010', description: '消息推送任务', scheduleType: 'cron', scheduleExpr: '0 */5 * * * ?', runMode: 'broadcast', owner: '李四', status: 'paused' }
])

// 筛选后的数据
const filteredTasks = computed(() => {
  return tasks.value.filter(task => {
    if (filters.taskId && !task.id.toLowerCase().includes(filters.taskId.toLowerCase())) return false
    if (filters.description && !task.description.includes(filters.description)) return false
    if (filters.scheduleType && task.scheduleType !== filters.scheduleType) return false
    if (filters.runMode && task.runMode !== filters.runMode) return false
    if (filters.owner && !task.owner.includes(filters.owner)) return false
    if (filters.status && task.status !== filters.status) return false
    return true
  })
})

// 分页配置
const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
  }
})

// 表格列定义
const columns = [
  {
    type: 'selection'
  },
  {
    title: '任务ID',
    key: 'id',
    width: 120,
    render(row) {
      return h('span', { class: 'task-id-cell' }, row.id)
    }
  },
  {
    title: '任务描述',
    key: 'description',
    width: 200,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '调度类型',
    key: 'scheduleType',
    width: 120,
    render(row) {
      const typeMap = {
        cron: 'Cron',
        fixed_rate: '固定频率',
        fixed_delay: '固定延迟',
        once: '一次性'
      }
      return h('div', { class: 'schedule-cell' }, [
        h('span', { class: 'schedule-type' }, typeMap[row.scheduleType] || row.scheduleType),
        h('span', { class: 'schedule-expr' }, row.scheduleExpr)
      ])
    }
  },
  {
    title: '运行模式',
    key: 'runMode',
    width: 110,
    render(row) {
      const modeMap = {
        standalone: { label: '单机', class: 'mode-standalone' },
        broadcast: { label: '广播', class: 'mode-broadcast' },
        sharding: { label: '分片', class: 'mode-sharding' },
        map_reduce: { label: 'MapReduce', class: 'mode-mapreduce' }
      }
      const mode = modeMap[row.runMode] || { label: row.runMode, class: '' }
      return h('span', { class: ['run-mode-badge', mode.class] }, mode.label)
    }
  },
  {
    title: '负责人',
    key: 'owner',
    width: 100,
    render(row) {
      return h('span', { class: 'owner-cell' }, row.owner)
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      const config = statusConfig[row.status] || { label: row.status, type: 'default' }
      return h(NTag, {
        type: config.type,
        size: 'small',
        round: false
      }, { default: () => config.label })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right',
    render(row) {
      return h('div', { class: 'action-buttons' }, [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h('button', {
            class: 'action-btn action-btn-primary',
            onClick: () => handleExecute(row)
          }, [
            h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
              h('polygon', { points: '5 3 19 12 5 21 5 3' })
            ])
          ]),
          default: () => '执行'
        }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h('button', {
            class: 'action-btn action-btn-info',
            onClick: () => handleEdit(row)
          }, [
            h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
              h('path', { d: 'M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7' }),
              h('path', { d: 'M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z' })
            ])
          ]),
          default: () => '编辑'
        }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h('button', {
            class: ['action-btn', row.status === 'running' ? 'action-btn-warning' : 'action-btn-success'],
            onClick: () => handleToggleStatus(row)
          }, [
            row.status === 'running' 
              ? h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
                  h('rect', { x: '6', y: '4', width: '4', height: '16' }),
                  h('rect', { x: '14', y: '4', width: '4', height: '16' })
                ])
              : h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
                  h('polygon', { points: '5 3 19 12 5 21 5 3' })
                ])
          ]),
          default: () => row.status === 'running' ? '暂停' : '启动'
        }),
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row)
        }, {
          trigger: () => h(NTooltip, { trigger: 'hover' }, {
            trigger: () => h('button', {
              class: 'action-btn action-btn-error'
            }, [
              h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
                h('path', { d: 'M3 6h18' }),
                h('path', { d: 'M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2' }),
                h('line', { x1: '10', y1: '11', x2: '10', y2: '17' }),
                h('line', { x1: '14', y1: '11', x2: '14', y2: '17' })
              ])
            ]),
            default: () => '删除'
          }),
          default: () => '确定删除该任务吗？'
        })
      ])
    }
  }
]

// 事件处理
const handleSearch = () => {
  pagination.page = 1
  message.success('搜索完成')
}

const handleReset = () => {
  filters.taskId = ''
  filters.description = ''
  filters.scheduleType = null
  filters.runMode = null
  filters.owner = ''
  filters.status = null
  pagination.page = 1
}

const handleCreate = () => {
  message.info('新建任务')
}

const handleCheck = (keys) => {
  checkedRowKeys.value = keys
}

const handleBatchDelete = () => {
  message.warning(`批量删除 ${checkedRowKeys.value.length} 个任务`)
}

const handleExecute = (row) => {
  message.success(`执行任务: ${row.id}`)
}

const handleEdit = (row) => {
  message.info(`编辑任务: ${row.id}`)
}

const handleToggleStatus = (row) => {
  const action = row.status === 'running' ? '暂停' : '启动'
  message.success(`${action}任务: ${row.id}`)
}

const handleDelete = (row) => {
  const index = tasks.value.findIndex(t => t.id === row.id)
  if (index > -1) {
    tasks.value.splice(index, 1)
    message.success(`删除任务: ${row.id}`)
  }
}
</script>

<style scoped>
.task-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.filter-card :deep(.n-card__content) {
  padding: 20px 24px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 160px;
}

.filter-item label {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.table-card :deep(.n-card__content) {
  padding: 20px 24px;
}

.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

/* 任务ID单元格 */
.task-table :deep(.task-id-cell) {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 0.8125rem;
  color: var(--primary-color);
  font-weight: 600;
  background: var(--primary-bg);
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
}

/* 任务描述 */
.task-table :deep(.n-data-table-td .n-ellipsis) {
  color: var(--text-secondary);
  font-weight: 500;
}

/* 调度类型单元格 */
.task-table :deep(.schedule-cell) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-table :deep(.schedule-type) {
  font-weight: 500;
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.task-table :deep(.schedule-expr) {
  font-size: 0.75rem;
  color: #8b8fa3;
  font-family: 'JetBrains Mono', 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-weight: 500;
  background: rgba(148, 163, 184, 0.08);
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  letter-spacing: 0.02em;
  transition: all 0.2s ease;
}

.task-table :deep(.schedule-expr)::before {
  content: '⏱';
  font-size: 0.7rem;
  opacity: 0.6;
}

.task-table :deep(.schedule-expr:hover) {
  color: #6b7280;
  background: rgba(148, 163, 184, 0.12);
  border-color: rgba(148, 163, 184, 0.2);
}

/* 运行模式标签 */
.task-table :deep(.run-mode-badge) {
  font-size: 0.75rem;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-block;
}

.task-table :deep(.mode-standalone) {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.task-table :deep(.mode-broadcast) {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.task-table :deep(.mode-sharding) {
  background: rgba(236, 72, 153, 0.1);
  color: #ec4899;
}

.task-table :deep(.mode-mapreduce) {
  background: rgba(14, 165, 233, 0.1);
  color: #0ea5e9;
}

/* 负责人 */
.task-table :deep(.owner-cell) {
  font-weight: 500;
  color: var(--text-secondary);
}

/* 操作按钮 */
.task-table :deep(.action-buttons) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-table :deep(.action-btn) {
  background: transparent;
  border: none;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.task-table :deep(.action-btn svg) {
  width: 16px;
  height: 16px;
}

.task-table :deep(.action-btn-primary) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-primary:hover) {
  color: #5E81F4;
}

.task-table :deep(.action-btn-info) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-info:hover) {
  color: #3b82f6;
}

.task-table :deep(.action-btn-success) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-success:hover) {
  color: #10b981;
}

.task-table :deep(.action-btn-warning) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-warning:hover) {
  color: #f59e0b;
}

.task-table :deep(.action-btn-error) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-error:hover) {
  color: #ef4444;
}

/* 响应式 */
@media (max-width: 1200px) {
  .filter-item {
    min-width: 140px;
  }
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-item {
    width: 100%;
  }
  
  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>
