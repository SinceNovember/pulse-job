<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <a href="#" class="logo">
        <div class="logo-icon">
          <svg width="30" height="30" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path opacity="0.25" d="M14.684 25.388C20.3284 25.388 24.904 20.8123 24.904 15.168C24.904 9.52365 20.3284 4.948 14.684 4.948C9.03965 4.948 4.464 9.52365 4.464 15.168C4.464 20.8123 9.03965 25.388 14.684 25.388Z" fill="#5E81F4"/>
            <path opacity="0.5" d="M6.292 13.272C3.74884 13.2711 1.45629 11.7393 0.482133 9.39014C-0.492025 7.04096 0.0437846 4.33633 1.84 2.53598C4.29692 0.080291 8.27908 0.080291 10.736 2.53598C11.9163 3.71535 12.5794 5.31546 12.5794 6.98398C12.5794 8.6525 11.9163 10.2526 10.736 11.432C9.56032 12.6149 7.95978 13.2776 6.292 13.272Z" fill="#5E81F4"/>
            <path opacity="0.5" d="M23.308 29.8959C20.3057 29.897 17.7208 27.7767 17.1348 24.8321C16.5488 21.8875 18.1248 18.9391 20.8988 17.7905C23.6728 16.642 26.8717 17.6133 28.5388 20.1104C30.2058 22.6074 29.8764 25.9343 27.752 28.0559C26.5753 29.2373 24.9754 29.8997 23.308 29.8959Z" fill="#5E81F4"/>
            <path d="M6.46 29.828C3.91539 29.8303 1.61987 28.2997 0.643664 25.9498C-0.332546 23.5999 0.202755 20.8934 2 19.092C2.27475 18.8241 2.57332 18.5818 2.892 18.368L3.2 18.172C3.416 18.044 3.64 17.928 3.876 17.816C3.996 17.764 4.116 17.708 4.248 17.66C4.49358 17.5703 4.74479 17.4968 5 17.44L5.16 17.396C5.18744 17.3861 5.2155 17.3781 5.244 17.372L6.668 17.268C6.852 17.268 7.068 17.304 7.264 17.332H7.308H7.496H7.556H7.616H8C10.4617 17.3091 12.8115 16.3006 14.524 14.532C16.2459 12.7768 17.1888 10.4023 17.14 7.94398V7.87598V7.80798C17.1259 7.73141 17.1178 7.65383 17.116 7.57598V7.38398L17.064 7.19998C17.064 7.08798 17.036 6.96798 17.032 6.85198L17.128 5.53198V5.49998V5.47598C17.128 5.44398 17.188 5.26798 17.188 5.26798C17.2488 4.9968 17.3263 4.72962 17.42 4.46798C17.464 4.34798 17.52 4.22798 17.576 4.10798C17.681 3.87112 17.8013 3.6413 17.936 3.41998V3.38398C17.992 3.29198 18.044 3.19998 18.108 3.11198C18.3181 2.7953 18.5579 2.49931 18.824 2.22798C21.285 -0.237417 25.2786 -0.240999 27.744 2.21998C30.2094 4.68096 30.213 8.67458 27.752 11.14C27.4788 11.4056 27.1815 11.6453 26.864 11.856C26.768 11.924 26.684 11.972 26.6 12.024L26.552 12.052C26.32 12.188 26.096 12.308 25.88 12.408C25.752 12.464 25.636 12.52 25.516 12.564C25.2636 12.6556 25.0057 12.7318 24.744 12.792C24.6886 12.8037 24.6339 12.8184 24.58 12.836L24.48 12.864L23.084 12.96C22.908 12.96 22.72 12.928 22.524 12.904H22.436H22.256H22.14H22.076H21.764C16.6622 12.9786 12.5777 17.1579 12.62 22.26V22.328V22.4C12.6308 22.4702 12.6375 22.541 12.64 22.612C12.64 22.688 12.64 22.76 12.66 22.832C12.68 22.904 12.66 22.968 12.684 23.04C12.698 23.1434 12.706 23.2476 12.708 23.352L12.608 24.724C12.6005 24.7604 12.5912 24.7965 12.58 24.832L12.544 24.956C12.4868 25.2276 12.4106 25.495 12.316 25.756C12.272 25.876 12.212 26 12.156 26.124C12.0543 26.355 11.9408 26.5806 11.816 26.8C11.752 26.912 11.692 27.012 11.624 27.108C11.4121 27.428 11.1696 27.7267 10.9 28C9.72428 29.1804 8.12605 29.8427 6.46 29.84V29.828Z" fill="#5E81F4"/>
          </svg>
        </div>
        <span class="logo-text">Pulse Job</span>
      </a>
    </div>

    <nav class="sidebar-nav">
      <ul class="nav-list">
        <li class="nav-item" :class="{ active: activeNav === 'dashboard' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'dashboard')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
            </svg>
            <span>仪表板</span>
          </a>
        </li>
        <li class="nav-item" :class="{ active: activeNav === 'dashboard-original' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'dashboard-original')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
              <rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
            </svg>
            <span>仪表板(原版)</span>
          </a>
        </li>
        <li class="nav-item" :class="{ active: activeNav === 'projects' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'projects')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
            </svg>
            <span>项目</span>
          </a>
        </li>
        <!-- 调度系统分组 -->
        <li class="nav-section">调度管理</li>
        <li class="nav-item" :class="{ active: activeNav === 'task-management' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'task-management')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/>
            </svg>
            <span>任务管理</span>
          </a>
        </li>
        <li class="nav-item" :class="{ active: activeNav === 'job-executor' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'job-executor')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/>
              <path d="M6 6h.01"/><path d="M6 18h.01"/>
            </svg>
            <span>执行器管理</span>
          </a>
        </li>
        <li class="nav-item" :class="{ active: activeNav === 'job-instance' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'job-instance')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
            </svg>
            <span>执行记录</span>
          </a>
        </li>
        <li class="nav-item" :class="{ active: activeNav === 'job-log' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'job-log')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
              <path d="M14 2v6h6"/><path d="M16 13H8"/><path d="M16 17H8"/><path d="M10 9H8"/>
            </svg>
            <span>执行日志</span>
          </a>
        </li>
        
        <!-- 其他功能分组 -->
        <li class="nav-section">系统功能</li>
        <li class="nav-item" :class="{ active: activeNav === 'reports' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'reports')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M18 20V10"/><path d="M12 20V4"/><path d="M6 20v-6"/>
            </svg>
            <span>统计报表</span>
          </a>
        </li>
        <li class="nav-item" :class="{ active: activeNav === 'notifications' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'notifications')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/>
            </svg>
            <span>告警通知</span>
            <span class="nav-badge warning">5</span>
          </a>
        </li>
        <li class="nav-item" :class="{ active: activeNav === 'help' }">
          <a href="#" class="nav-link" @click.prevent="$emit('nav-click', 'help')">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><path d="M12 17h.01"/>
            </svg>
            <span>帮助中心</span>
          </a>
        </li>
      </ul>
    </nav>

    <div class="sidebar-footer">
      <div class="user-section-title">User</div>
      <div class="user-profile">
        <div class="user-avatar">JD</div>
        <div class="user-info">
          <span class="user-name">John Doe</span>
        </div>
        <span class="user-status-indicator"></span>
      </div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  collapsed: Boolean,
  navItems: Array,
  activeNav: String
})

