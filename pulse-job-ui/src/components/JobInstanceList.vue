<template>
  <div class="instance-management">
    <!-- 统计卡片 -->
    <div class="stats-card">
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-icon blue">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.length }}</span>
            <span class="stat-label">总执行次数</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon green">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><path d="M22 4L12 14.01l-3-3"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.filter(i => i.status === 4).length }}</span>
            <span class="stat-label">执行成功</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon red">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.filter(i => i.status === 5).length }}</span>
            <span class="stat-label">执行失败</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon orange">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4l2 2"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ instanceList.filter(i => i.status === 3).length }}</span>
            <span class="stat-label">执行中</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表卡片 -->
    <div class="list-card">
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
              placeholder="搜索实例ID..."
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
        </div>
      </div>

      <!-- 高级筛选面板 -->
      <transition name="slide-down">
        <div v-if="showAdvancedFilter" class="advanced-filter">
          <div class="filter-row">
            <div class="filter-field">
              <label>任务</label>
              <n-select v-model:value="filters.jobId" :options="jobOptions" placeholder="全部任务" clearable size="small" />
            </div>
            <div class="filter-field">
              <label>状态</label>
              <n-select v-model:value="filters.status" :options="statusOptions" placeholder="全部状态" clearable size="small" />
            </div>
            <div class="filter-field">
              <label>触发类型</label>
              <n-select v-model:value="filters.triggerType" :options="triggerTypeOptions" placeholder="全部类型" clearable size="small" />
            </div>
            <div class="filter-field" style="min-width: 240px; max-width: 280px;">
              <label>时间范围</label>
              <n-date-picker 
                v-model:value="filters.dateRange" 
                type="daterange" 
                clearable
                size="small"
                :shortcuts="dateShortcuts"
              />
            </div>
            <div class="filter-field filter-actions">
              <n-button size="small" @click="handleReset">重置</n-button>
              <n-button size="small" type="primary" @click="handleSearch">查询</n-button>
            </div>
          </div>
        </div>
      </transition>

      <!-- 执行记录表格 -->
      <div class="table-section">
        <n-data-table
          :columns="columns"
          :data="filteredInstances"
          :pagination="pagination"
          :row-key="row => row.id"
          class="instance-table"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <transition name="modal">
      <div v-if="showDetail" class="detail-modal-overlay" @click.self="showDetail = false">
        <div class="detail-modal">
          <div class="detail-modal-header">
            <div class="header-left">
              <div class="header-logo">
                <svg width="22" height="22" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path opacity="0.25" d="M14.684 25.388C20.3284 25.388 24.904 20.8123 24.904 15.168C24.904 9.52365 20.3284 4.948 14.684 4.948C9.03965 4.948 4.464 9.52365 4.464 15.168C4.464 20.8123 9.03965 25.388 14.684 25.388Z" fill="#5E81F4"/>
                  <path opacity="0.5" d="M6.292 13.272C3.74884 13.2711 1.45629 11.7393 0.482133 9.39014C-0.492025 7.04096 0.0437846 4.33633 1.84 2.53598C4.29692 0.080291 8.27908 0.080291 10.736 2.53598C11.9163 3.71535 12.5794 5.31546 12.5794 6.98398C12.5794 8.6525 11.9163 10.2526 10.736 11.432C9.56032 12.6149 7.95978 13.2776 6.292 13.272Z" fill="#5E81F4"/>
                  <path opacity="0.5" d="M23.308 29.8959C20.3057 29.897 17.7208 27.7767 17.1348 24.8321C16.5488 21.8875 18.1248 18.9391 20.8988 17.7905C23.6728 16.642 26.8717 17.6133 28.5388 20.1104C30.2058 22.6074 29.8764 25.9343 27.752 28.0559C26.5753 29.2373 24.9754 29.8997 23.308 29.8959Z" fill="#5E81F4"/>
                  <path d="M6.46 29.828C3.91539 29.8303 1.61987 28.2997 0.643664 25.9498C-0.332546 23.5999 0.202755 20.8934 2 19.092C2.27475 18.8241 2.57332 18.5818 2.892 18.368L3.2 18.172C3.416 18.044 3.64 17.928 3.876 17.816C3.996 17.764 4.116 17.708 4.248 17.66C4.49358 17.5703 4.74479 17.4968 5 17.44L5.16 17.396C5.18744 17.3861 5.2155 17.3781 5.244 17.372L6.668 17.268C6.852 17.268 7.068 17.304 7.264 17.332H7.308H7.496H7.556H7.616H8C10.4617 17.3091 12.8115 16.3006 14.524 14.532C16.2459 12.7768 17.1888 10.4023 17.14 7.94398V7.87598V7.80798C17.1259 7.73141 17.1178 7.65383 17.116 7.57598V7.38398L17.064 7.19998C17.064 7.08798 17.036 6.96798 17.032 6.85198L17.128 5.53198V5.49998V5.47598C17.128 5.44398 17.188 5.26798 17.188 5.26798C17.2488 4.9968 17.3263 4.72962 17.42 4.46798C17.464 4.34798 17.52 4.22798 17.576 4.10798C17.681 3.87112 17.8013 3.6413 17.936 3.41998V3.38398C17.992 3.29198 18.044 3.19998 18.108 3.11198C18.3181 2.7953 18.5579 2.49931 18.824 2.22798C21.285 -0.237417 25.2786 -0.240013 27.7428 2.22178C30.207 4.68357 30.2096 8.67716 27.7488 11.1441C27.4763 11.4106 27.1803 11.6522 26.864 11.8659C26.7693 11.9299 26.676 11.9899 26.5813 12.0519C26.36 12.188 26.1293 12.3087 25.892 12.4139C25.772 12.4699 25.652 12.5259 25.532 12.5699C25.2697 12.665 25.0019 12.7425 24.73 12.802L24.538 12.842L24.468 12.858L23.148 12.966C23.0306 12.974 22.9126 12.974 22.7947 12.9667C22.6067 12.958 22.42 12.934 22 12.934H21.94H21.88H21.792H21.504H21.46C21.2605 12.9023 21.0623 12.8613 20.866 12.812C18.2167 12.1374 16.0588 10.1847 15.1213 7.61605C14.1838 5.04743 14.5963 2.18606 16.2213 0" fill="#5E81F4"/>
                </svg>
              </div>
              <span class="header-title">执行详情 #{{ detailInstance?.id }}</span>
            </div>
            <button class="close-btn" @click="showDetail = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div class="detail-modal-body" v-if="detailInstance">
            <n-descriptions :column="2" label-placement="left" bordered size="small">
              <n-descriptions-item label="实例ID">{{ detailInstance.id }}</n-descriptions-item>
              <n-descriptions-item label="任务ID">{{ detailInstance.jobId }}</n-descriptions-item>
              <n-descriptions-item label="任务名称" :span="2">{{ getJobName(detailInstance.jobId) }}</n-descriptions-item>
              <n-descriptions-item label="执行器ID">{{ detailInstance.executorId }}</n-descriptions-item>
              <n-descriptions-item label="执行器地址">
                <n-tag size="small">{{ detailInstance.executorAddress || '-' }}</n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="触发类型">
                <n-tag size="small" :type="getTriggerTypeColor(detailInstance.triggerType)">
                  {{ getTriggerTypeLabel(detailInstance.triggerType) }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="重试次数">{{ detailInstance.retryCount }}</n-descriptions-item>
            </n-descriptions>

            <div class="detail-section">
              <div class="section-title">调度信息</div>
              <n-descriptions :column="2" label-placement="left" bordered size="small">
                <n-descriptions-item label="调度时间">{{ detailInstance.triggerTime }}</n-descriptions-item>
                <n-descriptions-item label="调度结果">
                  <n-tag :type="getScheduleResultType(detailInstance.status)" size="small">
                    {{ getScheduleResultLabel(detailInstance.status) }}
                  </n-tag>
                </n-descriptions-item>
                <n-descriptions-item label="调度备注" :span="2">{{ getScheduleRemark(detailInstance) }}</n-descriptions-item>
              </n-descriptions>
            </div>

            <div class="detail-section">
              <div class="section-title">执行信息</div>
              <n-descriptions :column="2" label-placement="left" bordered size="small">
                <n-descriptions-item label="执行时间">{{ detailInstance.startTime || '-' }}</n-descriptions-item>
                <n-descriptions-item label="结束时间">{{ detailInstance.endTime || '-' }}</n-descriptions-item>
                <n-descriptions-item label="执行耗时">{{ getDuration(detailInstance.startTime, detailInstance.endTime) }}</n-descriptions-item>
                <n-descriptions-item label="执行结果">
                  <n-tag v-if="detailInstance.status >= 3" :type="getStatusType(detailInstance.status)" size="small">
                    {{ getExecResultLabel(detailInstance.status) }}
                  </n-tag>
                  <span v-else style="color: #9ca3af;">-</span>
                </n-descriptions-item>
                <n-descriptions-item label="执行备注" :span="2">{{ getExecRemark(detailInstance) }}</n-descriptions-item>
              </n-descriptions>
            </div>

            <div v-if="detailInstance.result" class="detail-section">
              <div class="section-title">执行结果详情</div>
              <div class="result-code-wrapper">
                <div class="result-code-header">
                  <div class="code-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>
                    </svg>
                  </div>
                  <span class="code-label">JSON</span>
                </div>
                <div class="result-code-body">
                  <n-code :code="formatJson(detailInstance.result)" language="json" />
                </div>
              </div>
            </div>
            
            <div v-if="detailInstance.errorMessage" class="detail-section">
              <div class="section-title">错误信息</div>
              <n-alert type="error" :show-icon="false">{{ detailInstance.errorMessage }}</n-alert>
            </div>
            
            <div v-if="detailInstance.scheduleError" class="detail-section">
              <div class="section-title">调度错误</div>
              <n-alert type="error" :show-icon="false">{{ detailInstance.scheduleError }}</n-alert>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 日志弹窗 -->
    <transition name="modal">
      <div v-if="showLogModal" class="log-modal-overlay" @click.self="showLogModal = false">
        <div class="log-modal" :class="{ fullscreen: isLogFullscreen }">
          <!-- 日志控制台 -->
          <div class="log-console" :class="{ 'light-theme': isLightTheme }">
            <!-- 控制台头部 -->
            <div class="console-header">
              <div class="header-left">
                <div class="header-logo">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                    <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                    <path d="M7 8l4 4-4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <line x1="13" y1="16" x2="17" y2="16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </div>
                <div class="header-title">
                  <span class="title-main">执行日志</span>
                  <span class="title-separator">-</span>
                  <span class="title-sub">{{ getJobName(currentLogInstance?.jobId) }} #{{ currentLogInstance?.id }}</span>
                </div>
              </div>
              <div class="header-actions">
                <button class="action-btn" :class="{ active: showLogHighlight }" @click="showLogHighlight = !showLogHighlight" title="日志高亮">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                    <path d="M2 17l10 5 10-5"/>
                    <path d="M2 12l10 5 10-5"/>
                  </svg>
                </button>
                <button class="action-btn" @click="isLightTheme = !isLightTheme" :title="isLightTheme ? '切换暗色' : '切换亮色'">
                  <svg v-if="isLightTheme" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                    <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
                  </svg>
                </button>
                <button class="action-btn" @click="isLogFullscreen = !isLogFullscreen" :title="isLogFullscreen ? '退出全屏' : '全屏'">
                  <svg v-if="isLogFullscreen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/>
                    <line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/>
                    <line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/>
                  </svg>
                </button>
                <button class="action-btn" @click="handleExportLog" title="下载">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                </button>
                <button class="action-btn" @click="handleRefreshLog" title="刷新">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M23 4v6h-6"/><path d="M1 20v-6h6"/>
                    <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
                  </svg>
                </button>
                <button class="close-btn" @click="showLogModal = false">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 工具栏 -->
            <div class="console-toolbar">
              <div class="filter-group">
                <button 
                  class="filter-tab" 
                  :class="{ active: logFilterLevel === null }"
                  @click="logFilterLevel = null"
                >
                  <span class="tab-label">ALL</span>
                  <span class="tab-count">{{ currentLogList.length }}</span>
                </button>
                <button 
                  class="filter-tab info" 
                  :class="{ active: logFilterLevel === 'INFO' }"
                  @click="logFilterLevel = 'INFO'"
                >
                  <span class="tab-label">INFO</span>
                  <span class="tab-count">{{ currentLogList.filter(l => l.logLevel === 'INFO').length }}</span>
                </button>
                <button 
                  class="filter-tab warn" 
                  :class="{ active: logFilterLevel === 'WARN' }"
                  @click="logFilterLevel = 'WARN'"
                >
                  <span class="tab-label">WARN</span>
                  <span class="tab-count">{{ currentLogList.filter(l => l.logLevel === 'WARN').length }}</span>
                </button>
                <button 
                  class="filter-tab error" 
                  :class="{ active: logFilterLevel === 'ERROR' }"
                  @click="logFilterLevel = 'ERROR'"
                >
                  <span class="tab-label">ERROR</span>
                  <span class="tab-count">{{ currentLogList.filter(l => l.logLevel === 'ERROR').length }}</span>
                </button>
              </div>
              <div class="search-box-log">
                <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input 
                  v-model="logSearchKeyword" 
                  type="text" 
                  class="search-input" 
                  placeholder="搜索日志内容..."
                />
                <button v-if="logSearchKeyword" class="search-clear" @click="logSearchKeyword = ''">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 日志内容 -->
            <div class="console-logs">
              <div 
                v-for="(log, index) in filteredLogList" 
                :key="log.id" 
                class="log-row"
                :class="[log.logLevel.toLowerCase(), { 'no-highlight': !showLogHighlight }]"
              >
                <span class="log-line">{{ String(index + 1).padStart(3, ' ') }}</span>
                <span class="log-time">{{ log.createTime }}</span>
<span class="log-level" :class="log.logLevel.toLowerCase()">
                              <span class="level-dot"></span>
                              {{ log.logLevel }}
                            </span>
                <span class="log-thread">[{{ log.thread }}]</span>
                <span class="log-logger">{{ log.logger }}</span>
                <span class="log-msg">{{ log.content }}</span>
              </div>
              <div v-if="filteredLogList.length === 0" class="no-logs">
                <div class="empty-illustration">
                  <svg viewBox="0 0 120 120" fill="none">
                    <circle cx="60" cy="60" r="50" stroke="currentColor" stroke-width="2" opacity="0.2"/>
                    <path d="M40 45h40M40 60h30M40 75h35" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.3"/>
                    <circle cx="85" cy="85" r="20" stroke="currentColor" stroke-width="3" opacity="0.4"/>
                    <line x1="100" y1="100" x2="115" y2="115" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.4"/>
                  </svg>
                </div>
                <span class="empty-title">暂无日志数据</span>
                <span class="empty-desc">日志将在任务执行时自动产生</span>
              </div>
            </div>

            <!-- 底部状态栏 -->
            <div class="console-footer">
              <div class="footer-left">
                <span class="footer-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                    <path d="M14 2v6h6"/>
                  </svg>
                  {{ filteredLogList.length }} 条记录
                </span>
              </div>
              <div class="footer-right">
                <span class="footer-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                  </svg>
                  最后更新: {{ lastLogUpdateTime }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h } from 'vue'
import { NButton, NTag, NTooltip, useMessage } from 'naive-ui'

const message = useMessage()

// 搜索关键词
const searchKeyword = ref('')

// 高级筛选
const showAdvancedFilter = ref(false)
const filters = reactive({
  jobId: null,
  status: null,
  triggerType: null,
  dateRange: null
})

// 详情抽屉
const showDetail = ref(false)
const detailInstance = ref(null)

// 日志弹窗
const showLogModal = ref(false)
const isLogFullscreen = ref(false)
const isLightTheme = ref(true)
const showLogHighlight = ref(true)
const currentLogInstance = ref(null)
const logFilterLevel = ref(null)
const logSearchKeyword = ref('')
const lastLogUpdateTime = ref('13:26:49')

// 计算激活的筛选条件数量
const activeFilterCount = computed(() => {
  let count = 0
  if (filters.jobId) count++
  if (filters.status !== null) count++
  if (filters.triggerType) count++
  if (filters.dateRange) count++
  return count
})

// 选项
const jobOptions = [
  { label: 'dataSyncHandler', value: 1 },
  { label: 'reportGenHandler', value: 2 },
  { label: 'emailSendHandler', value: 3 }
]

const statusOptions = [
  { label: '待执行', value: 0 },
  { label: '已发送', value: 1 },
  { label: '发送失败', value: 2 },
  { label: '执行中', value: 3 },
  { label: '成功', value: 4 },
  { label: '失败', value: 5 },
  { label: '超时', value: 6 }
]

const triggerTypeOptions = [
  { label: '自动调度', value: 'auto' },
  { label: '手动触发', value: 'manual' },
  { label: 'API调用', value: 'api' }
]

const dateShortcuts = {
  '今天': () => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return [today.getTime(), Date.now()]
  },
  '最近7天': () => {
    const end = Date.now()
    const start = end - 7 * 24 * 60 * 60 * 1000
    return [start, end]
  },
  '最近30天': () => {
    const end = Date.now()
    const start = end - 30 * 24 * 60 * 60 * 1000
    return [start, end]
  }
}

