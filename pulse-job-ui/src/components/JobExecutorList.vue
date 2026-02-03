<template>
  <div class="executor-management">
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
            placeholder="搜索执行器名称或描述..."
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
        <!-- WebSocket 连接状态 -->
        <div class="ws-status" :class="wsStatusClass" @click="toggleWsConnection">
          <span class="ws-dot"></span>
          <span class="ws-text">{{ wsStatusText }}</span>
        </div>
        <div class="tool-divider"></div>
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
          新建执行器
        </n-tooltip>
        <div class="tool-divider"></div>
        <!-- 视图切换 -->
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
                <rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>
              </svg>
            </button>
          </template>
          卡片视图
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/>
                <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
              </svg>
            </button>
          </template>
          列表视图
        </n-tooltip>
      </div>
    </div>

    <!-- 高级筛选面板 -->
    <transition name="slide-down">
      <div v-if="showAdvancedFilter" class="advanced-filter">
        <div class="filter-row">
          <div class="filter-field">
            <label>执行器名称</label>
            <n-input v-model:value="filters.executorName" placeholder="输入名称" clearable size="small" />
          </div>
          <div class="filter-field">
            <label>注册方式</label>
            <n-select v-model:value="filters.registerType" :options="registerTypeOptions" placeholder="全部" clearable size="small" />
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

    <!-- 执行器卡片视图 -->
    <div v-if="viewMode === 'card'" class="card-section">
      <div class="executor-grid">
        <div 
          v-for="executor in filteredExecutorList" 
          :key="executor.id" 
          class="executor-card"
          :class="{ offline: !isOnline(executor) }"
        >
          <div class="executor-header">
            <div class="executor-status" :class="isOnline(executor) ? 'online' : 'offline'"></div>
            <div class="executor-info">
              <h3 class="executor-name">{{ executor.executorName }}</h3>
              <p class="executor-desc">{{ executor.executorDesc || '暂无描述' }}</p>
            </div>
            <n-dropdown :options="actionOptions" @select="(key) => handleAction(key, executor)">
              <n-button quaternary circle size="small">
                <template #icon>
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                    <circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/>
                  </svg>
                </template>
              </n-button>
            </n-dropdown>
          </div>

          <div class="executor-body">
            <div class="info-item">
              <span class="info-label">注册方式</span>
              <n-tag size="small" :type="executor.registerType === 'AUTO' ? 'success' : 'info'">
                {{ executor.registerType === 'AUTO' ? '自动注册' : '手动注册' }}
              </n-tag>
            </div>
            <div class="info-item">
              <span class="info-label">地址列表</span>
              <div class="address-list">
                <n-tag 
                  v-for="(addr, idx) in getAddresses(executor)" 
                  :key="idx" 
                  size="small"
                  :type="isAddressOnline(addr) ? 'success' : 'default'"
                >
                  {{ addr }}
                </n-tag>
                <span v-if="getAddresses(executor).length === 0" class="no-address">暂无节点</span>
              </div>
            </div>
          </div>

          <div class="executor-footer">
            <div class="footer-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
              </svg>
              <span>{{ formatTime(executor.updateTime) }}</span>
            </div>
            <div class="footer-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/>
              </svg>
              <span>{{ getAddresses(executor).length }} 个节点</span>
            </div>
          </div>

          <!-- 在线状态动画 -->
          <div v-if="isOnline(executor)" class="pulse-ring"></div>
        </div>
      </div>
    </div>

    <!-- 执行器列表视图 -->
    <div v-else class="table-section">
      <n-data-table
        :columns="tableColumns"
        :data="filteredExecutorList"
        :pagination="pagination"
        :row-key="row => row.id"
        class="executor-table"
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" :title="editingExecutor ? '编辑执行器' : '新建执行器'" style="width: 500px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="80px">
        <n-form-item label="名称" path="executorName">
          <n-input v-model:value="formData.executorName" placeholder="如: executor-default" />
        </n-form-item>
        <n-form-item label="描述" path="executorDesc">
          <n-input v-model:value="formData.executorDesc" placeholder="执行器描述" />
        </n-form-item>
        <n-form-item label="注册方式" path="registerType">
          <n-radio-group v-model:value="formData.registerType">
            <n-space>
              <n-radio value="AUTO">自动注册</n-radio>
              <n-radio value="MANUAL">手动注册</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="formData.registerType === 'MANUAL'" label="地址" path="executorAddress">
          <n-input v-model:value="formData.executorAddress" placeholder="192.168.1.101:9999;192.168.1.102:9999" />
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
import { ref, h, computed, reactive, onMounted, onUnmounted } from 'vue'
import { NTag, NButton, NDropdown, NTooltip, useMessage } from 'naive-ui'
import { useWebSocket, useExecutorStatus, ConnectionState, MessageType } from '@/websocket'

