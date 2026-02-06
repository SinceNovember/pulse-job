<template>
  <div class="system-config">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">系统配置</h2>
      <p class="page-desc">管理调度系统的全局配置项</p>
    </div>

    <!-- 配置卡片区域 -->
    <div class="config-cards">
      <!-- 注册配置 -->
      <div class="config-card">
        <div class="card-header">
          <div class="card-icon register">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="8" rx="2"/>
              <rect x="2" y="14" width="20" height="8" rx="2"/>
              <path d="M6 6h.01"/><path d="M6 18h.01"/>
            </svg>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">注册配置</h3>
            <p class="card-subtitle">控制执行器和任务的自动注册行为</p>
          </div>
        </div>

        <div class="card-body">
          <!-- 执行器自动注册 -->
          <div class="config-item">
            <div class="config-info">
              <div class="config-label">执行器自动注册</div>
              <div class="config-desc">
                启用后，当执行器连接到调度中心时，如果执行器不存在会自动创建执行器记录。
                禁用后，需要先在管理界面手动创建执行器。
              </div>
            </div>
            <n-switch 
              v-model:value="registerConfig.autoRegisterExecutor" 
              :loading="saving"
              @update:value="handleConfigChange"
            >
              <template #checked>开启</template>
              <template #unchecked>关闭</template>
            </n-switch>
          </div>

          <!-- 任务自动注册 -->
          <div class="config-item">
            <div class="config-info">
              <div class="config-label">任务自动注册</div>
              <div class="config-desc">
                启用后，执行器上报的 JobHandler 会自动注册为可调度的任务。
                禁用后，需要在任务管理中手动创建任务。
              </div>
            </div>
            <n-switch 
              v-model:value="registerConfig.autoRegisterJob" 
              :loading="saving"
              @update:value="handleConfigChange"
            >
              <template #checked>开启</template>
              <template #unchecked>关闭</template>
            </n-switch>
          </div>
        </div>

        <div class="card-footer">
          <div class="status-indicator" :class="{ saved: !hasChanges, unsaved: hasChanges }">
            <span class="status-dot"></span>
            <span class="status-text">{{ hasChanges ? '有未保存的更改' : '配置已同步' }}</span>
          </div>
          <n-button 
            type="primary" 
            :loading="saving" 
            :disabled="!hasChanges"
            @click="saveConfig"
          >
            保存配置
          </n-button>
        </div>
      </div>

      <!-- 更多配置项可以在这里添加 -->
      <div class="config-card coming-soon">
        <div class="card-header">
          <div class="card-icon more">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
            </svg>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">更多配置</h3>
            <p class="card-subtitle">更多系统配置项即将推出</p>
          </div>
        </div>
        <div class="card-body placeholder">
          <p>调度策略、告警配置、日志清理等功能正在开发中...</p>
        </div>
      </div>
    </div>

    <!-- 配置说明 -->
    <div class="config-tips">
      <h4>配置说明</h4>
      <ul>
        <li>
          <strong>执行器自动注册</strong>：建议在生产环境中<em>关闭</em>此选项，
          以避免未经授权的执行器接入调度系统。
        </li>
        <li>
          <strong>任务自动注册</strong>：建议在生产环境中<em>关闭</em>此选项，
          通过手动创建任务来确保任务配置的准确性和安全性。
        </li>
        <li>
          配置修改后会<em>立即生效</em>，无需重启服务。
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'

const message = useMessage()

// API 基础地址
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// 加载状态
const loading = ref(false)
const saving = ref(false)

// 注册配置
const registerConfig = reactive({
  autoRegisterExecutor: true,
  autoRegisterJob: false
})

// 原始配置（用于检测是否有变更）
const originalConfig = reactive({
  autoRegisterExecutor: true,
  autoRegisterJob: false
})

// 是否有未保存的更改
const hasChanges = computed(() => {
  return registerConfig.autoRegisterExecutor !== originalConfig.autoRegisterExecutor ||
         registerConfig.autoRegisterJob !== originalConfig.autoRegisterJob
})

// 加载配置
async function fetchConfig() {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/systemConfig/register`)
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      registerConfig.autoRegisterExecutor = result.data.autoRegisterExecutor ?? true
      registerConfig.autoRegisterJob = result.data.autoRegisterJob ?? false
      // 同步原始配置
      originalConfig.autoRegisterExecutor = registerConfig.autoRegisterExecutor
      originalConfig.autoRegisterJob = registerConfig.autoRegisterJob
    } else {
      message.error(result.message || '加载配置失败')
    }
  } catch (error) {
    console.error('加载配置失败:', error)
    message.error('加载配置失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 配置变更时的处理（可选：实时保存）
function handleConfigChange() {
  // 可以在这里添加实时保存逻辑，或者仅标记为已更改
}

// 保存配置
async function saveConfig() {
  saving.value = true
  try {
    const response = await fetch(`${API_BASE}/api/systemConfig/register`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        autoRegisterExecutor: registerConfig.autoRegisterExecutor,
        autoRegisterJob: registerConfig.autoRegisterJob
      })
    })
    const result = await response.json()
    
    if (result.code === 200) {
      message.success('配置保存成功')
      // 同步原始配置
      originalConfig.autoRegisterExecutor = registerConfig.autoRegisterExecutor
      originalConfig.autoRegisterJob = registerConfig.autoRegisterJob
    } else {
      message.error(result.message || '保存配置失败')
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    message.error('保存配置失败，请检查网络连接')
  } finally {
    saving.value = false
  }
}

// 组件挂载时加载配置
onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
.system-config {
  padding: 0;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin: 0;
}

/* 配置卡片区域 */
.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(480px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

/* 配置卡片 */
.config-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: box-shadow 0.2s ease;
}

.config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.config-card.coming-soon {
  opacity: 0.7;
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  background: #fafbfc;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-icon svg {
  width: 24px;
  height: 24px;
}

.card-icon.register {
  background: linear-gradient(135deg, #e8f4fd 0%, #d1e9fc 100%);
  color: #2196f3;
}

.card-icon.more {
  background: linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%);
  color: #9c27b0;
}

.card-title-group {
  flex: 1;
}

.card-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 2px 0;
}

.card-subtitle {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin: 0;
}

/* 卡片内容 */
.card-body {
  padding: 20px 24px;
}

.card-body.placeholder {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 0.875rem;
}

/* 配置项 */
.config-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 0;
  border-bottom: 1px dashed var(--border-color);
}

.config-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.config-item:first-child {
  padding-top: 0;
}

.config-info {
  flex: 1;
}

.config-label {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.config-desc {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  background: #fafbfc;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8125rem;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-indicator.saved .status-dot {
  background: #52c41a;
}

.status-indicator.saved .status-text {
  color: #52c41a;
}

.status-indicator.unsaved .status-dot {
  background: #faad14;
  animation: pulse 1.5s ease-in-out infinite;
}

.status-indicator.unsaved .status-text {
  color: #faad14;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 配置说明 */
.config-tips {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  padding: 16px 20px;
}

.config-tips h4 {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #ad6800;
  margin: 0 0 12px 0;
}

.config-tips ul {
  margin: 0;
  padding-left: 20px;
}

.config-tips li {
  font-size: 0.8125rem;
  color: #ad8b00;
  line-height: 1.8;
  margin-bottom: 4px;
}

.config-tips li:last-child {
  margin-bottom: 0;
}

.config-tips strong {
  color: #ad6800;
}

.config-tips em {
  font-style: normal;
  color: #d48806;
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 768px) {
  .config-cards {
    grid-template-columns: 1fr;
  }
  
  .config-item {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