// 执行记录列表
const instanceList = ref([
  {
    id: 10001,
    jobId: 1,
    executorId: 1,
    triggerTime: '2026-01-26 02:00:00',
    startTime: '2026-01-26 02:00:01',
    endTime: '2026-01-26 02:00:05',
    status: 4,
    retryCount: 0,
    result: '{"synced": 1024, "duration": "4s"}',
    errorMessage: null,
    triggerType: 'auto',
    executorAddress: '192.168.1.101:9999'
  },
  {
    id: 10002,
    jobId: 2,
    executorId: 1,
    triggerTime: '2026-01-26 08:00:00',
    startTime: '2026-01-26 08:00:01',
    endTime: '2026-01-26 08:00:12',
    status: 4,
    retryCount: 0,
    result: '{"reportId": "RPT-20260126", "size": "2.3MB"}',
    errorMessage: null,
    triggerType: 'auto',
    executorAddress: '192.168.1.102:9999'
  },
  {
    id: 10003,
    jobId: 3,
    executorId: 2,
    triggerTime: '2026-01-26 09:30:00',
    startTime: '2026-01-26 09:30:01',
    endTime: '2026-01-26 09:30:03',
    status: 5,
    retryCount: 2,
    result: null,
    errorMessage: 'SMTP connection timeout',
    triggerType: 'auto',
    executorAddress: '192.168.1.110:9999'
  },
  {
    id: 10004,
    jobId: 1,
    executorId: 1,
    triggerTime: '2026-01-26 11:00:00',
    startTime: '2026-01-26 11:00:01',
    endTime: null,
    status: 3,
    retryCount: 0,
    result: null,
    errorMessage: null,
    triggerType: 'manual',
    executorAddress: '192.168.1.101:9999'
  },
  {
    id: 10005,
    jobId: 1,
    executorId: 1,
    triggerTime: '2026-01-26 10:30:00',
    startTime: '2026-01-26 10:30:01',
    endTime: '2026-01-26 10:35:00',
    status: 6,
    retryCount: 0,
    result: null,
    errorMessage: '任务执行超时',
    triggerType: 'api',
    executorAddress: '192.168.1.103:9999'
  },
  {
    id: 10006,
    jobId: 2,
    executorId: 1,
    triggerTime: '2026-01-26 12:00:00',
    startTime: null,
    endTime: null,
    status: 2,
    retryCount: 0,
    result: null,
    errorMessage: null,
    scheduleError: '执行器离线，无法建立连接',
    triggerType: 'auto',
    executorAddress: '192.168.1.105:9999'
  }
])