const message = useMessage()

// WebSocket 连接
const { state: wsState, connect: wsConnect, disconnect: wsDisconnect, isConnected, subscribe, on } = useWebSocket()

// 执行器实时状态
const executorStatusMap = reactive(new Map()) // executorName -> status info

// 连接统计
const connectionStats = reactive({
  totalSessions: 0,
  executorSessions: 0,
  browserSessions: 0,
  timestamp: null
})

// 最后更新时间
const lastUpdateTime = ref(null)

// 实时执行器列表（从 WebSocket 消息中收集）
const realtimeExecutors = computed(() => {
  const executors = []
  for (const [id, status] of executorStatusMap) {
    executors.push({
      executorId: id,
      ...status
    })
  }
  // 按最后更新时间排序，最新的在前
  return executors.sort((a, b) => (b.lastUpdate || 0) - (a.lastUpdate || 0))
})

// 连接 WebSocket 并订阅执行器状态
onMounted(() => {
  // 连接 WebSocket
  wsConnect()
  
  // 订阅执行器状态主题
  subscribe('executor.status', handleExecutorStatus)
  
  // 订阅连接统计主题
  subscribe('connection.stats', handleConnectionStats)
  
  // 监听执行器上线/下线事件
  on(MessageType.EXECUTOR_ONLINE, handleExecutorOnline)
  on(MessageType.EXECUTOR_OFFLINE, handleExecutorOffline)
  on(MessageType.EXECUTOR_HEARTBEAT, handleExecutorHeartbeat)
  on(MessageType.STATS_UPDATE, handleConnectionStats)
})

// 处理执行器状态更新
function handleExecutorStatus(msg) {
  const data = msg.data
  if (data && data.executorId) {
    executorStatusMap.set(data.executorId, {
      ...data,
      lastUpdate: Date.now()
    })
    // 更新对应执行器的信息
    updateExecutorFromWs(data)
  }
}

// 处理执行器上线
function handleExecutorOnline(msg) {
  const { executorId, address } = msg.data
  message.success(`执行器 ${executorId} 上线: ${address}`)
  executorStatusMap.set(executorId, {
    executorId,
    address,
    status: 'online',
    lastUpdate: Date.now()
  })
  updateExecutorFromWs(msg.data)
}

// 处理执行器下线
function handleExecutorOffline(msg) {
  const { executorId, reason } = msg.data
  message.warning(`执行器 ${executorId} 下线: ${reason || '未知原因'}`)
  const existing = executorStatusMap.get(executorId)
  if (existing) {
    existing.status = 'offline'
    existing.lastUpdate = Date.now()
  }
}

// 处理执行器心跳
function handleExecutorHeartbeat(msg) {
  const data = msg.data
  if (data && data.executorId) {
    const existing = executorStatusMap.get(data.executorId)
    if (existing) {
      // 更新现有数据
      Object.assign(existing, data)
      existing.lastHeartbeat = Date.now()
      existing.lastUpdate = Date.now()
      existing.status = 'online'
    } else {
      // 新增执行器
      executorStatusMap.set(data.executorId, {
        ...data,
        status: 'online',
        lastHeartbeat: Date.now(),
        lastUpdate: Date.now()
      })
    }
    lastUpdateTime.value = Date.now()
  }
}

