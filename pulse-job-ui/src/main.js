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

const app = createApp(App)

// 如果需要全局注册少量组件，可以使用 create
// const naive = create({
//   components: [NButton, NInput]
// })
// app.use(naive)

app.mount('#app')

// 导出主题配置供组件使用
export { themeOverrides }