// 模拟日志数据
const currentLogList = computed(() => {
  if (!currentLogInstance.value) return []
  const baseTime = currentLogInstance.value.startTime || '2026-01-26 00:00:00'
  return [
    {
      id: 1,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.i.LoggingJobInterceptor',
      content: `[Job-${currentLogInstance.value.jobId}] 开始执行: handler=${getJobName(currentLogInstance.value.jobId)}#execute()`,
      createTime: baseTime + '.001'
    },
    {
      id: 2,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '正在处理数据，共 1000 条记录',
      createTime: baseTime + '.125'
    },
    {
      id: 3,
      logLevel: 'DEBUG',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '连接数据库: jdbc:mysql://localhost:3306/pulse_job',
      createTime: baseTime + '.230'
    },
    {
      id: 4,
      logLevel: 'WARN',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '发现 3 条数据格式异常，已跳过处理',
      createTime: baseTime + '.350'
    },
    {
      id: 5,
      logLevel: 'ERROR',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.DataProcessor',
      content: 'java.lang.NullPointerException: Cannot invoke method on null object at line 128',
      createTime: baseTime + '.380'
    },
    {
      id: 6,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '数据处理完成: 成功 996 条, 失败 1 条, 跳过 3 条',
      createTime: baseTime + '.456'
    },
    {
      id: 7,
      logLevel: 'WARN',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.scheduler.RetryHandler',
      content: '部分任务需要重试，已加入重试队列',
      createTime: baseTime + '.520'
    },
    {
      id: 8,
      logLevel: 'ERROR',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.scheduler.RetryHandler',
      content: '重试失败: 达到最大重试次数 3 次，放弃处理',
      createTime: baseTime + '.600'
    },
    {
      id: 9,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.e.demo.job.Handler',
      content: '所有数据处理完毕',
      createTime: baseTime + '.650'
    },
    {
      id: 10,
      logLevel: 'INFO',
      thread: 'job-worker-1',
      logger: 'c.s.p.c.i.LoggingJobInterceptor',
      content: `[Job-${currentLogInstance.value.jobId}] 执行完成，耗时: 649ms`,
      createTime: (currentLogInstance.value.endTime || baseTime) + '.789'
    }
  ]
})