// 处理连接统计更新
function handleConnectionStats(msg) {
  const data = msg.data
  if (data) {
    connectionStats.totalSessions = data.totalSessions || 0
    connectionStats.executorSessions = data.executorSessions || 0
    connectionStats.browserSessions = data.browserSessions || 0
    connectionStats.timestamp = data.timestamp || Date.now()
    lastUpdateTime.value = Date.now()
  }
}

// 格式化最后更新时间
function formatLastUpdate() {
  if (!lastUpdateTime.value) return '未知'
  const diff = Date.now() - lastUpdateTime.value
  if (diff < 1000) return '刚刚'
  if (diff < 60000) return `${Math.floor(diff / 1000)} 秒前`
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  return new Date(lastUpdateTime.value).toLocaleTimeString()
}

// 格式化实时时间
function formatRealtimeTime(timestamp) {
  if (!timestamp) return '-'
  const diff = Date.now() - timestamp
  if (diff < 1000) return '刚刚'
  if (diff < 60000) return `${Math.floor(diff / 1000)}秒前`
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  return new Date(timestamp).toLocaleTimeString()
}

// 从 WebSocket 消息更新执行器列表
function updateExecutorFromWs(data) {
  if (!data.executorId) return
  
  const executor = executorList.value.find(e => e.executorName === data.executorId)
  if (executor) {
    executor.updateTime = new Date().toISOString()
    if (data.address && !executor.executorAddress.includes(data.address)) {
      executor.executorAddress = executor.executorAddress 
        ? executor.executorAddress + ';' + data.address 
        : data.address
    }
  }
}

// 视图模式
const viewMode = ref('card')

// 搜索关键词
const searchKeyword = ref('')

// 高级筛选
const showAdvancedFilter = ref(false)
const filters = reactive({
  executorName: '',
  registerType: null,
  status: null
})

// 筛选选项
const registerTypeOptions = [
  { label: '自动注册', value: 'AUTO' },
  { label: '手动注册', value: 'MANUAL' }
]

const statusOptions = [
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' }
]

// 计算激活的筛选条件数量
const activeFilterCount = computed(() => {
  let count = 0
  if (filters.executorName) count++
  if (filters.registerType) count++
  if (filters.status) count++
  return count
})

// 弹窗控制
const showCreateModal = ref(false)
const editingExecutor = ref(null)

// 表单数据
const formData = ref({
  executorName: '',
  executorDesc: '',
  registerType: 'AUTO',
  executorAddress: ''
})

const formRules = {
  executorName: { required: true, message: '请输入执行器名称' }
}

// 执行器列表
const executorList = ref([
  {
    id: 1,
    executorName: 'executor-default',
    executorDesc: '默认执行器',
    registerType: 'AUTO',
    executorAddress: '192.168.1.101:9999;192.168.1.102:9999;192.168.1.103:9999',
    updateTime: '2026-01-26T11:23:45'
  },
  {
    id: 2,
    executorName: 'executor-report',
    executorDesc: '报表专用执行器',
    registerType: 'AUTO',
    executorAddress: '192.168.1.110:9999',
    updateTime: '2026-01-26T11:20:00'
  },
  {
    id: 3,
    executorName: 'executor-manual',
    executorDesc: '手动注册执行器',
    registerType: 'MANUAL',
    executorAddress: '10.0.0.100:9999',
    updateTime: '2026-01-25T10:15:00'
  }
])