defineEmits(['nav-click'])
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
  transition: width 0.3s ease;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.sidebar-header {
  padding: 16px 28px;
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.logo:hover {
  opacity: 0.85;
}

.logo-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: #5E81F4;
  margin-left: 6px;
  letter-spacing: -0.02em;
  white-space: nowrap;
  overflow: hidden;
  max-width: 150px;
  transition: opacity 0.3s ease, max-width 0.3s ease;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.nav-section {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 16px 14px 8px;
  margin-top: 8px;
}

.nav-section:first-child {
  margin-top: 0;
}

.sidebar-collapsed .nav-section {
  opacity: 0;
  height: 0;
  padding: 0;
  margin: 0;
  overflow: hidden;
}

.nav-item {
  position: relative;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 2px;
  color: var(--text-nav);
  font-size: 0.875rem;
  font-weight: 500;
  transition: color 0.15s ease, background 0.15s ease;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-decoration: none;
}

.nav-link span {
  overflow: hidden;
  white-space: nowrap;
  max-width: 150px;
  transition: opacity 0.3s ease, max-width 0.3s ease;
}

.nav-link:hover {
  color: var(--primary-color);
}

.nav-link:hover .nav-icon {
  color: var(--primary-color);
}

.nav-item.active .nav-link {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.nav-item.active::after {
  content: '';
  position: absolute;
  right: -16px;
  top: 0;
  bottom: 0;
  width: 2px;
  height: 100%;
  background: #5E81F4;
  border-radius: 2px 0 0 2px;
}

.nav-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--text-nav);
  transition: color 0.15s ease;
}

.nav-item.active .nav-icon {
  color: var(--primary-color);
}

.nav-badge {
  margin-left: auto;
  background: var(--primary-color);
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  min-width: 20px;
  text-align: center;
  max-width: 50px;
  overflow: hidden;
  white-space: nowrap;
  transition: opacity 0.3s ease, max-width 0.3s ease, padding 0.3s ease;
}

.nav-badge.warning {
  background: #ff6b6b;
}

.sidebar-footer {
  padding: 16px 20px 20px;
  border-top: 1px solid var(--border-color);
  margin-top: auto;
}

.user-section-title {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-muted);
  margin-bottom: 14px;
  max-height: 30px;
  overflow: hidden;
  transition: opacity 0.3s ease, max-height 0.3s ease, margin 0.3s ease;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  padding: 8px 10px;
  margin: -8px -10px;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.user-profile:hover {
  cursor: pointer;
  background: var(--bg-hover);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5E81F4 0%, #7B9CF5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  font-weight: 600;
  font-size: 0.875rem;
}

.user-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  max-width: 150px;
  transition: opacity 0.3s ease, max-width 0.3s ease;
}

.user-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-primary);
  transition: color 0.15s ease;
}

.user-profile:hover .user-name {
  color: var(--primary-color);
}

.user-status-indicator {
  position: absolute;
  width: 12px;
  height: 12px;
  background: var(--success-color);
  border: 2px solid var(--bg-sidebar);
  border-radius: 50%;
  bottom: 8px;
  left: 38px;
}

/* 折叠状态 */
.sidebar-collapsed .sidebar {
  width: 80px;
}

.sidebar-collapsed .logo-text {
  opacity: 0;
  max-width: 0;
}

.sidebar-collapsed .nav-link span {
  opacity: 0;
  max-width: 0;
}

.sidebar-collapsed .nav-badge {
  opacity: 0;
  max-width: 0;
  padding: 0;
}

.sidebar-collapsed .user-section-title {
  opacity: 0;
  max-height: 0;
  margin-bottom: 0;
}

.sidebar-collapsed .user-info {
  opacity: 0;
  max-width: 0;
}


.sidebar-collapsed .nav-item.active::after {
  right: -16px;
}

</style>
