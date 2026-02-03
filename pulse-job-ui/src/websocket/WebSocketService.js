/**
 * PulseJob WebSocket 服务
 * 
 * 生产级 WebSocket 客户端，提供：
 * - 自动重连（指数退避）
 * - 心跳检测
 * - 消息订阅/发布
 * - 连接状态管理
 * - 消息队列（离线缓存）
 * 
 * @author PulseJob
 */

// 消息类型枚举
export const MessageType = {
  // 系统消息
  PING: 'PING',
  PONG: 'PONG',
  CONNECT: 'CONNECT',
  DISCONNECT: 'DISCONNECT',
  ERROR: 'ERROR',
  ACK: 'ACK',

  // 订阅消息
  SUBSCRIBE: 'SUBSCRIBE',
  UNSUBSCRIBE: 'UNSUBSCRIBE',
  SUBSCRIBED: 'SUBSCRIBED',
  UNSUBSCRIBED: 'UNSUBSCRIBED',

  // 执行器状态
  EXECUTOR_STATUS: 'EXECUTOR_STATUS',
  EXECUTOR_ONLINE: 'EXECUTOR_ONLINE',
  EXECUTOR_OFFLINE: 'EXECUTOR_OFFLINE',
  EXECUTOR_HEARTBEAT: 'EXECUTOR_HEARTBEAT',

  // 任务相关
  TASK_STATUS: 'TASK_STATUS',
  TASK_TRIGGERED: 'TASK_TRIGGERED',
  TASK_COMPLETED: 'TASK_COMPLETED',
  TASK_FAILED: 'TASK_FAILED',

  // 日志相关
  LOG_STREAM: 'LOG_STREAM',
  LOG_APPEND: 'LOG_APPEND',
  LOG_END: 'LOG_END',

  // 告警相关
  ALERT: 'ALERT',

  // 统计相关
  STATS_UPDATE: 'STATS_UPDATE',

  // 通用消息
  BROADCAST: 'BROADCAST',
  NOTIFICATION: 'NOTIFICATION'
}

// 连接状态枚举
export const ConnectionState = {
  DISCONNECTED: 'DISCONNECTED',
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  RECONNECTING: 'RECONNECTING',
  CLOSED: 'CLOSED'
}

/**
 * WebSocket 配置
 */
const DEFAULT_CONFIG = {
  // 服务器地址
  url: 'ws://localhost:8080/ws/pulse-job',
  
  // 重连配置
  reconnect: true,
  reconnectInterval: 1000,      // 初始重连间隔（毫秒）
  reconnectMaxInterval: 30000,  // 最大重连间隔（毫秒）
  reconnectMaxAttempts: -1,     // 最大重连次数，-1 为无限
  reconnectBackoffRate: 1.5,    // 退避倍率
  
  // 心跳配置
  heartbeatEnabled: true,
  heartbeatInterval: 25000,     // 心跳间隔（毫秒）
  heartbeatTimeout: 10000,      // 心跳超时（毫秒）
  
  // 消息队列配置
  queueEnabled: true,           // 启用离线消息队列
  queueMaxSize: 100,            // 队列最大长度
  
  // 其他配置
  debug: false,                 // 调试模式
  clientType: 'browser'         // 客户端类型
}

class WebSocketService {
  constructor(config = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config }
    this.ws = null
    this.state = ConnectionState.DISCONNECTED
    this.clientId = this._generateClientId()
    
    // 重连相关
    this.reconnectAttempts = 0
    this.reconnectTimer = null
    this.currentReconnectInterval = this.config.reconnectInterval
    
    // 心跳相关
    this.heartbeatTimer = null
    this.heartbeatTimeoutTimer = null
    this.lastPongTime = 0
    
    // 消息队列（离线缓存）
    this.messageQueue = []
    
    // 订阅管理
    this.subscriptions = new Map()  // topic -> Set<callback>
    
    // 事件监听器
    this.eventListeners = new Map() // event -> Set<callback>
    