// 筛选后的执行器列表
const filteredExecutorList = computed(() => {
  return executorList.value.filter(executor => {
    // 关键词搜索
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      const matchKeyword = executor.executorName.toLowerCase().includes(keyword) ||
             (executor.executorDesc && executor.executorDesc.toLowerCase().includes(keyword)) ||
             (executor.executorAddress && executor.executorAddress.toLowerCase().includes(keyword))
      if (!matchKeyword) return false
    }
    // 执行器名称筛选
    if (filters.executorName && !executor.executorName.toLowerCase().includes(filters.executorName.toLowerCase())) {
      return false
    }
    // 注册方式筛选
    if (filters.registerType && executor.registerType !== filters.registerType) {
      return false
    }
    // 状态筛选
    if (filters.status) {
      const online = isOnline(executor)
      if (filters.status === 'online' && !online) return false
      if (filters.status === 'offline' && online) return false
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

// 操作菜单
const actionOptions = [
  { label: '编辑', key: 'edit' },
  { label: '刷新', key: 'refresh' },
  { type: 'divider', key: 'd1' },
  { label: '删除', key: 'delete' }
]

// 表格列定义
const tableColumns = [
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      const online = isOnline(row)
      return h('div', { class: 'table-status' }, [
        h('span', { class: ['status-dot', online ? 'online' : 'offline'] }),
        h('span', { class: 'status-text' }, online ? '在线' : '离线')
      ])
    }
  },
  {
    title: '执行器名称',
    key: 'executorName',
    width: 180,
    render(row) {
      return h('span', { class: 'executor-name-cell' }, row.executorName)
    }
  },
  {
    title: '描述',
    key: 'executorDesc',
    width: 160,
    ellipsis: { tooltip: true },
    render(row) {
      return h('span', { class: 'desc-cell' }, row.executorDesc || '暂无描述')
    }
  },
  {
    title: '注册方式',
    key: 'registerType',
    width: 100,
    render(row) {
      return h(NTag, {
        size: 'small',
        type: row.registerType === 'AUTO' ? 'success' : 'info',
        round: false
      }, { default: () => row.registerType === 'AUTO' ? '自动注册' : '手动注册' })
    }
  },
  {
    title: '节点地址',
    key: 'executorAddress',
    render(row) {
      const addresses = getAddresses(row)
      if (addresses.length === 0) {
        return h('span', { class: 'no-address' }, '暂无节点')
      }
      return h('div', { class: 'address-tags' }, 
        addresses.slice(0, 2).map((addr, idx) => 
          h(NTag, {
            key: idx,
            size: 'small',
            type: isAddressOnline(addr) ? 'success' : 'default'
          }, { default: () => addr })
        ).concat(
          addresses.length > 2 ? [h('span', { class: 'more-count' }, `+${addresses.length - 2}`)] : []
        )
      )
    }
  },
  {
    title: '节点数',
    key: 'nodeCount',
    width: 80,
    render(row) {
      return h('span', { class: 'node-count' }, getAddresses(row).length)
    }
  },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 130,
    render(row) {
      return h('span', { class: 'time-cell' }, formatTime(row.updateTime))
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 130,
    render(row) {
      return h('div', { class: 'action-buttons' }, [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h('button', {
            class: 'action-btn action-btn-info',
            onClick: () => handleAction('edit', row)
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
            class: 'action-btn action-btn-success',
            onClick: () => handleAction('refresh', row)
          }, [
            h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
              h('path', { d: 'M23 4v6h-6' }),
              h('path', { d: 'M1 20v-6h6' }),
              h('path', { d: 'M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15' })
            ])
          ]),
          default: () => '刷新'
        }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h('button', {
            class: 'action-btn action-btn-error',
            onClick: () => handleAction('delete', row)
          }, [
            h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
              h('path', { d: 'M3 6h18' }),
              h('path', { d: 'M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2' })
            ])
          ]),
          default: () => '删除'
        })
      ])
    }
  }
]