// 过滤后的日志列表
const filteredLogList = computed(() => {
  return currentLogList.value.filter(log => {
    if (logSearchKeyword.value) {
      const keyword = logSearchKeyword.value.toLowerCase()
      if (!log.content.toLowerCase().includes(keyword) && 
          !log.logger.toLowerCase().includes(keyword)) return false
    }
    if (logFilterLevel.value && log.logLevel !== logFilterLevel.value) {
      return false
    }
    return true
  })
})

// 过滤后的列表
const filteredInstances = computed(() => {
  return instanceList.value.filter(instance => {
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      const matchKeyword = String(instance.id).includes(keyword) ||
             (instance.executorAddress && instance.executorAddress.toLowerCase().includes(keyword))
      if (!matchKeyword) return false
    }
    if (filters.jobId && instance.jobId !== filters.jobId) {
      return false
    }
    if (filters.status !== null && instance.status !== filters.status) {
      return false
    }
    if (filters.triggerType && instance.triggerType !== filters.triggerType) {
      return false
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

// 状态相关
const getStatusLabel = (status) => {
  const map = { 0: '待执行', 1: '已发送', 2: '发送失败', 3: '执行中', 4: '成功', 5: '失败', 6: '超时', 7: '已取消' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 0: 'default', 1: 'info', 2: 'error', 3: 'warning', 4: 'success', 5: 'error', 6: 'warning', 7: 'default' }
  return map[status] || 'default'
}

const getTriggerTypeLabel = (type) => {
  const map = { auto: '自动调度', manual: '手动触发', api: 'API调用' }
  return map[type] || type
}

const getTriggerTypeColor = (type) => {
  const map = { auto: 'success', manual: 'info', api: 'warning' }
  return map[type] || 'default'
}

const getJobName = (jobId) => {
  const map = { 1: 'dataSyncHandler', 2: 'reportGenHandler', 3: 'emailSendHandler' }
  return map[jobId] || `Job-${jobId}`
}

// 计算执行耗时
const getDuration = (startTime, endTime) => {
  if (!startTime) return '-'
  if (!endTime) return '执行中...'
  
  const start = new Date(startTime).getTime()
  const end = new Date(endTime).getTime()
  const diff = end - start
  
  if (diff < 1000) return `${diff}ms`
  if (diff < 60000) return `${(diff / 1000).toFixed(1)}s`
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    const seconds = Math.floor((diff % 60000) / 1000)
    return `${minutes}m ${seconds}s`
  }
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  return `${hours}h ${minutes}m`
}

const formatJson = (str) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

// 表格列
const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { 
    title: '任务名称', 
    key: 'jobName',
    width: 150,
    ellipsis: { tooltip: true },
    render: (row) => h('span', { 
      style: { 
        fontWeight: '500',
        color: 'var(--text-primary)'
      } 
    }, getJobName(row.jobId))
  },
  { title: '调度时间', key: 'triggerTime', width: 155 },
  { 
    title: '调度结果', 
    key: 'scheduleResult',
    width: 85,
    render: (row) => {
      // status 2 是发送失败(调度失败)，其他 >= 1 都是调度成功
      const isSuccess = row.status >= 1 && row.status !== 2
      return h(NTag, { 
        type: isSuccess ? 'success' : 'error', 
        size: 'small' 
      }, () => isSuccess ? '成功' : '失败')
    }
  },
  { title: '执行时间', key: 'startTime', width: 155 },
  { 
    title: '执行结果', 
    key: 'execResult',
    width: 85,
    render: (row) => {
      if (row.status < 3) return h('span', { style: { color: '#9ca3af' } }, '-')
      if (row.status === 3) return h(NTag, { type: 'warning', size: 'small' }, () => '执行中')
      const isSuccess = row.status === 4
      return h(NTag, { 
        type: isSuccess ? 'success' : 'error', 
        size: 'small' 
      }, () => isSuccess ? '成功' : '失败')
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: (row) => h('div', { class: 'action-buttons' }, [
      h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h('button', {
          class: 'action-btn action-btn-info',
          onClick: () => handleViewDetail(row)
        }, [
          h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('circle', { cx: '11', cy: '11', r: '8' }),
            h('line', { x1: '21', y1: '21', x2: '16.65', y2: '16.65' })
          ])
        ]),
        default: () => '详情'
      }),
      h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h('button', {
          class: 'action-btn action-btn-success',
          onClick: () => handleViewLog(row)
        }, [
          h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('rect', { x: '3', y: '3', width: '18', height: '18', rx: '2' }),
            h('path', { d: 'M7 8l4 4-4 4', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }),
            h('line', { x1: '13', y1: '16', x2: '17', y2: '16', 'stroke-linecap': 'round' })
          ])
        ]),
        default: () => '日志'
      })
    ])
  }
]

