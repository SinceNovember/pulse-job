<template>
  <div class="executor-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">执行器管理</h2>
      <div class="header-actions">
        <n-button type="primary" @click="showCreateModal = true">
          <template #icon>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
          </template>
          新建执行器
        </n-button>
      </div>
    </div>

    <!-- 执行器卡片 -->
    <div class="executor-grid">
      <div 
        v-for="executor in executorList" 
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
            <n-button quaternary circle>
              <template #icon>
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
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
import { ref, h } from 'vue'
import { useMessage } from 'naive-ui'

const message = useMessage()

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

// 操作菜单
const actionOptions = [
  { label: '编辑', key: 'edit' },
  { label: '刷新', key: 'refresh' },
  { type: 'divider', key: 'd1' },
  { label: '删除', key: 'delete' }
]

// 工具方法
const getAddresses = (executor) => {
  if (!executor.executorAddress) return []
  return executor.executorAddress.split(';').filter(Boolean)
}

const isOnline = (executor) => {
  // 模拟：5分钟内有更新视为在线
  const updateTime = new Date(executor.updateTime)
  const now = new Date()
  return (now - updateTime) < 5 * 60 * 1000 || Math.random() > 0.2
}

const isAddressOnline = (addr) => {
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

// 操作处理
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
.executor-page {
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
}

.executor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.executor-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--border-color);
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.executor-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.executor-card.offline {
  opacity: 0.7;
}

.executor-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.executor-status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.executor-status.online {
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.5);
}

.executor-status.offline {
  background: #9ca3af;
}

.executor-info {
  flex: 1;
  min-width: 0;
}

.executor-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.executor-desc {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin: 0;
}

.executor-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.address-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.executor-footer {
  display: flex;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--text-muted);
}

.pulse-ring {
  position: absolute;
  top: 28px;
  left: 20px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: pulse-ring 2s infinite;
}

@keyframes pulse-ring {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(34, 197, 94, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
  }
}
</style>