// WebSocket 状态计算属性
const wsStatusClass = computed(() => {
  switch (wsState.state) {
    case ConnectionState.CONNECTED:
      return 'connected'
    case ConnectionState.CONNECTING:
    case ConnectionState.RECONNECTING:
      return 'connecting'
    default:
      return 'disconnected'
  }
})

const wsStatusText = computed(() => {
  switch (wsState.state) {
    case ConnectionState.CONNECTED:
      return '实时同步'
    case ConnectionState.CONNECTING:
      return '连接中...'
    case ConnectionState.RECONNECTING:
      return '重连中...'
    default:
      return '未连接'
  }
})

// 切换 WebSocket 连接
function toggleWsConnection() {
  if (isConnected.value) {
    wsDisconnect()
    message.info('已断开实时连接')
  } else {
    wsConnect()
    message.info('正在连接...')
  }
}

// 工具方法
const getAddresses = (executor) => {
  if (!executor.executorAddress) return []
  return executor.executorAddress.split(';').filter(Boolean)
}

const isOnline = (executor) => {
  // 优先使用 WebSocket 实时状态
  const wsStatus = executorStatusMap.get(executor.executorName)
  if (wsStatus) {
    // 如果有 WebSocket 状态，检查最后心跳时间
    const heartbeatTimeout = 90000 // 90秒无心跳视为离线
    if (wsStatus.lastHeartbeat && Date.now() - wsStatus.lastHeartbeat < heartbeatTimeout) {
      return true
    }
    return wsStatus.status === 'online'
  }
  
  // 降级：使用更新时间判断
  const updateTime = new Date(executor.updateTime)
  const now = new Date()
  return (now - updateTime) < 5 * 60 * 1000
}

const isAddressOnline = (addr) => {
  // 检查地址是否在任何执行器的实时状态中
  for (const [, status] of executorStatusMap) {
    if (status.address === addr && status.status === 'online') {
      return true
    }
  }
  // 降级：随机模拟
  return Math.random() > 0.1
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { 
    month: '2-digit', 
    day: '2-digit', 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

// 筛选操作
const handleSearch = () => {
  pagination.page = 1
  message.success('查询完成')
}

const handleReset = () => {
  searchKeyword.value = ''
  filters.executorName = ''
  filters.registerType = null
  filters.status = null
  pagination.page = 1
}

// 操作处理
const handleRefresh = () => {
  message.success('刷新成功')
}

const handleCreate = () => {
  editingExecutor.value = null
  formData.value = { executorName: '', executorDesc: '', registerType: 'AUTO', executorAddress: '' }
  showCreateModal.value = true
}

const handleAction = (key, executor) => {
  switch (key) {
    case 'edit':
      editingExecutor.value = executor
      Object.assign(formData.value, executor)
      showCreateModal.value = true
      break
    case 'refresh':
      executor.updateTime = new Date().toISOString()
      message.success('刷新成功')
      break
    case 'delete':
      const idx = executorList.value.findIndex(e => e.id === executor.id)
      if (idx > -1) {
        executorList.value.splice(idx, 1)
        message.success('删除成功')
      }
      break
  }
}

const handleSubmit = () => {
  if (editingExecutor.value) {
    Object.assign(editingExecutor.value, formData.value)
    message.success('更新成功')
  } else {
    const newExecutor = {
      ...formData.value,
      id: Date.now(),
      updateTime: new Date().toISOString()
    }
    executorList.value.unshift(newExecutor)
    message.success('创建成功')
  }
  showCreateModal.value = false
  editingExecutor.value = null
  formData.value = { executorName: '', executorDesc: '', registerType: 'AUTO', executorAddress: '' }
}
</script>

<style scoped>
.executor-management {
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

.tool-btn.active {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.tool-divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
  margin: 0 6px;
}

/* WebSocket 状态指示器 */
.ws-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.ws-status:hover {
  background: rgba(0, 0, 0, 0.04);
}

.ws-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: background-color 0.3s ease;
}

.ws-status.connected .ws-dot {
  background: #52c41a;
  box-shadow: 0 0 8px rgba(82, 196, 26, 0.5);
}

.ws-status.connecting .ws-dot {
  background: #faad14;
  animation: ws-pulse 1s ease-in-out infinite;
}

.ws-status.disconnected .ws-dot {
  background: #d9d9d9;
}

@keyframes ws-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.ws-text {
  font-size: 12px;
  font-weight: 500;
}

.ws-status.connected .ws-text {
  color: #52c41a;
}

.ws-status.connecting .ws-text {
  color: #faad14;
}

.ws-status.disconnected .ws-text {
  color: #999;
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

/* ==================== 卡片视图 ==================== */
.card-section {
  background: #fff;
  border-radius: 0 0 10px 10px;
  padding: 20px;
}

.advanced-filter + .card-section,
.advanced-filter + .table-section {
  border-radius: 0 0 10px 10px;
}

.executor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.executor-card {
  background: #fafbfc;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--border-color);
  position: relative;
  overflow: hidden;
  transition: all 0.2s ease;
}

.executor-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: #e0e0e0;
}

.executor-card.offline {
  opacity: 0.7;
}

.executor-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
}

.executor-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.executor-status.online {
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.5);
}