// 事件处理
const handleSearch = () => {
  pagination.page = 1
  message.success('查询完成')
}

const handleReset = () => {
  searchKeyword.value = ''
  filters.jobId = null
  filters.status = null
  filters.triggerType = null
  filters.dateRange = null
  pagination.page = 1
}


const handleRefresh = () => {
  message.success('刷新成功')
}

const handleViewDetail = (row) => {
  detailInstance.value = row
  showDetail.value = true
}

const handleViewLog = (row) => {
  currentLogInstance.value = row
  logFilterLevel.value = null
  logSearchKeyword.value = ''
  showLogModal.value = true
}

// 调度结果相关函数
const getScheduleResultType = (status) => {
  return (status >= 1 && status !== 2) ? 'success' : 'error'
}

const getScheduleResultLabel = (status) => {
  return (status >= 1 && status !== 2) ? '成功' : '失败'
}

const getScheduleRemark = (row) => {
  const isSuccess = row.status >= 1 && row.status !== 2
  if (isSuccess) {
    return '调度成功，任务已发送至执行器'
  }
  return row.scheduleError || '无法连接执行器，请检查执行器是否在线'
}

// 执行结果相关函数
const getExecResultLabel = (status) => {
  if (status === 3) return '执行中'
  if (status === 4) return '成功'
  return '失败'
}

