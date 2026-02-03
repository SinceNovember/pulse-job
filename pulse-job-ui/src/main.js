import { createApp } from 'vue'
import App from './App.vue'
import './assets/styles/main.css'

// Naive UI 全局配置
import { 
  create,
  NConfigProvider,
  NMessageProvider,
  NDialogProvider,
  NNotificationProvider,
  NLoadingBarProvider
} from 'naive-ui'

// 导入自定义主题
import themeOverrides from './theme'

// 导入并初始化 WebSocket 服务
import { createWebSocketService } from './websocket'

// 创建 WebSocket 服务实例
const wsService = createWebSocketService({
  // WebSocket 服务器地址（根据环境配置）
  url: import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/pulse-job',
  
  // 开发环境开启调试模式
  debug: import.meta.env.DEV,
  
  // 重连配置
  reconnect: true,
  reconnectInterval: 1000,
  reconnectMaxInterval: 30000,
  reconnectMaxAttempts: -1,
  
  // 心跳配置
  heartbeatEnabled: true,
  heartbeatInterval: 25000,
  heartbeatTimeout: 10000,
  
  // 客户端类型
  clientType: 'browser'
})

// 自动连接 WebSocket（可选，也可以手动调用 connect）
// wsService.connect()

const app = createApp(App)

// 将 WebSocket 服务注入到全局属性
app.config.globalProperties.$ws = wsService

// 如果需要全局注册少量组件，可以使用 create
// const naive = create({
//   components: [NButton, NInput]
// })
// app.use(naive)

app.mount('#app')

// 导出主题配置和 WebSocket 服务供组件使用
export { themeOverrides, wsService }