.executor-status.offline {
  background: #9ca3af;
}

.executor-info {
  flex: 1;
  min-width: 0;
}

.executor-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 2px 0;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
}

.executor-desc {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin: 0;
}

.executor-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 0.6875rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.address-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.no-address {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.executor-footer {
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: var(--text-muted);
}

.pulse-ring {
  position: absolute;
  top: 22px;
  left: 16px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: pulse-ring 2s infinite;
}

@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4); }
  70% { box-shadow: 0 0 0 8px rgba(34, 197, 94, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

/* ==================== 表格视图 ==================== */
.table-section {
  background: #fff;
  border-radius: 0 0 10px 10px;
  padding: 16px 20px;
}

/* 分页样式 */
.executor-table :deep(.n-data-table__pagination) {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.executor-table :deep(.n-pagination-prefix) {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

/* 状态单元格 */
.executor-table :deep(.table-status) {
  display: flex;
  align-items: center;
  gap: 6px;
}

.executor-table :deep(.status-dot) {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.executor-table :deep(.status-dot.online) {
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.5);
}

.executor-table :deep(.status-dot.offline) {
  background: #9ca3af;
}

.executor-table :deep(.status-text) {
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

/* 执行器名称 */
.executor-table :deep(.executor-name-cell) {
  font-weight: 500;
  color: var(--text-primary);
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 0.8125rem;
}

/* 描述 */
.executor-table :deep(.desc-cell) {
  color: var(--text-secondary);
  font-size: 0.8125rem;
}

/* 节点地址 */
.executor-table :deep(.address-tags) {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.executor-table :deep(.no-address) {
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.executor-table :deep(.more-count) {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-left: 2px;
}

/* 节点数 */
.executor-table :deep(.node-count) {
  font-weight: 600;
  color: var(--primary-color);
  font-size: 0.875rem;
}

/* 更新时间 */
.executor-table :deep(.time-cell) {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

/* 操作按钮 */
.executor-table :deep(.action-buttons) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.executor-table :deep(.action-btn) {
  background: transparent;
  border: none;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.executor-table :deep(.action-btn svg) {
  width: 16px;
  height: 16px;
}

.executor-table :deep(.action-btn-info) {
  color: var(--text-secondary);
}

.executor-table :deep(.action-btn-info:hover) {
  color: #3b82f6;
}

.executor-table :deep(.action-btn-success) {
  color: var(--text-secondary);
}

.executor-table :deep(.action-btn-success:hover) {
  color: #10b981;
}

.executor-table :deep(.action-btn-error) {
  color: var(--text-secondary);
}

.executor-table :deep(.action-btn-error:hover) {
  color: #ef4444;
}
</style>