const getExecRemark = (row) => {
  if (row.status < 3) return '-'
  if (row.status === 3) return '任务正在执行中...'
  if (row.status === 4) return '执行完成'
  return row.errorMessage || '执行异常'
}

const handleRefreshLog = () => {
  message.success('日志刷新成功')
}

const handleExportLog = () => {
  const content = filteredLogList.value.map(log => 
    `${log.createTime} ${log.logLevel.padEnd(5)} [${log.thread}] ${log.logger} ${log.content}`
  ).join('\n')
  
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `pulse-job-logs-${currentLogInstance.value?.id}-${new Date().toISOString().slice(0, 10)}.log`
  a.click()
  URL.revokeObjectURL(url)
  
  message.success('导出成功')
}
</script>

<style scoped>
.instance-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ==================== 统计卡片 ==================== */
.stats-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  border: 1px solid var(--border-color);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-icon.blue { background: rgba(94, 129, 244, 0.1); color: #5E81F4; }
.stat-icon.green { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
.stat-icon.red { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.stat-icon.orange { background: rgba(249, 115, 22, 0.1); color: #f97316; }

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-top: 4px;
}

/* ==================== 列表卡片 ==================== */
.list-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

/* ==================== 工具栏样式 ==================== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
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
  width: 260px;
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

/* ==================== 表格区域 ==================== */
.table-section {
  background: #fff;
  padding: 16px 20px;
}

.instance-table :deep(.n-data-table__pagination) {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.instance-table :deep(.n-pagination-prefix) {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.instance-table :deep(.address-cell) {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  background: transparent;
}



.instance-table :deep(.action-buttons) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.instance-table :deep(.action-btn) {
  background: transparent;
  border: none;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.instance-table :deep(.action-btn svg) {
  width: 16px;
  height: 16px;
}

.instance-table :deep(.action-btn-info) {
  color: var(--text-secondary);
}

.instance-table :deep(.action-btn-info:hover) {
  color: #3b82f6;
}

.instance-table :deep(.action-btn-success) {
  color: var(--text-secondary);
}

.instance-table :deep(.action-btn-success:hover) {
  color: #10b981;
}

/* ==================== 详情抽屉 ==================== */
/* ==================== 详情弹窗 ==================== */
.detail-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.detail-modal {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.15), 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.06);
  width: 680px;
  max-width: 90vw;
  max-height: 85vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: modalIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.detail-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(to bottom, #fafbfc, #fff);
  border-bottom: 1px solid #f0f0f0;
}

.detail-modal-header .header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-modal-header .header-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.9;
}

.detail-modal-header .header-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #555;
  letter-spacing: 0.01em;
}

.detail-modal-header .close-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.detail-modal-header .close-btn:hover {
  background: #f5f5f5;
}

.detail-modal-header .close-btn svg {
  width: 16px;
  height: 16px;
  color: #999;
  transition: color 0.15s ease;
}

.detail-modal-header .close-btn:hover svg {
  color: #666;
}

.detail-modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
  background: #fff;
}

.detail-section {
  margin-top: 20px;
}

.detail-section .section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid #5E81F4;
}

/* 结果代码块样式 */
.result-code-wrapper {
  border-radius: 8px;
  overflow: hidden;
  background: #fafbfc;
  border: 1px solid #e5e7eb;
}

.result-code-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f3f4f6;
  border-bottom: 1px solid #e5e7eb;
}

.result-code-header .code-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5E81F4;
}

.result-code-header .code-icon svg {
  width: 16px;
  height: 16px;
}

.result-code-header .code-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.result-code-body {
  padding: 14px 16px;
  max-height: 200px;
  overflow-y: auto;
}

.result-code-body :deep(.n-code) {
  background: transparent !important;
  font-size: 0.8125rem;
  line-height: 1.6;
}

.result-code-body :deep(pre) {
  margin: 0;
  background: transparent !important;
}

