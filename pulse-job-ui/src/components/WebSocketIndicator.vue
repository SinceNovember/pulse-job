<template>
  <div class="ws-indicator" :class="stateClass" @click="togglePanel">
    <div class="ws-status">
      <span class="ws-dot"></span>
      <span class="ws-text">{{ stateText }}</span>
    </div>
    
    <!-- 详情面板 -->
    <transition name="fade">
      <div v-if="showPanel" class="ws-panel">
        <div class="ws-panel-header">
          <span>WebSocket 连接状态</span>
          <button class="ws-close" @click.stop="showPanel = false">&times;</button>
        </div>
        <div class="ws-panel-body">
          <div class="ws-info-row">
            <span class="ws-label">状态</span>
            <span class="ws-value" :class="stateClass">{{ stateText }}</span>
          </div>
          <div class="ws-info-row">
            <span class="ws-label">客户端ID</span>
            <span class="ws-value ws-mono">{{ state.clientId || '-' }}</span>
          </div>
          <div class="ws-info-row">
            <span class="ws-label">消息数</span>
            <span class="ws-value">{{ state.messageCount }}</span>
          </div>
          <div class="ws-info-row" v-if="state.reconnecting">
            <span class="ws-label">重连次数</span>
            <span class="ws-value">{{ state.reconnectAttempt }}</span>
          </div>
        </div>
        <div class="ws-panel-footer">
          <button 
            v-if="!isConnected" 
            class="ws-btn ws-btn-primary"
            @click.stop="handleConnect"
          >
            连接
          </button>
          <button 
            v-else 
            class="ws-btn ws-btn-secondary"
            @click.stop="handleDisconnect"
          >
            断开
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useWebSocket, ConnectionState } from '@/websocket'

const { state, isConnected, connect, disconnect } = useWebSocket()

const showPanel = ref(false)

const stateClass = computed(() => {
  switch (state.state) {
    case ConnectionState.CONNECTED:
      return 'connected'
    case ConnectionState.CONNECTING:
    case ConnectionState.RECONNECTING:
      return 'connecting'
    case ConnectionState.DISCONNECTED:
    case ConnectionState.CLOSED:
    default:
      return 'disconnected'
  }
})

const stateText = computed(() => {
  switch (state.state) {
    case ConnectionState.CONNECTED:
      return '已连接'
    case ConnectionState.CONNECTING:
      return '连接中...'
    case ConnectionState.RECONNECTING:
      return '重连中...'
    case ConnectionState.DISCONNECTED:
      return '未连接'
    case ConnectionState.CLOSED:
      return '已关闭'
    default:
      return '未知'
  }
})

function togglePanel() {
  showPanel.value = !showPanel.value
}

function handleConnect() {
  connect()
}

function handleDisconnect() {
  disconnect()
}
</script>

<style scoped>
.ws-indicator {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.ws-indicator:hover {
  background: rgba(0, 0, 0, 0.05);
}

.ws-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ws-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: background-color 0.3s ease;
}

.ws-indicator.connected .ws-dot {
  background: #52c41a;
  box-shadow: 0 0 8px rgba(82, 196, 26, 0.5);
}

.ws-indicator.connecting .ws-dot {
  background: #faad14;
  animation: pulse 1s ease-in-out infinite;
}

.ws-indicator.disconnected .ws-dot {
  background: #ff4d4f;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.ws-text {
  font-size: 12px;
  color: #666;
}

.ws-indicator.connected .ws-text {
  color: #52c41a;
}

.ws-indicator.connecting .ws-text {
  color: #faad14;
}

.ws-indicator.disconnected .ws-text {
  color: #ff4d4f;
}

/* 详情面板 */
.ws-panel {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  width: 280px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.ws-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f5f5f5;
  border-bottom: 1px solid #eee;
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.ws-close {
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.ws-close:hover {
  color: #333;
}

.ws-panel-body {
  padding: 12px 16px;
}

.ws-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.ws-info-row:last-child {
  border-bottom: none;
}

.ws-label {
  font-size: 12px;
  color: #999;
}

.ws-value {
  font-size: 12px;
  color: #333;
  font-weight: 500;
}

.ws-value.connected {
  color: #52c41a;
}

.ws-value.connecting {
  color: #faad14;
}

.ws-value.disconnected {
  color: #ff4d4f;
}

.ws-mono {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 11px;
}

.ws-panel-footer {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  text-align: right;
}

.ws-btn {
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.ws-btn-primary {
  background: #1890ff;
  color: #fff;
}

.ws-btn-primary:hover {
  background: #40a9ff;
}

.ws-btn-secondary {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #d9d9d9;
}

.ws-btn-secondary:hover {
  background: #fff;
  border-color: #1890ff;
  color: #1890ff;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
