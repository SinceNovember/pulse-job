<template>
  <div class="app-container" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <Sidebar 
      :collapsed="sidebarCollapsed" 
      :active-nav="activeNav"
      @nav-click="handleNavClick"
    />
    
    <main class="main-content">
      <Header 
        :sidebar-collapsed="sidebarCollapsed"
        :page-title="pageTitle"
        @toggle-sidebar="toggleSidebar"
      />
      
      <div class="content-body">
        <!-- Dashboard 页面 (Naive UI 组件版) -->
        <Dashboard v-if="activeNav === 'dashboard'" />
        
        <!-- Dashboard 页面 (原版 HTML/CSS) -->
        <DashboardOriginal v-else-if="activeNav === 'dashboard-original'" />
        
        <!-- 任务管理页面 -->
        <TaskManagement v-else-if="activeNav === 'task-management'" />

        <!-- 调度系统页面 -->
        <JobInfoList v-else-if="activeNav === 'job-info'" />
        <JobExecutorList v-else-if="activeNav === 'job-executor'" />
        <JobInstanceList v-else-if="activeNav === 'job-instance'" />
        <JobLogList v-else-if="activeNav === 'job-log'" />
        
        <!-- 任务页面 -->
        <template v-else-if="activeNav === 'tasks'">
          <FilterGroup 
            :active-filter="activeFilter"
            @filter-change="handleFilterChange"
          />
          
          <TaskInput @add-task="addTask" />
          
          <TaskList 
            :tasks="filteredTasks"
            @toggle-task="toggleTask"
          />
        </template>
        
        <!-- 其他页面占位 -->
        <div v-else class="coming-soon">
          <div class="coming-soon-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <h2>{{ pageTitle }}</h2>
          <p>该功能正在开发中，敬请期待...</p>
        </div>
        
        <!-- Naive UI 浮动按钮 -->
        <n-float-button
          position="fixed"
          :right="32"
          :bottom="32"
          type="primary"
          @click="scrollToTop"
        >
          <n-icon>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
          </n-icon>
        </n-float-button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import FilterGroup from './components/FilterGroup.vue'
import TaskInput from './components/TaskInput.vue'
import TaskList from './components/TaskList.vue'
import Dashboard from './components/Dashboard.vue'
import DashboardOriginal from './components/DashboardOriginal.vue'
import TaskManagement from './components/TaskManagement.vue'
import JobInfoList from './components/JobInfoList.vue'
import JobExecutorList from './components/JobExecutorList.vue'
import JobInstanceList from './components/JobInstanceList.vue'
import JobLogList from './components/JobLogList.vue'

// Naive UI 消息
const message = useMessage()

// 侧边栏折叠状态
const sidebarCollapsed = ref(false)

// 当前选中的导航
const activeNav = ref('dashboard')

// 页面标题映射
const pageTitles = {
  dashboard: '仪表板',
  'dashboard-original': '仪表板(原版)',
  projects: '项目',
  'task-management': '任务管理',
  'job-info': '任务配置',
  'job-executor': '执行器管理',
  'job-instance': '执行记录',
  'job-log': '执行日志',
  tasks: '任务',
  kanban: '看板',
  calendar: '日历',
  contacts: '联系人',
  messages: '消息',
  products: '产品',
  invoices: '发票',
  files: '文件',
  notifications: '告警通知',
  reports: '统计报表',
  help: '帮助中心'
}

// 计算页面标题
const pageTitle = computed(() => pageTitles[activeNav.value] || '页面')

// 当前筛选
const activeFilter = ref('all')

// 任务数据
const tasks = ref([
  { id: 1, title: '预算和合同', completed: true, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'completed' },
  { id: 2, title: '搜索 UI 工具包', completed: true, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'completed' },
  { id: 3, title: '设计新的仪表盘', completed: true, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'completed' },
  { id: 4, title: '设计搜索页面', completed: false, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'pending' },
  { id: 5, title: '准备 HTML 和 CSS', completed: false, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'pending' },
  { id: 6, title: '修复问题', completed: false, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'waiting' },
  { id: 7, title: '预算和合同', completed: false, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'waiting' },
  { id: 8, title: '搜索 UI 工具包', completed: false, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'waiting' },
  { id: 9, title: '搜索 UI 工具包', completed: false, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'waiting' },
  { id: 10, title: '预算和合同', completed: false, subtasks: { current: 0, total: 3 }, attachments: 7, comments: 5, status: 'waiting' }
])

// 筛选后的任务
const filteredTasks = computed(() => {
  if (activeFilter.value === 'completed') {
    return tasks.value.filter(task => task.completed)
  } else if (activeFilter.value === 'tracking') {
    return tasks.value.filter(task => !task.completed)
  }
  return tasks.value
})

// 方法
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebarCollapsed', sidebarCollapsed.value)
}

const handleNavClick = (navId) => {
  activeNav.value = navId
}

const handleFilterChange = (filterId) => {
  activeFilter.value = filterId
}

let taskIdCounter = 100

const addTask = (title) => {
  const newTask = {
    id: taskIdCounter++,
    title,
    completed: false,
    subtasks: { current: 0, total: 3 },
    attachments: 0,
    comments: 0,
    status: 'pending'
  }
  tasks.value.unshift(newTask)
  message.success('任务添加成功')
}

const toggleTask = (taskId) => {
  const task = tasks.value.find(t => t.id === taskId)
  if (task) {
    task.completed = !task.completed
    task.status = task.completed ? 'completed' : 'pending'
    message.info(task.completed ? '任务已完成' : '任务已恢复')
  }
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 初始化
onMounted(() => {
  const saved = localStorage.getItem('sidebarCollapsed')
  if (saved === 'true') {
    sidebarCollapsed.value = true
  }
})
</script>

<style scoped>
/* 开发中页面占位 */
.coming-soon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  color: var(--text-secondary);
}

.coming-soon-icon {
  width: 80px;
  height: 80px;
  background: var(--bg-card);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.coming-soon-icon svg {
  width: 40px;
  height: 40px;
  color: var(--text-muted);
}

.coming-soon h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.coming-soon p {
  font-size: 0.9375rem;
  margin: 0;
}
</style>