.result-code-body :deep(code) {
  background: transparent !important;
  color: #334155;
}

/* ==================== 日志弹窗 ==================== */
.log-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 40px;
}

.log-modal-overlay:has(.log-modal.fullscreen) {
  padding: 0;
}

.log-modal {
  width: 100%;
  max-width: 1200px;
  height: auto;
  max-height: calc(100vh - 80px);
  transition: all 0.3s ease;
}

.log-modal.fullscreen {
  max-width: 100vw;
  max-height: 100vh;
  width: 100vw;
  height: 100vh;
}

.log-modal.fullscreen .log-console {
  border-radius: 0;
  height: 100%;
  max-height: 100vh;
}

.log-modal.fullscreen .console-logs {
  max-height: calc(100vh - 140px);
  min-height: calc(100vh - 140px);
}

/* 弹窗动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .log-modal,
.modal-leave-to .log-modal {
  transform: scale(0.9);
  opacity: 0;
}

/* ==================== 日志控制台样式 ==================== */
.log-console {
  background: #0c1018;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 
    0 0 0 1px rgba(255, 255, 255, 0.05),
    0 25px 80px -10px rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: calc(100vh - 80px);
}

.console-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(to bottom, #161b26, #12161f);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #58a6ff;
  opacity: 0.9;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.title-main {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #c9d1d9;
  letter-spacing: 0.01em;
}

.title-separator {
  color: #484f58;
  font-weight: 400;
}

.title-sub {
  font-size: 0.875rem;
  color: #8b949e;
  font-weight: 400;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #8b949e;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #c9d1d9;
}

.action-btn.active {
  background: rgba(88, 166, 255, 0.15);
  color: #58a6ff;
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #8b949e;
  margin-left: 8px;
}

.close-btn svg {
  width: 16px;
  height: 16px;
}

.close-btn:hover {
  background: rgba(248, 81, 73, 0.15);
  color: #f85149;
}

/* 工具栏 */
.console-toolbar {
  background: rgba(22, 27, 34, 0.8);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.2);
  padding: 4px;
  border-radius: 10px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: #6e7681;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tab:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #8b949e;
}

.filter-tab.active {
  background: #238636;
  color: white;
  box-shadow: 0 2px 8px rgba(35, 134, 54, 0.3);
}

.filter-tab.warn.active {
  background: #9e6a03;
  box-shadow: 0 2px 8px rgba(158, 106, 3, 0.3);
}

.filter-tab.error.active {
  background: #da3633;
  box-shadow: 0 2px 8px rgba(218, 54, 51, 0.3);
}

.tab-count {
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.filter-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
}

.search-box-log {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  max-width: 320px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 0 14px;
  transition: all 0.2s ease;
}

.search-box-log:focus-within {
  border-color: rgba(255, 255, 255, 0.1);
}

.search-box-log .search-icon {
  color: #484f58;
}

.search-box-log .search-input {
  color: #c9d1d9;
  padding: 10px 0;
  margin-left: 0;
}

.search-box-log .search-input::placeholder {
  color: #484f58;
}

.search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 4px;
  color: #6e7681;
  cursor: pointer;
  transition: all 0.15s ease;
}

.search-clear svg {
  width: 12px;
  height: 12px;
}

.search-clear:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #c9d1d9;
}

/* 日志内容 */
.console-logs {
  padding: 16px 0;
  min-height: 300px;
  max-height: 400px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  flex: 1;
  background: #0c1018;
  color: #c9d1d9;
}

.log-row {
  padding: 10px 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border-left: 3px solid transparent;
  transition: all 0.15s ease;
}

.log-row:hover {
  background: rgba(255, 255, 255, 0.02);
}

