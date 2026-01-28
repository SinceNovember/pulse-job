<template>
  <div class="task-management">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input 
            v-model="filters.keyword" 
            type="text" 
            class="search-input" 
            placeholder="搜索任务名称或ID..."
            @keyup.enter="handleSearch"
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
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" @click="handleCreate">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
          </template>
          新建任务
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" :disabled="checkedRowKeys.length === 0" @click="handleBatchDelete">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 6h18"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
              </svg>
            </button>
          </template>
          批量删除
        </n-tooltip>
        <div class="tool-divider"></div>
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" @click="tableExpanded = !tableExpanded">
              <svg v-if="!tableExpanded" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/>
                <line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/>
                <line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/>
              </svg>
            </button>
          </template>
          {{ tableExpanded ? '收起' : '展开' }}
        </n-tooltip>
        <n-dropdown trigger="click" :options="moreOptions" @select="handleMoreAction">
          <button class="tool-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/>
            </svg>
          </button>
        </n-dropdown>
      </div>
    </div>

    <!-- 高级筛选面板 -->
    <transition name="slide-down">
      <div v-if="showAdvancedFilter" class="advanced-filter">
        <div class="filter-row">
          <div class="filter-field">
            <label>任务ID</label>
            <n-input v-model:value="filters.taskId" placeholder="输入任务ID" clearable size="small" />
          </div>
          <div class="filter-field">
            <label>调度类型</label>
            <n-select v-model:value="filters.scheduleType" :options="scheduleTypeOptions" placeholder="全部" clearable size="small" />
          </div>
          <div class="filter-field">
            <label>运行模式</label>
            <n-select v-model:value="filters.runMode" :options="runModeOptions" placeholder="全部" clearable size="small" />
          </div>
          <div class="filter-field">
            <label>负责人</label>
            <n-input v-model:value="filters.owner" placeholder="输入负责人" clearable size="small" />
          </div>
          <div class="filter-field">
            <label>状态</label>
            <n-select v-model:value="filters.status" :options="statusOptions" placeholder="全部" clearable size="small" />
          </div>
          <div class="filter-field filter-actions">
            <n-button size="small" @click="handleReset">重置</n-button>
            <n-button size="small" type="primary" @click="handleSearch">查询</n-button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 表格卡片 -->
    <div class="table-section">
      <n-data-table
        :columns="columns"
        :data="filteredTasks"
        :pagination="pagination"
        :row-key="row => row.id"
        :checked-row-keys="checkedRowKeys"
        @update:checked-row-keys="handleCheck"
        class="task-table"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h } from 'vue'
import { 
  NInput, NSelect, NButton, NDataTable, NTag, NPopconfirm, NTooltip, NDropdown,
  useMessage
} from 'naive-ui'

const message = useMessage()

// 状态
const showAdvancedFilter = ref(false)
const tableExpanded = ref(false)

// 更多操作选项
const moreOptions = [
  { label: '导出数据', key: 'export' },
  { label: '导入任务', key: 'import' },
  { type: 'divider' },
  { label: '批量启用', key: 'batch-enable' },
  { label: '批量禁用', key: 'batch-disable' }
]

// 筛选条件
const filters = reactive({
  keyword: '',
  taskId: '',
  scheduleType: null,
  runMode: null,
  owner: '',
  status: null
})

// 计算激活的筛选条件数量
const activeFilterCount = computed(() => {
  let count = 0
  if (filters.taskId) count++
  if (filters.scheduleType) count++
  if (filters.runMode) count++
  if (filters.owner) count++
  if (filters.status) count++
  return count
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
    // 关键词搜索 - 匹配ID或描述
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (!task.id.toLowerCase().includes(kw) && !task.description.toLowerCase().includes(kw)) {
        return false
      }
    }
    if (filters.taskId && !task.id.toLowerCase().includes(filters.taskId.toLowerCase())) return false
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
    width: 140,
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
  filters.keyword = ''
  filters.taskId = ''
  filters.scheduleType = null
  filters.runMode = null
  filters.owner = ''
  filters.status = null
  pagination.page = 1
}

const handleRefresh = () => {
  message.success('刷新成功')
}

const handleMoreAction = (key) => {
  const actionMap = {
    'export': '导出数据',
    'import': '导入任务',
    'batch-enable': '批量启用',
    'batch-disable': '批量禁用'
  }
  message.info(actionMap[key] || key)
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
  gap: 0;
}

/* ==================== 工具栏样式 ==================== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-radius: 10px 10px 0 0;
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
  width: 240px;
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

.tool-btn:hover svg {
  color: var(--primary-color);
}

.tool-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tool-btn:disabled:hover {
  background: transparent;
  color: var(--text-secondary);
}

.tool-divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
  margin: 0 6px;
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

/* 去掉下拉框聚焦时的蓝色边框 */
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
  border-radius: 0 0 10px 10px;
  padding: 16px 20px;
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
  gap: 6px;
}

.task-table :deep(.schedule-type) {
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.task-table :deep(.schedule-expr) {
  display: inline-block;
  font-size: 0.75rem;
  color: #8b95a5;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  background: #f4f6f8;
  padding: 4px 10px;
  border-radius: 6px;
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
@media (max-width: 900px) {
  .toolbar {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .toolbar-left {
    order: 1;
    width: 100%;
  }
  
  .search-box {
    flex: 1;
  }
  
  .toolbar-right {
    order: 2;
    width: 100%;
    justify-content: flex-end;
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
}

@media (max-width: 640px) {
  .toolbar {
    padding: 12px 16px;
  }
  
  .search-box {
    width: 100%;
  }
  
  .table-section {
    padding: 12px 16px;
  }
}
</style>