    // 消息处理器
    this.messageHandlers = new Map() // type -> handler
    
    // 注册内置消息处理器
    this._registerBuiltinHandlers()
  }

  /**
   * 连接 WebSocket
   */
  connect() {
    if (this.state === ConnectionState.CONNECTED || 
        this.state === ConnectionState.CONNECTING) {
      this._log('Already connected or connecting')
      return
    }

    this._setState(ConnectionState.CONNECTING)
    
    const url = this._buildUrl()
    this._log('Connecting to:', url)

    try {
      this.ws = new WebSocket(url)
      this._setupEventHandlers()
    } catch (error) {
      this._log('Connection error:', error)
      this._handleConnectionError(error)
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this._log('Disconnecting...')
    this._stopHeartbeat()
    this._stopReconnect()
    
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect')
      this.ws = null
    }
    
    this._setState(ConnectionState.CLOSED)
  }

  /**
   * 发送消息
   */
  send(type, data, topic = null) {
    const message = {
      type,
      topic,
      data,
      timestamp: Date.now()
    }

    if (this.state !== ConnectionState.CONNECTED) {
      if (this.config.queueEnabled) {
        this._enqueueMessage(message)
        this._log('Message queued (offline):', type)
      }
      return false
    }

    return this._sendRaw(message)
  }

  /**
   * 订阅主题
   */
  subscribe(topic, callback) {
    if (!this.subscriptions.has(topic)) {
      this.subscriptions.set(topic, new Set())
      
      // 发送订阅请求到服务器
      if (this.state === ConnectionState.CONNECTED) {
        this.send(MessageType.SUBSCRIBE, null, topic)
      }
    }
    
    this.subscriptions.get(topic).add(callback)
    this._log('Subscribed to topic:', topic)
    
    // 返回取消订阅函数
    return () => this.unsubscribe(topic, callback)
  }

  /**
   * 取消订阅
   */
  unsubscribe(topic, callback) {
    const callbacks = this.subscriptions.get(topic)
    if (callbacks) {
      if (callback) {
        callbacks.delete(callback)
        if (callbacks.size === 0) {
          this.subscriptions.delete(topic)
          // 发送取消订阅请求
          if (this.state === ConnectionState.CONNECTED) {
            this.send(MessageType.UNSUBSCRIBE, null, topic)
          }
        }
      } else {
        this.subscriptions.delete(topic)
        if (this.state === ConnectionState.CONNECTED) {
          this.send(MessageType.UNSUBSCRIBE, null, topic)
        }
      }
    }
  }

  /**
   * 添加事件监听器
   */
  on(event, callback) {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, new Set())
    }
    this.eventListeners.get(event).add(callback)
    
    return () => this.off(event, callback)
  }

  /**
   * 移除事件监听器
   */
  off(event, callback) {
    const listeners = this.eventListeners.get(event)
    if (listeners) {
      if (callback) {
        listeners.delete(callback)
      } else {
        this.eventListeners.delete(event)
      }
    }
  }

  /**
   * 注册消息类型处理器
   */
  registerHandler(type, handler) {
    this.messageHandlers.set(type, handler)
  }

  /**
   * 获取连接状态
   */
  getState() {
    return this.state
  }

  /**
   * 检查是否已连接
   */
  isConnected() {
    return this.state === ConnectionState.CONNECTED
  }

  /**
   * 获取客户端ID
   */
  getClientId() {
    return this.clientId
  }

  // ==================== 私有方法 ====================

  _buildUrl() {
    const params = new URLSearchParams({
      clientId: this.clientId,
      clientType: this.config.clientType
    })
    return `${this.config.url}?${params.toString()}`
  }

  _setupEventHandlers() {
    this.ws.onopen = (event) => this._handleOpen(event)
    this.ws.onclose = (event) => this._handleClose(event)
    this.ws.onerror = (event) => this._handleError(event)
    this.ws.onmessage = (event) => this._handleMessage(event)
  }

  _handleOpen(event) {
    this._log('Connection opened')
    this._setState(ConnectionState.CONNECTED)
    this.reconnectAttempts = 0
    this.currentReconnectInterval = this.config.reconnectInterval
    
    // 启动心跳
    this._startHeartbeat()
    
    // 重新订阅所有主题
    this._resubscribeAll()
    
    // 发送队列中的消息
    this._flushMessageQueue()
    
    // 触发事件
    this._emit('open', event)
    this._emit('connected', { clientId: this.clientId })
  }

  _handleClose(event) {
    this._log('Connection closed:', event.code, event.reason)
    this._stopHeartbeat()
    
    const wasConnected = this.state === ConnectionState.CONNECTED
    
    if (event.code === 1000) {
      // 正常关闭
      this._setState(ConnectionState.CLOSED)
    } else if (this.config.reconnect && this.state !== ConnectionState.CLOSED) {
      // 异常关闭，尝试重连
      this._setState(ConnectionState.RECONNECTING)
      this._scheduleReconnect()
    } else {
      this._setState(ConnectionState.DISCONNECTED)
    }
    
    this._emit('close', event)
    if (wasConnected) {
      this._emit('disconnected', { code: event.code, reason: event.reason })
    }
  }

  _handleError(event) {
    this._log('Connection error:', event)
    this._emit('error', event)
  }

  _handleConnectionError(error) {
    this._setState(ConnectionState.DISCONNECTED)
    if (this.config.reconnect) {
      this._scheduleReconnect()
    }
  }

  _handleMessage(event) {
    try {
      const message = JSON.parse(event.data)
      this._log('Message received:', message.type)
      
      // 更新活动时间
      this.lastActivityTime = Date.now()
      
      // 处理内置消息类型
      const handler = this.messageHandlers.get(message.type)
      if (handler) {
        handler(message)
      }
      
      // 分发到订阅者
      if (message.topic) {
        const callbacks = this.subscriptions.get(message.topic)
        if (callbacks) {
          callbacks.forEach(cb => {
            try {
              cb(message)
            } catch (e) {
              console.error('Subscription callback error:', e)
            }
          })
        }
      }
      
      // 触发消息事件
      this._emit('message', message)
      this._emit(message.type, message)
      
    } catch (error) {
      this._log('Failed to parse message:', error)
    }
  }

  _registerBuiltinHandlers() {
    // 心跳响应
    this.messageHandlers.set(MessageType.PING, (message) => {
      this._sendRaw({
        type: MessageType.PONG,
        data: message.data
      })
    })

    // 心跳响应确认
    this.messageHandlers.set(MessageType.PONG, (message) => {
      this.lastPongTime = Date.now()
      this._clearHeartbeatTimeout()
      this._log('Heartbeat pong received, latency:', Date.now() - message.data, 'ms')
    })

    // 连接成功
    this.messageHandlers.set(MessageType.CONNECT, (message) => {
      this._log('Connection confirmed:', message.data)
    })

    // 订阅成功
    this.messageHandlers.set(MessageType.SUBSCRIBED, (message) => {
      this._log('Subscription confirmed:', message.topic)
    })

    // 错误消息
    this.messageHandlers.set(MessageType.ERROR, (message) => {
      console.error('Server error:', message.errorCode, message.errorMessage)
      this._emit('serverError', message)
    })
  }

  // ==================== 心跳相关 ====================

  _startHeartbeat() {
    if (!this.config.heartbeatEnabled) return
    
    this._stopHeartbeat()
    
    this.heartbeatTimer = setInterval(() => {
      this._sendHeartbeat()
    }, this.config.heartbeatInterval)
    
    this._log('Heartbeat started, interval:', this.config.heartbeatInterval)
  }

  _stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
    this._clearHeartbeatTimeout()
  }

  _sendHeartbeat() {
    if (this.state !== ConnectionState.CONNECTED) return
    
    const timestamp = Date.now()
    this._sendRaw({
      type: MessageType.PING,
      data: timestamp
    })
    
    // 设置心跳超时
    this._setHeartbeatTimeout()
  }

  _setHeartbeatTimeout() {
    this._clearHeartbeatTimeout()
    
    this.heartbeatTimeoutTimer = setTimeout(() => {
      this._log('Heartbeat timeout, reconnecting...')
      this._handleHeartbeatTimeout()
    }, this.config.heartbeatTimeout)
  }

  _clearHeartbeatTimeout() {
    if (this.heartbeatTimeoutTimer) {
      clearTimeout(this.heartbeatTimeoutTimer)
      this.heartbeatTimeoutTimer = null
    }
  }

  _handleHeartbeatTimeout() {
    // 心跳超时，关闭连接并重连
    if (this.ws) {
      this.ws.close(4000, 'Heartbeat timeout')
    }
  }

  // ==================== 重连相关 ====================

  _scheduleReconnect() {
    if (!this.config.reconnect) return
    
    if (this.config.reconnectMaxAttempts !== -1 && 
        this.reconnectAttempts >= this.config.reconnectMaxAttempts) {
      this._log('Max reconnect attempts reached')
      this._setState(ConnectionState.DISCONNECTED)
      this._emit('reconnectFailed', { attempts: this.reconnectAttempts })
      return
    }

    this.reconnectAttempts++
    
    this._log(`Scheduling reconnect attempt ${this.reconnectAttempts} in ${this.currentReconnectInterval}ms`)
    
    this._emit('reconnecting', { 
      attempt: this.reconnectAttempts, 
      delay: this.currentReconnectInterval 
    })

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, this.currentReconnectInterval)

    // 指数退避
    this.currentReconnectInterval = Math.min(
      this.currentReconnectInterval * this.config.reconnectBackoffRate,
      this.config.reconnectMaxInterval
    )
  }

  _stopReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  // ==================== 消息队列 ====================

  _enqueueMessage(message) {
    if (this.messageQueue.length >= this.config.queueMaxSize) {
      this.messageQueue.shift() // 移除最早的消息
    }
    this.messageQueue.push(message)
  }

  _flushMessageQueue() {
    if (this.messageQueue.length === 0) return
    
    this._log(`Flushing ${this.messageQueue.length} queued messages`)
    
    const queue = [...this.messageQueue]
    this.messageQueue = []
    
    queue.forEach(message => {
      this._sendRaw(message)
    })
  }

  // ==================== 订阅管理 ====================

  _resubscribeAll() {
    if (this.subscriptions.size === 0) return
    
    this._log(`Resubscribing to ${this.subscriptions.size} topics`)
    
    this.subscriptions.forEach((_, topic) => {
      this.send(MessageType.SUBSCRIBE, null, topic)
    })
  }

  // ==================== 工具方法 ====================

  _sendRaw(message) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return false
    }
    
    try {
      this.ws.send(JSON.stringify(message))
      return true
    } catch (error) {
      this._log('Send error:', error)
      return false
    }
  }

  _setState(state) {
    const oldState = this.state
    this.state = state
    
    if (oldState !== state) {
      this._emit('stateChange', { oldState, newState: state })
    }
  }

  _emit(event, data) {
    const listeners = this.eventListeners.get(event)
    if (listeners) {
      listeners.forEach(cb => {
        try {
          cb(data)
        } catch (e) {
          console.error('Event listener error:', e)
        }
      })
    }
  }

  _log(...args) {
    if (this.config.debug) {
      console.log('[WebSocket]', ...args)
    }
  }

  _generateClientId() {
    return 'browser_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }
}

// 创建单例实例
let instance = null

export function createWebSocketService(config) {
  instance = new WebSocketService(config)
  return instance
}

export function getWebSocketService() {
  if (!instance) {
    instance = new WebSocketService()
  }
  return instance
}

export default WebSocketService
