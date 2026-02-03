/**
 * WebSocket Vue Composable
 * 
 * 提供在 Vue 组件中使用 WebSocket 的便捷方法
 * 
 * @author PulseJob
 */

import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { getWebSocketService, ConnectionState, MessageType } from './WebSocketService'

/**
 * WebSocket 状态管理
 */
const wsState = reactive({
  state: ConnectionState.DISCONNECTED,
  clientId: null,
  connected: false,
  reconnecting: false,
  reconnectAttempt: 0,
  lastError: null,
  lastMessage: null,
  messageCount: 0
})

/**
 * 主 WebSocket composable
 */
export function useWebSocket(options = {}) {
  const ws = getWebSocketService()
  
  // 本地状态
  const isConnected = computed(() => wsState.connected)
  const connectionState = computed(() => wsState.state)
  
  // 事件处理器清理列表
  const cleanupFns = []
  
  /**
   * 连接 WebSocket
   */
  function connect() {
    ws.connect()
  }
  
  /**
   * 断开连接
   */
  function disconnect() {
    ws.disconnect()
  }
  
  /**
   * 发送消息
   */
  function send(type, data, topic = null) {
    return ws.send(type, data, topic)
  }
  
  /**
   * 订阅主题
   */
  function subscribe(topic, callback) {
    const unsubscribe = ws.subscribe(topic, callback)
    cleanupFns.push(unsubscribe)
    return unsubscribe
  }
  
  /**
   * 监听事件
   */
  function on(event, callback) {
    const off = ws.on(event, callback)
    cleanupFns.push(off)
    return off
  }
  
  // 设置全局事件监听
  if (options.autoConnect !== false) {
    // 状态变化
    ws.on('stateChange', ({ newState }) => {
      wsState.state = newState
      wsState.connected = newState === ConnectionState.CONNECTED
      wsState.reconnecting = newState === ConnectionState.RECONNECTING
    })
    
    // 连接成功
    ws.on('connected', ({ clientId }) => {
      wsState.clientId = clientId
      wsState.lastError = null
    })
    
    // 重连
    ws.on('reconnecting', ({ attempt }) => {
      wsState.reconnectAttempt = attempt
    })
    
    // 错误
    ws.on('error', (error) => {
      wsState.lastError = error
    })
    
    // 消息
    ws.on('message', (message) => {
      wsState.lastMessage = message
      wsState.messageCount++
    })
  }
  
  // 组件卸载时清理
  onUnmounted(() => {
    cleanupFns.forEach(fn => fn())
    cleanupFns.length = 0
  })
  
  return {
    // 状态
    state: wsState,
    isConnected,
    connectionState,
    
    // 方法
    connect,
    disconnect,
    send,
    subscribe,
    on,
    
    // 原始服务
    ws
  }
}

/**
 * 执行器状态订阅
 */
export function useExecutorStatus() {
  const executors = ref([])
  const lastUpdate = ref(null)
  
  const { subscribe, on, isConnected } = useWebSocket()
  
  onMounted(() => {
    // 订阅执行器状态主题
    subscribe('executor.status', (message) => {
      handleExecutorMessage(message)
    })
    
    // 监听执行器相关消息类型
    on(MessageType.EXECUTOR_STATUS, handleExecutorMessage)
    on(MessageType.EXECUTOR_ONLINE, handleExecutorOnline)
    on(MessageType.EXECUTOR_OFFLINE, handleExecutorOffline)
  })
  
  function handleExecutorMessage(message) {
    const data = message.data
    lastUpdate.value = Date.now()
    
    // 更新或添加执行器状态
    const index = executors.value.findIndex(e => e.executorId === data.executorId)
    if (index >= 0) {
      executors.value[index] = { ...executors.value[index], ...data }
    } else {
      executors.value.push(data)
    }
  }
  
  function handleExecutorOnline(message) {
    const { executorId, address } = message.data
    const index = executors.value.findIndex(e => e.executorId === executorId)
    if (index >= 0) {
      executors.value[index].status = 'online'
      executors.value[index].address = address
    } else {
      executors.value.push({
        executorId,
        address,
        status: 'online'
      })
    }
  }
  
  function handleExecutorOffline(message) {
    const { executorId } = message.data
    const index = executors.value.findIndex(e => e.executorId === executorId)
    if (index >= 0) {
      executors.value[index].status = 'offline'
    }
  }
  
  return {
    executors,
    lastUpdate,
    isConnected
  }
}