.log-row.info { border-left-color: transparent; }
.log-row.warn { border-left-color: #d29922; background: rgba(210, 153, 34, 0.12); }
.log-row.error { border-left-color: #f85149; background: rgba(248, 81, 73, 0.08); }
.log-row.debug { border-left-color: transparent; }

.log-line {
  color: #484f58;
  min-width: 32px;
  text-align: right;
  font-size: 12px;
  user-select: none;
}

.log-time {
  color: #6e7681;
  min-width: 190px;
  flex-shrink: 0;
}

.log-level {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 60px;
  font-weight: 600;
  flex-shrink: 0;
}

.level-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.log-level.info { color: #3fb950; }
.log-level.info .level-dot { background: #3fb950; box-shadow: 0 0 6px rgba(63, 185, 80, 0.5); }
.log-level.warn { color: #d29922; }
.log-level.warn .level-dot { background: #d29922; box-shadow: 0 0 6px rgba(210, 153, 34, 0.5); }
.log-level.error { color: #f85149; }
.log-level.error .level-dot { background: #f85149; box-shadow: 0 0 6px rgba(248, 81, 73, 0.5); }
.log-level.debug { color: #8b949e; }
.log-level.debug .level-dot { background: #8b949e; }

/* 关闭高亮时移除背景色 */
.log-row.no-highlight.warn,
.log-row.no-highlight.error {
  background: transparent !important;
  border-left-color: transparent !important;
}

.log-row.no-highlight.error .log-msg {
  color: #c9d1d9 !important;
}

.log-console.light-theme .log-row.no-highlight.error .log-msg {
  color: #334155 !important;
}

.log-thread {
  color: #a371f7;
  min-width: 130px;
  flex-shrink: 0;
}

.log-logger {
  color: #58a6ff;
  min-width: 260px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-msg {
  color: #c9d1d9;
  flex: 1;
}

.log-row.error .log-msg {
  color: #f85149;
}

/* 底部状态栏 */
.console-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: rgba(22, 27, 34, 0.6);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6e7681;
  font-size: 12px;
}

.footer-item svg {
  width: 14px;
  height: 14px;
}

/* 空状态 */
.no-logs {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  color: #30363d;
}

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #8b949e;
}

.empty-desc {
  font-size: 13px;
  color: #6e7681;
}

/* 滚动条样式 */
.console-logs::-webkit-scrollbar {
  width: 10px;
}

.console-logs::-webkit-scrollbar-track {
  background: transparent;
}

.console-logs::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 5px;
  border: 2px solid transparent;
  background-clip: content-box;
}

.console-logs::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
  background-clip: content-box;
}

/* ==================== 白色主题 ==================== */
/* 亮色主题 - 容器 */
.log-console.light-theme {
  background: #fff;
  box-shadow: 
    0 0 0 1px rgba(0, 0, 0, 0.08),
    0 25px 80px -10px rgba(0, 0, 0, 0.15);
}

/* 亮色主题 - 头部 */
.log-console.light-theme .console-header {
  background: linear-gradient(to bottom, #fafbfc, #fff);
  border-bottom: 1px solid #f0f0f0;
}

.log-console.light-theme .header-logo { color: #5E81F4; }
.log-console.light-theme .title-main { color: #555; }
.log-console.light-theme .title-separator { color: #ccc; }
.log-console.light-theme .title-sub { color: #888; }

.log-console.light-theme .action-btn { color: #999; }
.log-console.light-theme .action-btn:hover { background: #f5f5f5; color: #666; }
.log-console.light-theme .action-btn.active { background: rgba(94, 129, 244, 0.1); color: #5E81F4; }

.log-console.light-theme .close-btn { color: #999; }
.log-console.light-theme .close-btn:hover { background: #f5f5f5; color: #666; }

.log-console.light-theme .console-toolbar {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.log-console.light-theme .filter-group {
  background: white;
  border: 1px solid #e2e8f0;
}

.log-console.light-theme .filter-tab {
  color: #64748b;
}

.log-console.light-theme .filter-tab:hover {
  background: #f1f5f9;
  color: #334155;
}

.log-console.light-theme .filter-tab.active {
  background: #10b981;
}

.log-console.light-theme .filter-tab.warn.active {
  background: #f59e0b;
}

.log-console.light-theme .filter-tab.error.active {
  background: #ef4444;
}

.log-console.light-theme .tab-count {
  background: rgba(0, 0, 0, 0.06);
}

.log-console.light-theme .search-box-log {
  background: white;
  border: 1px solid #e2e8f0;
}

.log-console.light-theme .search-box-log:focus-within {
  border-color: #cbd5e1;
}

.log-console.light-theme .search-box-log .search-icon { color: #94a3b8; }
.log-console.light-theme .search-box-log .search-input { color: #1e293b; }
.log-console.light-theme .search-box-log .search-input::placeholder { color: #94a3b8; }

.log-console.light-theme .console-logs { background: #ffffff; }

.log-console.light-theme .log-row:hover { background: #f8fafc; }
.log-console.light-theme .log-row.warn { background: rgba(245, 158, 11, 0.06); }
.log-console.light-theme .log-row.error { background: rgba(239, 68, 68, 0.06); }

.log-console.light-theme .log-line { color: #cbd5e1; }
.log-console.light-theme .log-time { color: #94a3b8; }
.log-console.light-theme .log-level.info { color: #059669; }
.log-console.light-theme .log-level.info .level-dot { background: #059669; box-shadow: 0 0 6px rgba(5, 150, 105, 0.4); }
.log-console.light-theme .log-level.warn { color: #d97706; }
.log-console.light-theme .log-level.warn .level-dot { background: #d97706; box-shadow: 0 0 6px rgba(217, 119, 6, 0.4); }
.log-console.light-theme .log-level.error { color: #dc2626; }
.log-console.light-theme .log-level.error .level-dot { background: #dc2626; box-shadow: 0 0 6px rgba(220, 38, 38, 0.4); }
.log-console.light-theme .log-level.debug { color: #94a3b8; }
.log-console.light-theme .log-level.debug .level-dot { background: #94a3b8; }

.log-console.light-theme .log-thread { color: #7c3aed; }
.log-console.light-theme .log-logger { color: #2563eb; }
.log-console.light-theme .log-msg { color: #334155; }
.log-console.light-theme .log-row.error .log-msg { color: #dc2626; }

.log-console.light-theme .console-footer {
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.log-console.light-theme .footer-item { color: #64748b; }

.log-console.light-theme .empty-illustration { color: #e2e8f0; }
.log-console.light-theme .empty-title { color: #64748b; }
.log-console.light-theme .empty-desc { color: #94a3b8; }

.log-console.light-theme .console-logs::-webkit-scrollbar-thumb {
  background: #cbd5e1;
}

.log-console.light-theme .console-logs::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stat-item {
    padding: 12px;
    background: #fafbfc;
    border-radius: 10px;
  }
  
  .log-logger {
    display: none;
  }
}

@media (max-width: 640px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-field {
    max-width: none;
  }
  
  .filter-field.filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
  
  .log-modal-overlay {
    padding: 20px;
  }
}
</style>