/**
 * 任务日志订阅
 */
export function useJobLog(instanceId) {
  const logs = ref([])
  const isStreaming = ref(false)
  const isComplete = ref(false)
  
  const { subscribe, on } = useWebSocket()
  
  onMounted(() => {
    if (!instanceId) return
    
    const topic = `job.log.${instanceId}`
    
    // 订阅日志主题
    subscribe(topic, (message) => {
      if (message.type === MessageType.LOG_APPEND) {
        logs.value.push(message.data)
      } else if (message.type === MessageType.LOG_STREAM) {
        isStreaming.value = true
        logs.value = []
      } else if (message.type === MessageType.LOG_END) {
        isStreaming.value = false
        isComplete.value = true
      }
    })
  })
  
  function clearLogs() {
    logs.value = []
    isComplete.value = false
  }
  
  return {
    logs,
    isStreaming,
    isComplete,
    clearLogs
  }
}

/**
 * 任务状态订阅
 */
export function useTaskStatus() {
  const taskEvents = ref([])
  const lastEvent = ref(null)
  
  const { subscribe, on } = useWebSocket()
  
  onMounted(() => {
    subscribe('task.status', (message) => {
      lastEvent.value = message
      taskEvents.value.unshift({
        ...message.data,
        type: message.type,
        receivedAt: Date.now()
      })
      
      // 只保留最近 100 条
      if (taskEvents.value.length > 100) {
        taskEvents.value = taskEvents.value.slice(0, 100)
      }
    })
    
    on(MessageType.TASK_TRIGGERED, (message) => {
      lastEvent.value = message
    })
    
    on(MessageType.TASK_COMPLETED, (message) => {
      lastEvent.value = message
    })
    
    on(MessageType.TASK_FAILED, (message) => {
      lastEvent.value = message
    })
  })
  
  return {
    taskEvents,
    lastEvent
  }
}

/**
 * 告警订阅
 */
export function useAlerts() {
  const alerts = ref([])
  const unreadCount = computed(() => alerts.value.filter(a => !a.read).length)
  
  const { subscribe, on } = useWebSocket()
  
  onMounted(() => {
    subscribe('alert', handleAlert)
    on(MessageType.ALERT, handleAlert)
  })
  
  function handleAlert(message) {
    alerts.value.unshift({
      ...message.data,
      id: message.data.id || Date.now().toString(),
      read: false,
      receivedAt: Date.now()
    })
    
    // 只保留最近 50 条
    if (alerts.value.length > 50) {
      alerts.value = alerts.value.slice(0, 50)
    }
  }
  
  function markAsRead(alertId) {
    const alert = alerts.value.find(a => a.id === alertId)
    if (alert) {
      alert.read = true
    }
  }
  
  function markAllAsRead() {
    alerts.value.forEach(a => a.read = true)
  }
  
  function clearAlerts() {
    alerts.value = []
  }
  
  return {
    alerts,
    unreadCount,
    markAsRead,
    markAllAsRead,
    clearAlerts
  }
}

/**
 * 统计数据订阅
 */
export function useStats() {
  const stats = ref({})
  const lastUpdate = ref(null)
  
  const { subscribe, on } = useWebSocket()
  
  onMounted(() => {
    subscribe('stats', handleStats)
    on(MessageType.STATS_UPDATE, handleStats)
  })
  
  function handleStats(message) {
    stats.value = { ...stats.value, ...message.data }
    lastUpdate.value = Date.now()
  }
  
  return {
    stats,
    lastUpdate
  }
}

export { ConnectionState, MessageType }
