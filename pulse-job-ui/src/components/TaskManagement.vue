<template>
  <div class="task-management">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input 
            v-model="filters.keyword" 
            type="text" 
            class="search-input" 
            placeholder="搜索任务名称或ID..."
            @keyup.enter="handleSearch"
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
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" @click="handleCreate">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
          </template>
          新建任务
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" :disabled="checkedRowKeys.length === 0" @click="handleBatchDelete">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 6h18"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
              </svg>
            </button>
          </template>
          批量删除
        </n-tooltip>
        <div class="tool-divider"></div>
        <n-tooltip trigger="hover">
          <template #trigger>
            <button class="tool-btn" @click="tableExpanded = !tableExpanded">
              <svg v-if="!tableExpanded" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/>
                <line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/>
                <line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/>
              </svg>
            </button>
          </template>
          {{ tableExpanded ? '收起' : '展开' }}
        </n-tooltip>
        <n-dropdown trigger="click" :options="moreOptions" @select="handleMoreAction">
          <button class="tool-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/>
            </svg>
          </button>
        </n-dropdown>
      </div>
    </div>

    <!-- 高级筛选面板 -->
    <transition name="slide-down">
      <div v-if="showAdvancedFilter" class="advanced-filter">
      <div class="filter-row">
          <div class="filter-field">
          <label>任务ID</label>
            <n-input v-model:value="filters.taskId" placeholder="输入任务ID" clearable size="small" />
        </div>
          <div class="filter-field">
          <label>调度类型</label>
            <n-select v-model:value="filters.scheduleType" :options="scheduleTypeOptions" placeholder="全部" clearable size="small" />
        </div>
          <div class="filter-field">
          <label>运行模式</label>
            <n-select v-model:value="filters.runMode" :options="runModeOptions" placeholder="全部" clearable size="small" />
        </div>
          <div class="filter-field">
          <label>负责人</label>
            <n-input v-model:value="filters.owner" placeholder="输入负责人" clearable size="small" />
        </div>
          <div class="filter-field">
          <label>状态</label>
            <n-select v-model:value="filters.status" :options="statusOptions" placeholder="全部" clearable size="small" />
        </div>
          <div class="filter-field filter-actions">
            <n-button size="small" @click="handleReset">重置</n-button>
            <n-button size="small" type="primary" @click="handleSearch">查询</n-button>
        </div>
      </div>
      </div>
    </transition>

    <!-- 表格卡片 -->
    <div class="table-section">
      <n-data-table
        :columns="columns"
        :data="filteredTasks"
        :pagination="pagination"
        :row-key="row => row.id"
        :checked-row-keys="checkedRowKeys"
        @update:checked-row-keys="handleCheck"
        class="task-table"
      />
    </div>

    <!-- 创建任务弹窗 -->
    <transition name="modal">
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div 
        class="modal-container" 
        :style="{ transform: `translate(${modalPosition.x}px, ${modalPosition.y}px)` }"
      >
        <div 
          class="modal-header" 
          @mousedown="startDrag"
          style="cursor: move;"
        >
          <div class="modal-header-left">
            <div class="modal-logo">
              <svg width="22" height="22" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path opacity="0.25" d="M14.684 25.388C20.3284 25.388 24.904 20.8123 24.904 15.168C24.904 9.52365 20.3284 4.948 14.684 4.948C9.03965 4.948 4.464 9.52365 4.464 15.168C4.464 20.8123 9.03965 25.388 14.684 25.388Z" fill="#5E81F4"/>
                <path opacity="0.5" d="M6.292 13.272C3.74884 13.2711 1.45629 11.7393 0.482133 9.39014C-0.492025 7.04096 0.0437846 4.33633 1.84 2.53598C4.29692 0.080291 8.27908 0.080291 10.736 2.53598C11.9163 3.71535 12.5794 5.31546 12.5794 6.98398C12.5794 8.6525 11.9163 10.2526 10.736 11.432C9.56032 12.6149 7.95978 13.2776 6.292 13.272Z" fill="#5E81F4"/>
                <path opacity="0.5" d="M23.308 29.8959C20.3057 29.897 17.7208 27.7767 17.1348 24.8321C16.5488 21.8875 18.1248 18.9391 20.8988 17.7905C23.6728 16.642 26.8717 17.6133 28.5388 20.1104C30.2058 22.6074 29.8764 25.9343 27.752 28.0559C26.5753 29.2373 24.9754 29.8997 23.308 29.8959Z" fill="#5E81F4"/>
                <path d="M6.46 29.828C3.91539 29.8303 1.61987 28.2997 0.643664 25.9498C-0.332546 23.5999 0.202755 20.8934 2 19.092C2.27475 18.8241 2.57332 18.5818 2.892 18.368L3.2 18.172C3.416 18.044 3.64 17.928 3.876 17.816C3.996 17.764 4.116 17.708 4.248 17.66C4.49358 17.5703 4.74479 17.4968 5 17.44L5.16 17.396C5.18744 17.3861 5.2155 17.3781 5.244 17.372L6.668 17.268C6.852 17.268 7.068 17.304 7.264 17.332H7.308H7.496H7.556H7.616H8C10.4617 17.3091 12.8115 16.3006 14.524 14.532C16.2459 12.7768 17.1888 10.4023 17.14 7.94398V7.87598V7.80798C17.1259 7.73141 17.1178 7.65383 17.116 7.57598V7.38398L17.064 7.19998C17.064 7.08798 17.036 6.96798 17.032 6.85198L17.128 5.53198V5.49998V5.47598C17.128 5.44398 17.188 5.26798 17.188 5.26798C17.2488 4.9968 17.3263 4.72962 17.42 4.46798C17.464 4.34798 17.52 4.22798 17.576 4.10798C17.681 3.87112 17.8013 3.6413 17.936 3.41998V3.38398C17.992 3.29198 18.044 3.19998 18.108 3.11198C18.3181 2.7953 18.5579 2.49931 18.824 2.22798C21.285 -0.237417 25.2786 -0.240999 27.744 2.21998C30.2094 4.68096 30.213 8.67458 27.752 11.14C27.4788 11.4056 27.1815 11.6453 26.864 11.856C26.768 11.924 26.684 11.972 26.6 12.024L26.552 12.052C26.32 12.188 26.096 12.308 25.88 12.408C25.752 12.464 25.636 12.52 25.516 12.564C25.2636 12.6556 25.0057 12.7318 24.744 12.792C24.6886 12.8037 24.6339 12.8184 24.58 12.836L24.48 12.864L23.084 12.96C22.908 12.96 22.72 12.928 22.524 12.904H22.436H22.256H22.14H22.076H21.764C16.6622 12.9786 12.5777 17.1579 12.62 22.26V22.328V22.4C12.6308 22.4702 12.6375 22.541 12.64 22.612C12.64 22.688 12.64 22.76 12.66 22.832C12.68 22.904 12.66 22.968 12.684 23.04C12.698 23.1434 12.706 23.2476 12.708 23.352L12.608 24.724C12.6005 24.7604 12.5912 24.7965 12.58 24.832L12.544 24.956C12.4868 25.2276 12.4106 25.495 12.316 25.756C12.272 25.876 12.212 26 12.156 26.124C12.0543 26.355 11.9408 26.5806 11.816 26.8C11.752 26.912 11.692 27.012 11.624 27.108C11.4121 27.428 11.1696 27.7267 10.9 28C9.72428 29.1804 8.12605 29.8427 6.46 29.84V29.828Z" fill="#5E81F4"/>
              </svg>
            </div>
            <span class="modal-title">添加任务</span>
          </div>
          <button class="modal-close" @click="showCreateModal = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <!-- 基础配置 -->
          <div class="form-section">
            <div class="form-section-title">基础配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">执行器</label>
                <n-select v-model:value="createForm.executor" :options="executorOptions" placeholder="请选择执行器" />
              </div>
              <div class="form-item">
                <label class="form-label required">任务描述</label>
                <n-input v-model:value="createForm.description" placeholder="请输入任务描述" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">负责人</label>
                <n-input v-model:value="createForm.owner" placeholder="请输入负责人" />
              </div>
              <div class="form-item">
                <label class="form-label">报警邮件</label>
                <n-input v-model:value="createForm.alarmEmail" placeholder="多个邮件用逗号分隔" />
              </div>
            </div>
          </div>

          <!-- 调度配置 -->
          <div class="form-section">
            <div class="form-section-title">调度配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">调度类型</label>
                <n-select v-model:value="createForm.scheduleType" :options="scheduleTypeOptions" placeholder="请选择" />
              </div>
              <div class="form-item" v-if="createForm.scheduleType === 'cron'">
                <label class="form-label required">Cron表达式</label>
                <div class="cron-input-wrapper">
                  <n-input v-model:value="createForm.cronExpr" placeholder="如: 0 0 0 * * ?" />
                  <button type="button" class="cron-picker-btn" @click="openCronPicker('create')">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
                    </svg>
                  </button>
                </div>
              </div>
              <div class="form-item" v-else-if="createForm.scheduleType === 'fixed_rate' || createForm.scheduleType === 'fixed_delay'">
                <label class="form-label required">{{ createForm.scheduleType === 'fixed_rate' ? '执行间隔' : '延迟时间' }}</label>
                <div class="interval-input-wrapper">
                  <n-input-number v-model:value="createForm.intervalValue" :min="1" placeholder="请输入" style="flex: 1" />
                  <n-select v-model:value="createForm.intervalUnit" :options="intervalUnitOptions" style="width: 100px" />
                </div>
              </div>
              <div class="form-item" v-else-if="createForm.scheduleType === 'once'">
                <label class="form-label">执行时间</label>
                <n-date-picker v-model:value="createForm.onceTime" type="datetime" placeholder="留空则立即执行" clearable style="width: 100%" />
              </div>
            </div>
          </div>

          <!-- 任务配置 -->
          <div class="form-section">
            <div class="form-section-title">任务配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">运行模式</label>
                <n-select v-model:value="createForm.runMode" :options="glueTypeOptions" placeholder="请选择" />
              </div>
              <div class="form-item">
                <label class="form-label required">JobHandler</label>
                <n-input v-model:value="createForm.handler" placeholder="请输入Handler名称" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item full">
                <label class="form-label">任务参数</label>
                <n-input v-model:value="createForm.params" placeholder="执行器任务参数" />
              </div>
            </div>
          </div>

          <!-- 高级配置 -->
          <div class="form-section">
            <div class="form-section-title">高级配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">路由策略</label>
                <n-select v-model:value="createForm.routeStrategy" :options="routeStrategyOptions" placeholder="请选择" />
              </div>
              <div class="form-item">
                <label class="form-label">子任务ID</label>
                <n-input v-model:value="createForm.childJobId" placeholder="多个用逗号分隔" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">调度过期策略</label>
                <n-select v-model:value="createForm.misfireStrategy" :options="misfireStrategyOptions" placeholder="请选择" />
              </div>
              <div class="form-item">
                <label class="form-label required">阻塞处理策略</label>
                <n-select v-model:value="createForm.blockStrategy" :options="blockStrategyOptions" placeholder="请选择" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label">任务超时时间</label>
                <n-input-number v-model:value="createForm.timeout" :min="0" placeholder="单位秒，0=不限制" style="width: 100%" />
              </div>
              <div class="form-item">
                <label class="form-label">失败重试次数</label>
                <n-input-number v-model:value="createForm.retryCount" :min="0" :max="10" placeholder="0=不重试" style="width: 100%" />
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" @click="handleSubmitCreate">确定</n-button>
        </div>
      </div>
    </div>
    </transition>

    <!-- 查看详情弹窗 -->
    <transition name="modal">
    <div v-if="showDetailModal" class="detail-modal-overlay" @click.self="showDetailModal = false">
      <div class="detail-modal">
        <div class="detail-modal-header">
          <div class="header-left">
            <div class="header-logo">
              <svg width="22" height="22" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path opacity="0.25" d="M14.684 25.388C20.3284 25.388 24.904 20.8123 24.904 15.168C24.904 9.52365 20.3284 4.948 14.684 4.948C9.03965 4.948 4.464 9.52365 4.464 15.168C4.464 20.8123 9.03965 25.388 14.684 25.388Z" fill="#5E81F4"/>
                <path opacity="0.5" d="M6.292 13.272C3.74884 13.2711 1.45629 11.7393 0.482133 9.39014C-0.492025 7.04096 0.0437846 4.33633 1.84 2.53598C4.29692 0.080291 8.27908 0.080291 10.736 2.53598C11.9163 3.71535 12.5794 5.31546 12.5794 6.98398C12.5794 8.6525 11.9163 10.2526 10.736 11.432C9.56032 12.6149 7.95978 13.2776 6.292 13.272Z" fill="#5E81F4"/>
                <path opacity="0.5" d="M23.308 29.8959C20.3057 29.897 17.7208 27.7767 17.1348 24.8321C16.5488 21.8875 18.1248 18.9391 20.8988 17.7905C23.6728 16.642 26.8717 17.6133 28.5388 20.1104C30.2058 22.6074 29.8764 25.9343 27.752 28.0559C26.5753 29.2373 24.9754 29.8997 23.308 29.8959Z" fill="#5E81F4"/>
                <path d="M6.46 29.828C3.91539 29.8303 1.61987 28.2997 0.643664 25.9498C-0.332546 23.5999 0.202755 20.8934 2 19.092C2.27475 18.8241 2.57332 18.5818 2.892 18.368L3.2 18.172C3.416 18.044 3.64 17.928 3.876 17.816C3.996 17.764 4.116 17.708 4.248 17.66C4.49358 17.5703 4.74479 17.4968 5 17.44L5.16 17.396C5.18744 17.3861 5.2155 17.3781 5.244 17.372L6.668 17.268C6.852 17.268 7.068 17.304 7.264 17.332H7.308H7.496H7.556H7.616H8C10.4617 17.3091 12.8115 16.3006 14.524 14.532C16.2459 12.7768 17.1888 10.4023 17.14 7.94398V7.87598V7.80798C17.1259 7.73141 17.1178 7.65383 17.116 7.57598V7.38398L17.064 7.19998C17.064 7.08798 17.036 6.96798 17.032 6.85198L17.128 5.53198V5.49998V5.47598C17.128 5.44398 17.188 5.26798 17.188 5.26798C17.2488 4.9968 17.3263 4.72962 17.42 4.46798C17.464 4.34798 17.52 4.22798 17.576 4.10798C17.681 3.87112 17.8013 3.6413 17.936 3.41998V3.38398C17.992 3.29198 18.044 3.19998 18.108 3.11198C18.3181 2.7953 18.5579 2.49931 18.824 2.22798C21.285 -0.237417 25.2786 -0.240999 27.744 2.21998C30.2094 4.68096 30.213 8.67458 27.752 11.14C27.4788 11.4056 27.1815 11.6453 26.864 11.856C26.768 11.924 26.684 11.972 26.6 12.024L26.552 12.052C26.32 12.188 26.096 12.308 25.88 12.408C25.752 12.464 25.636 12.52 25.516 12.564C25.2636 12.6556 25.0057 12.7318 24.744 12.792C24.6886 12.8037 24.6339 12.8184 24.58 12.836L24.48 12.864L23.084 12.96C22.908 12.96 22.72 12.928 22.524 12.904H22.436H22.256H22.14H22.076H21.764C16.6622 12.9786 12.5777 17.1579 12.62 22.26V22.328V22.4C12.6308 22.4702 12.6375 22.541 12.64 22.612C12.64 22.688 12.64 22.76 12.66 22.832C12.68 22.904 12.66 22.968 12.684 23.04C12.698 23.1434 12.706 23.2476 12.708 23.352L12.608 24.724C12.6005 24.7604 12.5912 24.7965 12.58 24.832L12.544 24.956C12.4868 25.2276 12.4106 25.495 12.316 25.756C12.272 25.876 12.212 26 12.156 26.124C12.0543 26.355 11.9408 26.5806 11.816 26.8C11.752 26.912 11.692 27.012 11.624 27.108C11.4121 27.428 11.1696 27.7267 10.9 28C9.72428 29.1804 8.12605 29.8427 6.46 29.84V29.828Z" fill="#5E81F4"/>
              </svg>
            </div>
            <span class="header-title">任务详情 #{{ currentTask.id }}</span>
          </div>
          <button class="close-btn" @click="showDetailModal = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="detail-modal-body">
          <div class="detail-section first">
            <div class="section-title">基础配置</div>
            <n-descriptions :column="2" label-placement="left" bordered size="small">
              <n-descriptions-item label="任务ID">{{ currentTask.id }}</n-descriptions-item>
              <n-descriptions-item label="状态">
                <n-tag :type="statusConfig[currentTask.status]?.type" size="small">
                  {{ statusConfig[currentTask.status]?.label }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="任务描述" :span="2">{{ currentTask.description }}</n-descriptions-item>
              <n-descriptions-item label="执行器">{{ currentTask.executor || '示例执行器' }}</n-descriptions-item>
              <n-descriptions-item label="负责人">{{ currentTask.owner }}</n-descriptions-item>
            </n-descriptions>
          </div>

          <div class="detail-section">
            <div class="section-title">调度配置</div>
            <n-descriptions :column="2" label-placement="left" bordered size="small">
              <n-descriptions-item label="调度类型">{{ getScheduleTypeLabel(currentTask.scheduleType) }}</n-descriptions-item>
              <n-descriptions-item label="Cron表达式">
                <span class="code-text">{{ currentTask.scheduleExpr }}</span>
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <div class="detail-section">
            <div class="section-title">任务配置</div>
            <n-descriptions :column="2" label-placement="left" bordered size="small">
              <n-descriptions-item label="运行模式">{{ getRunModeLabel(currentTask.runMode) }}</n-descriptions-item>
              <n-descriptions-item label="JobHandler">{{ currentTask.handler }}</n-descriptions-item>
            </n-descriptions>
          </div>

          <div class="detail-section">
            <div class="section-title">高级配置</div>
            <n-descriptions :column="2" label-placement="left" bordered size="small">
              <n-descriptions-item label="路由策略">{{ currentTask.routeStrategy || '第一个' }}</n-descriptions-item>
              <n-descriptions-item label="子任务ID">{{ currentTask.childJobId || '-' }}</n-descriptions-item>
              <n-descriptions-item label="调度过期策略">{{ currentTask.misfireStrategy || '忽略' }}</n-descriptions-item>
              <n-descriptions-item label="阻塞处理策略">{{ currentTask.blockStrategy || '单机串行' }}</n-descriptions-item>
              <n-descriptions-item label="任务超时时间">{{ currentTask.timeout || 0 }}秒</n-descriptions-item>
              <n-descriptions-item label="失败重试次数">{{ currentTask.retryCount || 0 }}次</n-descriptions-item>
            </n-descriptions>
          </div>
        </div>
        <div class="detail-modal-footer"></div>
      </div>
    </div>
    </transition>

    <!-- 编辑任务弹窗 -->
    <transition name="modal">
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div 
        class="modal-container" 
        :style="{ transform: `translate(${editModalPosition.x}px, ${editModalPosition.y}px)` }"
      >
        <div class="modal-header" @mousedown="startEditDrag" style="cursor: move;">
          <div class="modal-header-left">
            <div class="modal-logo">
              <svg width="22" height="22" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path opacity="0.25" d="M14.684 25.388C20.3284 25.388 24.904 20.8123 24.904 15.168C24.904 9.52365 20.3284 4.948 14.684 4.948C9.03965 4.948 4.464 9.52365 4.464 15.168C4.464 20.8123 9.03965 25.388 14.684 25.388Z" fill="#5E81F4"/>
                <path opacity="0.5" d="M6.292 13.272C3.74884 13.2711 1.45629 11.7393 0.482133 9.39014C-0.492025 7.04096 0.0437846 4.33633 1.84 2.53598C4.29692 0.080291 8.27908 0.080291 10.736 2.53598C11.9163 3.71535 12.5794 5.31546 12.5794 6.98398C12.5794 8.6525 11.9163 10.2526 10.736 11.432C9.56032 12.6149 7.95978 13.2776 6.292 13.272Z" fill="#5E81F4"/>
                <path opacity="0.5" d="M23.308 29.8959C20.3057 29.897 17.7208 27.7767 17.1348 24.8321C16.5488 21.8875 18.1248 18.9391 20.8988 17.7905C23.6728 16.642 26.8717 17.6133 28.5388 20.1104C30.2058 22.6074 29.8764 25.9343 27.752 28.0559C26.5753 29.2373 24.9754 29.8997 23.308 29.8959Z" fill="#5E81F4"/>
                <path d="M6.46 29.828C3.91539 29.8303 1.61987 28.2997 0.643664 25.9498C-0.332546 23.5999 0.202755 20.8934 2 19.092C2.27475 18.8241 2.57332 18.5818 2.892 18.368L3.2 18.172C3.416 18.044 3.64 17.928 3.876 17.816C3.996 17.764 4.116 17.708 4.248 17.66C4.49358 17.5703 4.74479 17.4968 5 17.44L5.16 17.396C5.18744 17.3861 5.2155 17.3781 5.244 17.372L6.668 17.268C6.852 17.268 7.068 17.304 7.264 17.332H7.308H7.496H7.556H7.616H8C10.4617 17.3091 12.8115 16.3006 14.524 14.532C16.2459 12.7768 17.1888 10.4023 17.14 7.94398V7.87598V7.80798C17.1259 7.73141 17.1178 7.65383 17.116 7.57598V7.38398L17.064 7.19998C17.064 7.08798 17.036 6.96798 17.032 6.85198L17.128 5.53198V5.49998V5.47598C17.128 5.44398 17.188 5.26798 17.188 5.26798C17.2488 4.9968 17.3263 4.72962 17.42 4.46798C17.464 4.34798 17.52 4.22798 17.576 4.10798C17.681 3.87112 17.8013 3.6413 17.936 3.41998V3.38398C17.992 3.29198 18.044 3.19998 18.108 3.11198C18.3181 2.7953 18.5579 2.49931 18.824 2.22798C21.285 -0.237417 25.2786 -0.240999 27.744 2.21998C30.2094 4.68096 30.213 8.67458 27.752 11.14C27.4788 11.4056 27.1815 11.6453 26.864 11.856C26.768 11.924 26.684 11.972 26.6 12.024L26.552 12.052C26.32 12.188 26.096 12.308 25.88 12.408C25.752 12.464 25.636 12.52 25.516 12.564C25.2636 12.6556 25.0057 12.7318 24.744 12.792C24.6886 12.8037 24.6339 12.8184 24.58 12.836L24.48 12.864L23.084 12.96C22.908 12.96 22.72 12.928 22.524 12.904H22.436H22.256H22.14H22.076H21.764C16.6622 12.9786 12.5777 17.1579 12.62 22.26V22.328V22.4C12.6308 22.4702 12.6375 22.541 12.64 22.612C12.64 22.688 12.64 22.76 12.66 22.832C12.68 22.904 12.66 22.968 12.684 23.04C12.698 23.1434 12.706 23.2476 12.708 23.352L12.608 24.724C12.6005 24.7604 12.5912 24.7965 12.58 24.832L12.544 24.956C12.4868 25.2276 12.4106 25.495 12.316 25.756C12.272 25.876 12.212 26 12.156 26.124C12.0543 26.355 11.9408 26.5806 11.816 26.8C11.752 26.912 11.692 27.012 11.624 27.108C11.4121 27.428 11.1696 27.7267 10.9 28C9.72428 29.1804 8.12605 29.8427 6.46 29.84V29.828Z" fill="#5E81F4"/>
              </svg>
            </div>
            <span class="modal-title">编辑任务</span>
          </div>
          <button class="modal-close" @click="showEditModal = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <!-- 基础配置 -->
          <div class="form-section">
            <div class="form-section-title">基础配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label">任务ID</label>
                <n-input v-model:value="editForm.id" disabled />
              </div>
              <div class="form-item">
                <label class="form-label required">执行器</label>
                <n-select v-model:value="editForm.executor" :options="executorOptions" placeholder="请选择执行器" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">任务描述</label>
                <n-input v-model:value="editForm.description" placeholder="请输入任务描述" />
              </div>
              <div class="form-item">
                <label class="form-label required">负责人</label>
                <n-input v-model:value="editForm.owner" placeholder="请输入负责人" />
              </div>
            </div>
          </div>

          <!-- 调度配置 -->
          <div class="form-section">
            <div class="form-section-title">调度配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">调度类型</label>
                <n-select v-model:value="editForm.scheduleType" :options="scheduleTypeOptions" placeholder="请选择" />
              </div>
              <div class="form-item" v-if="editForm.scheduleType === 'cron'">
                <label class="form-label required">Cron表达式</label>
                <div class="cron-input-wrapper">
                  <n-input v-model:value="editForm.cronExpr" placeholder="如: 0 0 0 * * ?" />
                  <button type="button" class="cron-picker-btn" @click="openCronPicker('edit')">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
                    </svg>
                  </button>
                </div>
              </div>
              <div class="form-item" v-else-if="editForm.scheduleType === 'fixed_rate' || editForm.scheduleType === 'fixed_delay'">
                <label class="form-label required">{{ editForm.scheduleType === 'fixed_rate' ? '执行间隔' : '延迟时间' }}</label>
                <div class="interval-input-wrapper">
                  <n-input-number v-model:value="editForm.intervalValue" :min="1" placeholder="请输入" style="flex: 1" />
                  <n-select v-model:value="editForm.intervalUnit" :options="intervalUnitOptions" style="width: 100px" />
                </div>
              </div>
              <div class="form-item" v-else-if="editForm.scheduleType === 'once'">
                <label class="form-label">执行时间</label>
                <n-date-picker v-model:value="editForm.onceTime" type="datetime" placeholder="留空则立即执行" clearable style="width: 100%" />
              </div>
            </div>
          </div>

          <!-- 任务配置 -->
          <div class="form-section">
            <div class="form-section-title">任务配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">运行模式</label>
                <n-select v-model:value="editForm.runMode" :options="glueTypeOptions" placeholder="请选择" />
              </div>
              <div class="form-item">
                <label class="form-label required">JobHandler</label>
                <n-input v-model:value="editForm.handler" placeholder="请输入Handler名称" />
              </div>
            </div>
          </div>

          <!-- 高级配置 -->
          <div class="form-section">
            <div class="form-section-title">高级配置</div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">路由策略</label>
                <n-select v-model:value="editForm.routeStrategy" :options="routeStrategyOptions" placeholder="请选择" />
              </div>
              <div class="form-item">
                <label class="form-label">子任务ID</label>
                <n-input v-model:value="editForm.childJobId" placeholder="多个用逗号分隔" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">调度过期策略</label>
                <n-select v-model:value="editForm.misfireStrategy" :options="misfireStrategyOptions" placeholder="请选择" />
              </div>
              <div class="form-item">
                <label class="form-label required">阻塞处理策略</label>
                <n-select v-model:value="editForm.blockStrategy" :options="blockStrategyOptions" placeholder="请选择" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label">任务超时时间</label>
                <n-input-number v-model:value="editForm.timeout" :min="0" placeholder="单位秒，0=不限制" style="width: 100%" />
              </div>
              <div class="form-item">
                <label class="form-label">失败重试次数</label>
                <n-input-number v-model:value="editForm.retryCount" :min="0" :max="10" placeholder="0=不重试" style="width: 100%" />
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button type="primary" @click="handleSubmitEdit">保存</n-button>
        </div>
      </div>
    </div>
    </transition>

    <!-- Cron选择器弹窗 -->
    <transition name="modal">
    <div v-if="showCronPicker" class="detail-modal-overlay" @click.self="showCronPicker = false">
      <div class="cron-picker-modal">
        <div class="detail-modal-header">
          <div class="header-left">
            <div class="header-logo">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#5E81F4" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
              </svg>
            </div>
            <span class="header-title">Cron表达式生成器</span>
          </div>
          <button class="close-btn" @click="showCronPicker = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="cron-picker-body">
          <!-- Tab 导航 -->
          <div class="cron-tabs">
            <button 
              v-for="tab in cronTabs" 
              :key="tab.key" 
              class="cron-tab"
              :class="{ active: cronConfig.activeTab === tab.key }"
              @click="cronConfig.activeTab = tab.key"
            >
              {{ tab.label }}
              <span class="tab-value">{{ getTabValue(tab.key) }}</span>
            </button>
          </div>

          <!-- Tab 内容 -->
          <div class="cron-tab-content">
            <!-- 秒 -->
            <div v-if="cronConfig.activeTab === 'second'" class="tab-panel">
              <div class="panel-options">
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.secondType" value="every" />
                  <span>每秒</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.secondType" value="interval" />
                  <span>每隔</span>
                  <n-input-number v-model:value="cronConfig.secondInterval" :min="1" :max="59" size="small" style="width: 70px" />
                  <span>秒执行</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.secondType" value="specific" />
                  <span>指定秒</span>
                </label>
              </div>
              <div v-if="cronConfig.secondType === 'specific'" class="value-grid">
                <label v-for="i in 60" :key="i" class="checkbox-item">
                  <input type="checkbox" :value="i-1" v-model="cronConfig.secondValues" />
                  <span>{{ String(i-1).padStart(2, '0') }}</span>
                </label>
              </div>
            </div>

            <!-- 分钟 -->
            <div v-if="cronConfig.activeTab === 'minute'" class="tab-panel">
              <div class="panel-options">
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.minuteType" value="every" />
                  <span>每分钟</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.minuteType" value="interval" />
                  <span>每隔</span>
                  <n-input-number v-model:value="cronConfig.minuteInterval" :min="1" :max="59" size="small" style="width: 70px" />
                  <span>分钟执行</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.minuteType" value="specific" />
                  <span>指定分钟</span>
                </label>
              </div>
              <div v-if="cronConfig.minuteType === 'specific'" class="value-grid">
                <label v-for="i in 60" :key="i" class="checkbox-item">
                  <input type="checkbox" :value="i-1" v-model="cronConfig.minuteValues" />
                  <span>{{ String(i-1).padStart(2, '0') }}</span>
                </label>
              </div>
            </div>

            <!-- 小时 -->
            <div v-if="cronConfig.activeTab === 'hour'" class="tab-panel">
              <div class="panel-options">
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.hourType" value="every" />
                  <span>每小时</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.hourType" value="interval" />
                  <span>每隔</span>
                  <n-input-number v-model:value="cronConfig.hourInterval" :min="1" :max="23" size="small" style="width: 70px" />
                  <span>小时执行</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.hourType" value="specific" />
                  <span>指定小时</span>
                </label>
              </div>
              <div v-if="cronConfig.hourType === 'specific'" class="value-grid cols-12">
                <label v-for="i in 24" :key="i" class="checkbox-item">
                  <input type="checkbox" :value="i-1" v-model="cronConfig.hourValues" />
                  <span>{{ String(i-1).padStart(2, '0') }}</span>
                </label>
              </div>
            </div>

            <!-- 日 -->
            <div v-if="cronConfig.activeTab === 'day'" class="tab-panel">
              <div class="panel-options">
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.dayType" value="every" />
                  <span>每天</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.dayType" value="none" />
                  <span>不指定（使用周）</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.dayType" value="last" />
                  <span>每月最后一天</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.dayType" value="specific" />
                  <span>指定日期</span>
                </label>
              </div>
              <div v-if="cronConfig.dayType === 'specific'" class="value-grid cols-10">
                <label v-for="i in 31" :key="i" class="checkbox-item">
                  <input type="checkbox" :value="i" v-model="cronConfig.dayValues" />
                  <span>{{ i }}</span>
                </label>
              </div>
            </div>

            <!-- 月 -->
            <div v-if="cronConfig.activeTab === 'month'" class="tab-panel">
              <div class="panel-options">
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.monthType" value="every" />
                  <span>每月</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.monthType" value="specific" />
                  <span>指定月份</span>
                </label>
              </div>
              <div v-if="cronConfig.monthType === 'specific'" class="value-grid cols-6">
                <label v-for="i in 12" :key="i" class="checkbox-item">
                  <input type="checkbox" :value="i" v-model="cronConfig.monthValues" />
                  <span>{{ i }}月</span>
                </label>
              </div>
            </div>

            <!-- 周 -->
            <div v-if="cronConfig.activeTab === 'week'" class="tab-panel">
              <div class="panel-options">
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.weekType" value="none" />
                  <span>不指定（使用日）</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.weekType" value="every" />
                  <span>每天</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.weekType" value="specific" />
                  <span>指定星期</span>
                </label>
              </div>
              <div v-if="cronConfig.weekType === 'specific'" class="value-grid cols-7">
                <label v-for="item in weekDays" :key="item.value" class="checkbox-item">
                  <input type="checkbox" :value="item.value" v-model="cronConfig.weekValues" />
                  <span>{{ item.label }}</span>
                </label>
              </div>
            </div>

            <!-- 年 -->
            <div v-if="cronConfig.activeTab === 'year'" class="tab-panel">
              <div class="panel-options">
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.yearType" value="every" />
                  <span>每年</span>
                </label>
                <label class="radio-item">
                  <input type="radio" v-model="cronConfig.yearType" value="specific" />
                  <span>指定年份</span>
                </label>
              </div>
              <div v-if="cronConfig.yearType === 'specific'" class="value-grid cols-5">
                <label v-for="year in yearOptions" :key="year" class="checkbox-item">
                  <input type="checkbox" :value="year" v-model="cronConfig.yearValues" />
                  <span>{{ year }}</span>
                </label>
              </div>
            </div>
          </div>

          <!-- 预览 -->
          <div class="cron-preview">
            <div class="preview-fields">
              <span class="field" v-for="(field, index) in cronFields" :key="index" :class="{ active: cronConfig.activeTab === field.key }">
                <em>{{ field.label }}</em>
                {{ field.value }}
              </span>
            </div>
            <div class="preview-expression">{{ generatedCronExpression }}</div>
            <div class="preview-desc">{{ cronDescription }}</div>
          </div>
        </div>
        <div class="detail-modal-footer" style="justify-content: flex-end; gap: 12px; display: flex;">
          <n-button @click="showCronPicker = false">取消</n-button>
          <n-button type="primary" @click="applyCronExpression">确定</n-button>
        </div>
      </div>
    </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h } from 'vue'
import { 
  NInput, NInputNumber, NSelect, NButton, NDataTable, NTag, NPopconfirm, NTooltip, NDropdown,
  NDatePicker, useMessage
} from 'naive-ui'

const message = useMessage()

// 状态
const showAdvancedFilter = ref(false)
const tableExpanded = ref(false)
const showCreateModal = ref(false)
const showDetailModal = ref(false)
const showEditModal = ref(false)
const currentTask = ref({})

// Cron选择器
const showCronPicker = ref(false)
const cronPickerTarget = ref('create') // 'create' or 'edit'
const cronConfig = reactive({
  activeTab: 'second',
  // 秒
  secondType: 'every',
  secondInterval: 5,
  secondValues: [],
  // 分钟
  minuteType: 'specific',
  minuteInterval: 5,
  minuteValues: [0],
  // 小时
  hourType: 'specific',
  hourInterval: 2,
  hourValues: [0],
  // 日
  dayType: 'every',
  dayValues: [],
  // 月
  monthType: 'every',
  monthValues: [],
  // 周
  weekType: 'none',
  weekValues: [],
  // 年
  yearType: 'every',
  yearValues: [],
})

// Tab 配置
const cronTabs = [
  { key: 'second', label: '秒' },
  { key: 'minute', label: '分' },
  { key: 'hour', label: '时' },
  { key: 'day', label: '日' },
  { key: 'month', label: '月' },
  { key: 'week', label: '周' },
  { key: 'year', label: '年' },
]

// 星期选项
const weekDays = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' },
]

// 年份选项（当前年份往后10年）
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 10 }, (_, i) => currentYear + i)

// 获取各字段的值
const getSecondValue = () => {
  if (cronConfig.secondType === 'every') return '*'
  if (cronConfig.secondType === 'interval') return `0/${cronConfig.secondInterval}`
  if (cronConfig.secondValues.length > 0) return cronConfig.secondValues.sort((a, b) => a - b).join(',')
  return '0'
}

const getMinuteValue = () => {
  if (cronConfig.minuteType === 'every') return '*'
  if (cronConfig.minuteType === 'interval') return `0/${cronConfig.minuteInterval}`
  if (cronConfig.minuteValues.length > 0) return cronConfig.minuteValues.sort((a, b) => a - b).join(',')
  return '0'
}

const getHourValue = () => {
  if (cronConfig.hourType === 'every') return '*'
  if (cronConfig.hourType === 'interval') return `0/${cronConfig.hourInterval}`
  if (cronConfig.hourValues.length > 0) return cronConfig.hourValues.sort((a, b) => a - b).join(',')
  return '0'
}

const getDayValue = () => {
  if (cronConfig.dayType === 'every') return '*'
  if (cronConfig.dayType === 'none') return '?'
  if (cronConfig.dayType === 'last') return 'L'
  if (cronConfig.dayValues.length > 0) return cronConfig.dayValues.sort((a, b) => a - b).join(',')
  return '*'
}

const getMonthValue = () => {
  if (cronConfig.monthType === 'every') return '*'
  if (cronConfig.monthValues.length > 0) return cronConfig.monthValues.sort((a, b) => a - b).join(',')
  return '*'
}

const getWeekValue = () => {
  if (cronConfig.weekType === 'none') return '?'
  if (cronConfig.weekType === 'every') return '*'
  if (cronConfig.weekValues.length > 0) return cronConfig.weekValues.join(',')
  return '?'
}

const getYearValue = () => {
  if (cronConfig.yearType === 'every') return '*'
  if (cronConfig.yearValues.length > 0) return cronConfig.yearValues.sort((a, b) => a - b).join(',')
  return '*'
}

// 获取Tab显示值
const getTabValue = (key) => {
  switch (key) {
    case 'second': return getSecondValue()
    case 'minute': return getMinuteValue()
    case 'hour': return getHourValue()
    case 'day': return getDayValue()
    case 'month': return getMonthValue()
    case 'week': return getWeekValue()
    case 'year': return getYearValue()
  }
}

// Cron字段
const cronFields = computed(() => [
  { key: 'second', label: '秒', value: getSecondValue() },
  { key: 'minute', label: '分', value: getMinuteValue() },
  { key: 'hour', label: '时', value: getHourValue() },
  { key: 'day', label: '日', value: getDayValue() },
  { key: 'month', label: '月', value: getMonthValue() },
  { key: 'week', label: '周', value: getWeekValue() },
  { key: 'year', label: '年', value: getYearValue() },
])

// 生成的Cron表达式
const generatedCronExpression = computed(() => {
  const year = getYearValue()
  const base = `${getSecondValue()} ${getMinuteValue()} ${getHourValue()} ${getDayValue()} ${getMonthValue()} ${getWeekValue()}`
  return year === '*' ? base : `${base} ${year}`
})

// 解析Cron字段
const parseCronField = (field, type) => {
  if (field === '*') return { type: 'every' }
  if (field === '?') return { type: 'any' }
  if (field === 'L') return { type: 'last' }
  if (field.includes('/')) {
    const [start, interval] = field.split('/')
    return { type: 'interval', start: start === '*' ? 0 : parseInt(start), interval: parseInt(interval) }
  }
  if (field.includes('-') && !field.includes(',')) {
    const [start, end] = field.split('-')
    return { type: 'range', start, end }
  }
  if (field.includes(',')) {
    return { type: 'list', values: field.split(',') }
  }
  return { type: 'fixed', value: field }
}

// 生成Cron描述
const cronDescription = computed(() => {
  const expr = generatedCronExpression.value
  if (!expr) return '请配置执行时间'
  
  const parts = expr.split(' ')
  if (parts.length < 6) return '表达式格式错误'
  
  const [secondStr, minuteStr, hourStr, dayStr, monthStr, weekStr, yearStr] = parts
  
  const second = parseCronField(secondStr, 'second')
  const minute = parseCronField(minuteStr, 'minute')
  const hour = parseCronField(hourStr, 'hour')
  const day = parseCronField(dayStr, 'day')
  const month = parseCronField(monthStr, 'month')
  const week = parseCronField(weekStr, 'week')
  const year = yearStr ? parseCronField(yearStr, 'year') : { type: 'every' }
  
  const weekNameMap = { 
    'MON': '一', 'TUE': '二', 'WED': '三', 'THU': '四', 
    'FRI': '五', 'SAT': '六', 'SUN': '日',
    '1': '日', '2': '一', '3': '二', '4': '三', '5': '四', '6': '五', '7': '六'
  }
  
  let desc = []
  
  // 年
  if (year.type === 'fixed') {
    desc.push(`${year.value}年`)
  } else if (year.type === 'list') {
    desc.push(`${year.values.join('/')}年`)
  } else if (year.type === 'range') {
    desc.push(`${year.start}-${year.end}年`)
  }
  
  // 月
  if (month.type === 'fixed') {
    desc.push(`${month.value}月`)
  } else if (month.type === 'list') {
    desc.push(`${month.values.join('、')}月`)
  } else if (month.type === 'range') {
    desc.push(`${month.start}-${month.end}月`)
  } else if (month.type === 'interval') {
    desc.push(`从${month.start}月开始每${month.interval}个月`)
  }
  
  // 日/周 - 只在有具体约束时显示
  if (week.type !== 'any' && week.type !== 'every') {
    if (week.type === 'fixed') {
      desc.push(`每周${weekNameMap[week.value] || week.value}`)
    } else if (week.type === 'list') {
      const names = week.values.map(v => weekNameMap[v] || v)
      // 检查工作日
      if (names.length === 5 && ['一','二','三','四','五'].every(d => names.includes(d))) {
        desc.push('工作日')
      } else if (names.length === 2 && names.includes('六') && names.includes('日')) {
        desc.push('周末')
      } else {
        desc.push(`每周${names.join('、')}`)
      }
    } else if (week.type === 'range') {
      desc.push(`每周${weekNameMap[week.start] || week.start}至周${weekNameMap[week.end] || week.end}`)
    }
  } else if (day.type !== 'any' && day.type !== 'every') {
    if (day.type === 'last') {
      desc.push('每月最后一天')
    } else if (day.type === 'fixed') {
      desc.push(`每月${day.value}日`)
    } else if (day.type === 'list') {
      desc.push(`每月${day.values.join('、')}日`)
    } else if (day.type === 'range') {
      desc.push(`每月${day.start}-${day.end}日`)
    } else if (day.type === 'interval') {
      desc.push(`每${day.interval}天`)
    }
  }
  // 不再显示"每天"，因为这是默认行为
  
  // 构建时间描述
  let timeParts = []
  
  // 判断最高频率级别
  const hasSecondFreq = second.type === 'every' || second.type === 'interval'
  const hasMinuteFreq = minute.type === 'every' || minute.type === 'interval'
  const hasHourFreq = hour.type === 'every' || hour.type === 'interval'
  
  // 小时 - 只在有具体值或有间隔时显示
  if (hour.type === 'interval') {
    timeParts.push(`每${hour.interval}小时`)
  } else if (hour.type === 'fixed') {
    timeParts.push(`${hour.value}点`)
  } else if (hour.type === 'list') {
    timeParts.push(`${hour.values.join('、')}点`)
  } else if (hour.type === 'range') {
    timeParts.push(`${hour.start}-${hour.end}点`)
  }
  // hour.type === 'every' 时不显示"每小时"，因为这是默认行为
  
  // 分钟 - 只在有具体值或有间隔时显示
  if (minute.type === 'interval') {
    timeParts.push(`每${minute.interval}分钟`)
  } else if (minute.type === 'fixed' && minute.value !== '0') {
    timeParts.push(`${minute.value}分`)
  } else if (minute.type === 'list') {
    timeParts.push(`${minute.values.join('、')}分`)
  } else if (minute.type === 'range') {
    timeParts.push(`${minute.start}-${minute.end}分`)
  }
  // minute.type === 'every' 时不显示"每分钟"
  
  // 秒 - 只在有具体值或有频率时显示
  if (second.type === 'every') {
    timeParts.push('每秒')
  } else if (second.type === 'interval') {
    timeParts.push(`每${second.interval}秒`)
  } else if (second.type === 'fixed' && second.value !== '0') {
    timeParts.push(`${second.value}秒`)
  } else if (second.type === 'list') {
    timeParts.push(`${second.values.join('、')}秒`)
  }
  
  // 处理特殊情况
  // 每小时整点: 0 0 * * * ?
  if (timeParts.length === 0 && hour.type === 'every' && 
      (minute.type === 'fixed' && minute.value === '0') && 
      (second.type === 'fixed' && second.value === '0')) {
    timeParts.push('每小时整点')
  }
  
  // 每分钟: 0 * * * * ?
  if (timeParts.length === 0 && minute.type === 'every' && 
      (second.type === 'fixed' && second.value === '0')) {
    timeParts.push('每分钟')
  }
  
  // 如果还是没有时间描述，添加默认
  if (timeParts.length === 0 && desc.length === 0) {
    if (second.type === 'every') {
      timeParts.push('每秒')
    } else {
      timeParts.push('每秒')
    }
  }
  
  // 组合描述
  let result = ''
  if (desc.length > 0) {
    result += desc.join(' ') + ' '
  }
  if (timeParts.length > 0) {
    result += timeParts.join(' ') + ' '
  }
  result += '执行'
  
  return result.replace(/\s+/g, ' ').trim()
})

// 解析Cron表达式并设置配置
const parseCronExpression = (expr) => {
  if (!expr) return
  
  const parts = expr.trim().split(/\s+/)
  if (parts.length < 6) return
  
  const [secondStr, minuteStr, hourStr, dayStr, monthStr, weekStr, yearStr] = parts
  
  // 解析秒
  if (secondStr === '*') {
    cronConfig.secondType = 'every'
  } else if (secondStr.includes('/')) {
    cronConfig.secondType = 'interval'
    cronConfig.secondInterval = parseInt(secondStr.split('/')[1])
  } else {
    cronConfig.secondType = 'specific'
    cronConfig.secondValues = secondStr.split(',').map(v => parseInt(v))
  }
  
  // 解析分钟
  if (minuteStr === '*') {
    cronConfig.minuteType = 'every'
  } else if (minuteStr.includes('/')) {
    cronConfig.minuteType = 'interval'
    cronConfig.minuteInterval = parseInt(minuteStr.split('/')[1])
  } else {
    cronConfig.minuteType = 'specific'
    cronConfig.minuteValues = minuteStr.split(',').map(v => parseInt(v))
  }
  
  // 解析小时
  if (hourStr === '*') {
    cronConfig.hourType = 'every'
  } else if (hourStr.includes('/')) {
    cronConfig.hourType = 'interval'
    cronConfig.hourInterval = parseInt(hourStr.split('/')[1])
  } else {
    cronConfig.hourType = 'specific'
    cronConfig.hourValues = hourStr.split(',').map(v => parseInt(v))
  }
  
  // 解析日
  if (dayStr === '*') {
    cronConfig.dayType = 'every'
  } else if (dayStr === '?') {
    cronConfig.dayType = 'none'
  } else if (dayStr === 'L') {
    cronConfig.dayType = 'last'
  } else {
    cronConfig.dayType = 'specific'
    cronConfig.dayValues = dayStr.split(',').map(v => parseInt(v))
  }
  
  // 解析月
  if (monthStr === '*') {
    cronConfig.monthType = 'every'
  } else {
    cronConfig.monthType = 'specific'
    cronConfig.monthValues = monthStr.split(',').map(v => parseInt(v))
  }
  
  // 解析周
  if (weekStr === '?') {
    cronConfig.weekType = 'none'
  } else if (weekStr === '*') {
    cronConfig.weekType = 'every'
  } else {
    cronConfig.weekType = 'specific'
    cronConfig.weekValues = weekStr.split(',')
  }
  
  // 解析年
  if (yearStr) {
    if (yearStr === '*') {
      cronConfig.yearType = 'every'
    } else {
      cronConfig.yearType = 'specific'
      cronConfig.yearValues = yearStr.split(',').map(v => parseInt(v))
    }
  }
}

// 打开Cron选择器
const openCronPicker = (target) => {
  cronPickerTarget.value = target
  
  // 获取现有的cron表达式
  const existingExpr = target === 'create' ? createForm.cronExpr : editForm.cronExpr
  
  // 先重置为默认值
  cronConfig.activeTab = 'second'
  cronConfig.secondType = 'every'
  cronConfig.secondInterval = 5
  cronConfig.secondValues = []
  cronConfig.minuteType = 'specific'
  cronConfig.minuteInterval = 5
  cronConfig.minuteValues = [0]
  cronConfig.hourType = 'specific'
  cronConfig.hourInterval = 2
  cronConfig.hourValues = [0]
  cronConfig.dayType = 'every'
  cronConfig.dayValues = []
  cronConfig.monthType = 'every'
  cronConfig.monthValues = []
  cronConfig.weekType = 'none'
  cronConfig.weekValues = []
  cronConfig.yearType = 'every'
  cronConfig.yearValues = []
  
  // 如果有现有表达式，解析并回显
  if (existingExpr && existingExpr.trim()) {
    parseCronExpression(existingExpr)
  }
  showCronPicker.value = true
}

// 应用Cron表达式
const applyCronExpression = () => {
  const expr = generatedCronExpression.value
  if (cronPickerTarget.value === 'create') {
    createForm.cronExpr = expr
  } else {
    editForm.cronExpr = expr
  }
  showCronPicker.value = false
}

// 弹窗拖拽 - 创建弹窗
const modalPosition = reactive({ x: 0, y: 0 })

// 弹窗拖拽 - 详情弹窗
const detailModalPosition = reactive({ x: 0, y: 0 })
const isDetailDragging = ref(false)
const detailDragStart = reactive({ x: 0, y: 0 })

const startDetailDrag = (e) => {
  isDetailDragging.value = true
  detailDragStart.x = e.clientX - detailModalPosition.x
  detailDragStart.y = e.clientY - detailModalPosition.y
  document.addEventListener('mousemove', onDetailDrag)
  document.addEventListener('mouseup', stopDetailDrag)
}

const onDetailDrag = (e) => {
  if (isDetailDragging.value) {
    detailModalPosition.x = e.clientX - detailDragStart.x
    detailModalPosition.y = e.clientY - detailDragStart.y
  }
}

const stopDetailDrag = () => {
  isDetailDragging.value = false
  document.removeEventListener('mousemove', onDetailDrag)
  document.removeEventListener('mouseup', stopDetailDrag)
}

// 弹窗拖拽 - 编辑弹窗
const editModalPosition = reactive({ x: 0, y: 0 })
const isEditDragging = ref(false)
const editDragStart = reactive({ x: 0, y: 0 })

const startEditDrag = (e) => {
  isEditDragging.value = true
  editDragStart.x = e.clientX - editModalPosition.x
  editDragStart.y = e.clientY - editModalPosition.y
  document.addEventListener('mousemove', onEditDrag)
  document.addEventListener('mouseup', stopEditDrag)
}

const onEditDrag = (e) => {
  if (isEditDragging.value) {
    editModalPosition.x = e.clientX - editDragStart.x
    editModalPosition.y = e.clientY - editDragStart.y
  }
}

const stopEditDrag = () => {
  isEditDragging.value = false
  document.removeEventListener('mousemove', onEditDrag)
  document.removeEventListener('mouseup', stopEditDrag)
}
const isDragging = ref(false)
const dragStart = reactive({ x: 0, y: 0 })

const startDrag = (e) => {
  isDragging.value = true
  dragStart.x = e.clientX - modalPosition.x
  dragStart.y = e.clientY - modalPosition.y
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!isDragging.value) return
  modalPosition.x = e.clientX - dragStart.x
  modalPosition.y = e.clientY - dragStart.y
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 创建表单
const createForm = reactive({
  executor: null,
  description: '',
  owner: '',
  alarmEmail: '',
  scheduleType: 'cron',
  cronExpr: '',
  intervalValue: 5,
  intervalUnit: 'minutes',
  onceTime: null,
  runMode: 'BEAN',
  handler: '',
  params: '',
  routeStrategy: 'FIRST',
  childJobId: '',
  misfireStrategy: 'DO_NOTHING',
  blockStrategy: 'SERIAL_EXECUTION',
  timeout: 0,
  retryCount: 0
})

// 编辑表单
const editForm = reactive({
  id: '',
  executor: null,
  description: '',
  owner: '',
  scheduleType: 'cron',
  cronExpr: '',
  intervalValue: 5,
  intervalUnit: 'minutes',
  onceTime: null,
  runMode: 'BEAN',
  handler: '',
  routeStrategy: 'FIRST',
  childJobId: '',
  misfireStrategy: 'DO_NOTHING',
  blockStrategy: 'SERIAL_EXECUTION',
  timeout: 0,
  retryCount: 0
})

// 执行器选项
const executorOptions = [
  { label: '示例执行器', value: 'sample-executor' },
  { label: '数据处理执行器', value: 'data-executor' },
  { label: '报表执行器', value: 'report-executor' }
]

// 运行模式选项
const glueTypeOptions = [
  { label: 'BEAN', value: 'BEAN' },
  { label: 'GLUE(Java)', value: 'GLUE_GROOVY' },
  { label: 'GLUE(Shell)', value: 'GLUE_SHELL' },
  { label: 'GLUE(Python)', value: 'GLUE_PYTHON' },
  { label: 'GLUE(PowerShell)', value: 'GLUE_POWERSHELL' }
]

// 路由策略选项
const routeStrategyOptions = [
  { label: '第一个', value: 'FIRST' },
  { label: '最后一个', value: 'LAST' },
  { label: '轮询', value: 'ROUND' },
  { label: '随机', value: 'RANDOM' },
  { label: '一致性HASH', value: 'CONSISTENT_HASH' },
  { label: '最不经常使用', value: 'LEAST_FREQUENTLY_USED' },
  { label: '最近最久未使用', value: 'LEAST_RECENTLY_USED' },
  { label: '故障转移', value: 'FAILOVER' },
  { label: '忙碌转移', value: 'BUSYOVER' },
  { label: '分片广播', value: 'SHARDING_BROADCAST' }
]

// 调度过期策略
const misfireStrategyOptions = [
  { label: '忽略', value: 'DO_NOTHING' },
  { label: '立即执行一次', value: 'FIRE_ONCE_NOW' }
]

// 阻塞处理策略
const blockStrategyOptions = [
  { label: '单机串行', value: 'SERIAL_EXECUTION' },
  { label: '丢弃后续调度', value: 'DISCARD_LATER' },
  { label: '覆盖之前调度', value: 'COVER_EARLY' }
]

// 更多操作选项
const moreOptions = [
  { label: '导出数据', key: 'export' },
  { label: '导入任务', key: 'import' },
  { type: 'divider' },
  { label: '批量启用', key: 'batch-enable' },
  { label: '批量禁用', key: 'batch-disable' }
]

// 筛选条件
const filters = reactive({
  keyword: '',
  taskId: '',
  scheduleType: null,
  runMode: null,
  owner: '',
  status: null
})

// 计算激活的筛选条件数量
const activeFilterCount = computed(() => {
  let count = 0
  if (filters.taskId) count++
  if (filters.scheduleType) count++
  if (filters.runMode) count++
  if (filters.owner) count++
  if (filters.status) count++
  return count
})

// 下拉选项
const scheduleTypeOptions = [
  { label: 'Cron', value: 'cron' },
  { label: '固定频率', value: 'fixed_rate' },
  { label: '固定延迟', value: 'fixed_delay' },
  { label: '一次性', value: 'once' }
]

const intervalUnitOptions = [
  { label: '秒', value: 'seconds' },
  { label: '分钟', value: 'minutes' },
  { label: '小时', value: 'hours' },
]

const runModeOptions = [
  { label: '单机', value: 'standalone' },
  { label: '广播', value: 'broadcast' },
  { label: '分片', value: 'sharding' },
  { label: 'MapReduce', value: 'map_reduce' }
]

const statusOptions = [
  { label: '运行中', value: 'running' },
  { label: '已暂停', value: 'paused' },
  { label: '已停止', value: 'stopped' },
  { label: '异常', value: 'error' }
]

// 状态配置
const statusConfig = {
  running: { label: '运行中', type: 'success' },
  paused: { label: '已暂停', type: 'warning' },
  stopped: { label: '已停止', type: 'default' },
  error: { label: '异常', type: 'error' }
}

// 选中的行
const checkedRowKeys = ref([])

// 模拟数据
const tasks = ref([
  { id: 'JOB-001', handler: 'com.example.DataSyncHandler', description: '数据同步任务', scheduleType: 'cron', scheduleExpr: '0 0 * * * ?', runMode: 'standalone', owner: '张三', status: 'running', nextTime: '2026-01-29 15:00:00' },
  { id: 'JOB-002', handler: 'com.example.LogCleanHandler', description: '日志清理任务', scheduleType: 'fixed_rate', scheduleExpr: '每30分钟', runMode: 'broadcast', owner: '李四', status: 'running', nextTime: '2026-01-29 14:30:00' },
  { id: 'JOB-003', handler: 'com.example.ReportHandler', description: '报表生成任务', scheduleType: 'cron', scheduleExpr: '0 0 8 * * ?', runMode: 'standalone', owner: '王五', status: 'paused', nextTime: '-' },
  { id: 'JOB-004', handler: 'com.example.CacheRefreshHandler', description: '缓存刷新任务', scheduleType: 'fixed_delay', scheduleExpr: '每5分钟', runMode: 'standalone', owner: '张三', status: 'running', nextTime: '2026-01-29 14:05:00' },
  { id: 'JOB-005', handler: 'com.example.EmailSendHandler', description: '邮件发送任务', scheduleType: 'cron', scheduleExpr: '0 0 9 * * ?', runMode: 'standalone', owner: '赵六', status: 'error', nextTime: '-' },
  { id: 'JOB-006', handler: 'com.example.BackupHandler', description: '数据备份任务', scheduleType: 'cron', scheduleExpr: '0 0 2 * * ?', runMode: 'standalone', owner: '李四', status: 'running', nextTime: '2026-01-30 02:00:00' },
  { id: 'JOB-007', handler: 'com.example.OrderStatHandler', description: '订单统计任务', scheduleType: 'cron', scheduleExpr: '0 0 0 * * ?', runMode: 'sharding', owner: '王五', status: 'running', nextTime: '2026-01-30 00:00:00' },
  { id: 'JOB-008', handler: 'com.example.UserProfileHandler', description: '用户画像计算', scheduleType: 'once', scheduleExpr: '手动触发', runMode: 'map_reduce', owner: '张三', status: 'stopped', nextTime: '-' },
  { id: 'JOB-009', handler: 'com.example.StockSyncHandler', description: '库存同步任务', scheduleType: 'fixed_rate', scheduleExpr: '每10分钟', runMode: 'standalone', owner: '赵六', status: 'running', nextTime: '2026-01-29 14:10:00' },
  { id: 'JOB-010', handler: 'com.example.PushHandler', description: '消息推送任务', scheduleType: 'cron', scheduleExpr: '0 */5 * * * ?', runMode: 'broadcast', owner: '李四', status: 'paused', nextTime: '-' }
])

// 筛选后的数据
const filteredTasks = computed(() => {
  return tasks.value.filter(task => {
    // 关键词搜索 - 匹配ID或描述
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (!task.id.toLowerCase().includes(kw) && !task.description.toLowerCase().includes(kw)) {
        return false
      }
    }
    if (filters.taskId && !task.id.toLowerCase().includes(filters.taskId.toLowerCase())) return false
    if (filters.scheduleType && task.scheduleType !== filters.scheduleType) return false
    if (filters.runMode && task.runMode !== filters.runMode) return false
    if (filters.owner && !task.owner.includes(filters.owner)) return false
    if (filters.status && task.status !== filters.status) return false
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

// 状态切换处理
const handleStatusSwitch = (row, value) => {
  row.status = value ? 'running' : 'stopped'
  const action = value ? '启动' : '停止'
  message.success(`${action}任务: ${row.name}`)
}

// 表格列定义
const columns = [
  {
    type: 'selection'
  },
  {
    title: 'ID',
    key: 'id',
    width: 100
  },
  {
    title: '执行器',
    key: 'handler',
    width: 200,
    ellipsis: {
      tooltip: true
    },
    render(row) {
      return h('code', { class: 'handler-cell' }, row.handler)
    }
  },
  {
    title: '任务描述',
    key: 'description',
    width: 160,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '调度类型',
    key: 'scheduleType',
    width: 140,
    render(row) {
      const typeMap = {
        cron: 'Cron',
        fixed_rate: '固定频率',
        fixed_delay: '固定延迟',
        once: '一次性'
      }
      return h('div', { class: 'schedule-cell' }, [
        h('span', { class: 'schedule-type' }, typeMap[row.scheduleType] || row.scheduleType),
        h('span', { class: 'schedule-expr' }, row.scheduleExpr)
      ])
    }
  },
  {
    title: '运行模式',
    key: 'runMode',
    width: 110,
    render(row) {
      const modeMap = {
        standalone: { label: '单机', class: 'mode-standalone' },
        broadcast: { label: '广播', class: 'mode-broadcast' },
        sharding: { label: '分片', class: 'mode-sharding' },
        map_reduce: { label: 'MapReduce', class: 'mode-mapreduce' }
      }
      const mode = modeMap[row.runMode] || { label: row.runMode, class: '' }
      return h('span', { class: ['run-mode-badge', mode.class] }, mode.label)
    }
  },
  {
    title: '负责人',
    key: 'owner',
    width: 100,
    render(row) {
      return h('span', { class: 'owner-cell' }, row.owner)
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    align: 'center',
    render(row) {
      const isRunning = row.status === 'running'
      return h(NPopconfirm, {
        onPositiveClick: () => handleStatusSwitch(row, !isRunning),
        positiveText: isRunning ? '停止' : '启动',
        negativeText: '取消',
        positiveButtonProps: {
          type: isRunning ? 'warning' : 'primary',
          size: 'small'
        },
        negativeButtonProps: {
          size: 'small',
          quaternary: true
        },
        showIcon: false
      }, {
        trigger: () => h('div', { 
          class: 'status-switch-wrapper'
        }, [
          h('div', { 
            class: ['status-switch', isRunning ? 'active' : '']
          }, [
            h('div', { class: 'switch-dot' })
          ])
        ]),
        default: () => h('div', { 
          style: {
            display: 'flex',
            alignItems: 'flex-start',
            gap: '12px',
            padding: '4px 0'
          }
        }, [
          h('div', { 
            style: {
              flexShrink: 0,
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: isRunning ? 'rgba(245, 158, 11, 0.1)' : 'rgba(94, 129, 244, 0.1)'
            }
          }, [
            h('svg', { 
              viewBox: '0 0 24 24', 
              fill: 'none', 
              stroke: 'currentColor', 
              'stroke-width': '2',
              style: {
                width: '20px',
                height: '20px',
                color: isRunning ? '#f59e0b' : '#5E81F4'
              }
            }, isRunning ? [
              h('circle', { cx: '12', cy: '12', r: '10', fill: 'none' }),
              h('line', { x1: '12', y1: '8', x2: '12', y2: '12', 'stroke-linecap': 'round' }),
              h('circle', { cx: '12', cy: '16', r: '0.5', fill: 'currentColor', stroke: 'none' })
            ] : [
              h('polygon', { points: '5 3 19 12 5 21 5 3' })
            ])
          ]),
          h('div', { 
            style: {
              display: 'flex',
              flexDirection: 'column',
              gap: '4px'
            }
          }, [
            h('div', { 
              style: {
                fontSize: '14px',
                fontWeight: '600',
                color: '#1f2937'
              }
            }, isRunning ? '停止任务' : '启动任务'),
            h('div', { 
              style: {
                fontSize: '13px',
                color: '#6b7280',
                lineHeight: '1.4'
              }
            }, isRunning 
              ? '停止后任务将不再自动执行' 
              : '启动后任务将按计划自动执行')
          ])
        ])
      })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 190,
    fixed: 'right',
    render(row) {
      return h('div', { class: 'action-buttons' }, [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h('button', {
            class: 'action-btn action-btn-secondary',
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
            class: 'action-btn action-btn-primary',
            onClick: () => handleExecute(row)
          }, [
            h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
              h('polygon', { points: '5 3 19 12 5 21 5 3' })
            ])
          ]),
          default: () => '执行'
        }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h('button', {
            class: 'action-btn action-btn-info',
            onClick: () => handleEdit(row)
          }, [
            h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
              h('path', { d: 'M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7' }),
              h('path', { d: 'M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z' })
            ])
          ]),
          default: () => '编辑'
        }),
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row)
        }, {
          trigger: () => h(NTooltip, { trigger: 'hover' }, {
            trigger: () => h('button', {
              class: 'action-btn action-btn-error'
            }, [
              h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
                h('path', { d: 'M3 6h18' }),
                h('path', { d: 'M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2' }),
                h('line', { x1: '10', y1: '11', x2: '10', y2: '17' }),
                h('line', { x1: '14', y1: '11', x2: '14', y2: '17' })
              ])
            ]),
            default: () => '删除'
          }),
          default: () => '确定删除该任务吗？'
        }),
        h(NDropdown, {
          trigger: 'click',
          options: [
            { label: '执行一次', key: 'executeOnce' },
            { label: '下次执行时间', key: 'nextExecuteTime' }
          ],
          onSelect: (key) => handleRowMoreAction(key, row)
        }, {
          default: () => h(NTooltip, { trigger: 'hover' }, {
            trigger: () => h('button', {
              class: 'action-btn action-btn-secondary'
            }, [
              h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
                h('circle', { cx: '12', cy: '12', r: '1' }),
                h('circle', { cx: '19', cy: '12', r: '1' }),
                h('circle', { cx: '5', cy: '12', r: '1' })
              ])
            ]),
            default: () => '更多'
          })
        })
      ])
    }
  }
]

// 事件处理
const handleSearch = () => {
  pagination.page = 1
  message.success('搜索完成')
}

const handleReset = () => {
  filters.keyword = ''
  filters.taskId = ''
  filters.scheduleType = null
  filters.runMode = null
  filters.owner = ''
  filters.status = null
  pagination.page = 1
}

const handleRefresh = () => {
  message.success('刷新成功')
}

const handleMoreAction = (key) => {
  const actionMap = {
    'export': '导出数据',
    'import': '导入任务',
    'batch-enable': '批量启用',
    'batch-disable': '批量禁用'
  }
  message.info(actionMap[key] || key)
}

const handleCreate = () => {
  // 重置表单
  createForm.executor = null
  createForm.description = ''
  createForm.owner = ''
  createForm.alarmEmail = ''
  createForm.scheduleType = 'cron'
  createForm.cronExpr = ''
  createForm.intervalValue = 5
  createForm.intervalUnit = 'minutes'
  createForm.onceTime = null
  createForm.runMode = 'BEAN'
  createForm.handler = ''
  createForm.params = ''
  createForm.routeStrategy = 'FIRST'
  createForm.childJobId = ''
  createForm.misfireStrategy = 'DO_NOTHING'
  createForm.blockStrategy = 'SERIAL_EXECUTION'
  createForm.timeout = 0
  createForm.retryCount = 0
  // 重置弹窗位置
  modalPosition.x = 0
  modalPosition.y = 0
  showCreateModal.value = true
}

const handleSubmitCreate = () => {
  if (!createForm.executor || !createForm.description || !createForm.owner || !createForm.handler) {
    message.warning('请填写必填项')
    return
  }
  // 添加到列表
  // 根据调度类型生成表达式
  let scheduleExpr = ''
  if (createForm.scheduleType === 'cron') {
    scheduleExpr = createForm.cronExpr
  } else if (createForm.scheduleType === 'fixed_rate' || createForm.scheduleType === 'fixed_delay') {
    const unitMap = { seconds: '秒', minutes: '分钟', hours: '小时' }
    scheduleExpr = `每${createForm.intervalValue}${unitMap[createForm.intervalUnit]}`
  } else if (createForm.scheduleType === 'once') {
    scheduleExpr = createForm.onceTime ? new Date(createForm.onceTime).toLocaleString() : '手动触发'
  }
  
  tasks.value.unshift({
    id: 'JOB-' + String(tasks.value.length + 1).padStart(3, '0'),
    handler: createForm.handler,
    description: createForm.description,
    scheduleType: createForm.scheduleType,
    scheduleExpr: scheduleExpr || '手动触发',
    runMode: createForm.routeStrategy === 'SHARDING_BROADCAST' ? 'sharding' : 'standalone',
    owner: createForm.owner,
    status: 'stopped'
  })
  showCreateModal.value = false
  message.success('任务创建成功')
}

const handleCheck = (keys) => {
  checkedRowKeys.value = keys
}

const handleBatchDelete = () => {
  message.warning(`批量删除 ${checkedRowKeys.value.length} 个任务`)
}

const handleExecute = (row) => {
  message.success(`执行任务: ${row.id}`)
}

// 查看详情
const handleViewDetail = (row) => {
  currentTask.value = { ...row }
  detailModalPosition.x = 0
  detailModalPosition.y = 0
  showDetailModal.value = true
}

// 从详情弹窗打开编辑
const openEditFromDetail = () => {
  showDetailModal.value = false
  handleEdit(currentTask.value)
}

// 获取调度类型标签
const getScheduleTypeLabel = (type) => {
  const option = scheduleTypeOptions.find(o => o.value === type)
  return option ? option.label : type
}

// 获取运行模式标签
const getRunModeLabel = (mode) => {
  const option = runModeOptions.find(o => o.value === mode)
  return option ? option.label : mode
}

// 编辑任务
const handleEdit = (row) => {
  currentTask.value = { ...row }
  // 填充编辑表单
  editForm.id = row.id
  editForm.executor = 'sample-executor'
  editForm.description = row.description
  editForm.owner = row.owner
  editForm.scheduleType = row.scheduleType
  
  // 根据调度类型设置对应字段
  if (row.scheduleType === 'cron') {
    editForm.cronExpr = row.scheduleExpr
  } else if (row.scheduleType === 'fixed_rate' || row.scheduleType === 'fixed_delay') {
    // 解析 "每30分钟" 这样的格式
    const match = row.scheduleExpr.match(/每(\d+)(秒|分钟|小时)/)
    if (match) {
      editForm.intervalValue = parseInt(match[1])
      editForm.intervalUnit = match[2] === '秒' ? 'seconds' : match[2] === '分钟' ? 'minutes' : 'hours'
    }
  }
  
  editForm.runMode = row.runMode === 'standalone' ? 'BEAN' : 'GLUE_SHELL'
  editForm.handler = row.handler
  editForm.routeStrategy = 'FIRST'
  editForm.childJobId = ''
  editForm.misfireStrategy = 'DO_NOTHING'
  editForm.blockStrategy = 'SERIAL_EXECUTION'
  editForm.timeout = 0
  editForm.retryCount = 0
  
  editModalPosition.x = 0
  editModalPosition.y = 0
  showEditModal.value = true
}

// 提交编辑
const handleSubmitEdit = () => {
  if (!editForm.executor || !editForm.description || !editForm.owner || !editForm.handler) {
    message.warning('请填写必填项')
    return
  }
  
  // 更新列表中的数据
  const index = tasks.value.findIndex(t => t.id === editForm.id)
  if (index > -1) {
    // 根据调度类型生成表达式
    let scheduleExpr = ''
    if (editForm.scheduleType === 'cron') {
      scheduleExpr = editForm.cronExpr
    } else if (editForm.scheduleType === 'fixed_rate' || editForm.scheduleType === 'fixed_delay') {
      const unitMap = { seconds: '秒', minutes: '分钟', hours: '小时' }
      scheduleExpr = `每${editForm.intervalValue}${unitMap[editForm.intervalUnit]}`
    } else if (editForm.scheduleType === 'once') {
      scheduleExpr = editForm.onceTime ? new Date(editForm.onceTime).toLocaleString() : '手动触发'
    }
    
    tasks.value[index] = {
      ...tasks.value[index],
      handler: editForm.handler,
      description: editForm.description,
      scheduleType: editForm.scheduleType,
      scheduleExpr: scheduleExpr || '手动触发',
      runMode: editForm.routeStrategy === 'SHARDING_BROADCAST' ? 'sharding' : 'standalone',
      owner: editForm.owner
    }
  }
  
  showEditModal.value = false
  message.success('任务更新成功')
}

const handleToggleStatus = (row) => {
  const action = row.status === 'running' ? '暂停' : '启动'
  message.success(`${action}任务: ${row.id}`)
}


const handleDelete = (row) => {
  const index = tasks.value.findIndex(t => t.id === row.id)
  if (index > -1) {
    tasks.value.splice(index, 1)
    message.success(`删除任务: ${row.id}`)
  }
}

const handleRowMoreAction = (key, row) => {
  if (key === 'executeOnce') {
    message.success(`执行一次任务: ${row.name}`)
  } else if (key === 'nextExecuteTime') {
    // 计算下次执行时间（示例）
    const nextTime = new Date()
    nextTime.setMinutes(nextTime.getMinutes() + 5)
    const timeStr = nextTime.toLocaleString('zh-CN', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
    message.info(`任务 "${row.name}" 下次执行时间: ${timeStr}`)
  }
}
</script>

<style scoped>
.task-management {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ==================== 工具栏样式 ==================== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-radius: 10px 10px 0 0;
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
  width: 240px;
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

.tool-btn:hover svg {
  color: var(--primary-color);
}

.tool-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tool-btn:disabled:hover {
  background: transparent;
  color: var(--text-secondary);
}

.tool-divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
  margin: 0 6px;
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

/* 去掉下拉框聚焦时的蓝色边框 */
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
  border-radius: 0 0 10px 10px;
  padding: 16px 20px;
}

/* 分页样式 */
.task-table :deep(.n-data-table__pagination) {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.task-table :deep(.n-pagination-prefix) {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

/* 任务处理器单元格 */
.task-table :deep(.handler-cell) {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  background: transparent;
}

/* 任务描述 */
.task-table :deep(.n-data-table-td .n-ellipsis) {
  color: var(--text-secondary);
  font-weight: 500;
}

/* 调度类型单元格 */
.task-table :deep(.schedule-cell) {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-table :deep(.schedule-type) {
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.task-table :deep(.schedule-expr) {
  display: inline-block;
  font-size: 0.75rem;
  color: #8b95a5;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  background: #f4f6f8;
  padding: 4px 10px;
  border-radius: 6px;
}

/* 运行模式标签 */
.task-table :deep(.run-mode-badge) {
  font-size: 0.75rem;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-block;
}

.task-table :deep(.mode-standalone) {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.task-table :deep(.mode-broadcast) {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.task-table :deep(.mode-sharding) {
  background: rgba(236, 72, 153, 0.1);
  color: #ec4899;
}

.task-table :deep(.mode-mapreduce) {
  background: rgba(14, 165, 233, 0.1);
  color: #0ea5e9;
}

/* 负责人 */
.task-table :deep(.owner-cell) {
  font-weight: 500;
  color: var(--text-secondary);
}

/* 操作按钮 */
.task-table :deep(.action-buttons) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-table :deep(.action-btn) {
  background: transparent;
  border: none;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.task-table :deep(.action-btn svg) {
  width: 16px;
  height: 16px;
}

.task-table :deep(.action-btn-secondary) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-secondary:hover) {
  color: #8b5cf6;
}

.task-table :deep(.action-btn-primary) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-primary:hover) {
  color: #5E81F4;
}

.task-table :deep(.action-btn-info) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-info:hover) {
  color: #3b82f6;
}

.task-table :deep(.action-btn-success) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-success:hover) {
  color: #10b981;
}

.task-table :deep(.action-btn-warning) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-warning:hover) {
  color: #f59e0b;
}

.task-table :deep(.action-btn-error) {
  color: var(--text-secondary);
}

.task-table :deep(.action-btn-error:hover) {
  color: #ef4444;
}

/* 响应式 */
@media (max-width: 900px) {
  .toolbar {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .toolbar-left {
    order: 1;
    width: 100%;
  }
  
  .search-box {
    flex: 1;
  }
  
  .toolbar-right {
    order: 2;
    width: 100%;
    justify-content: flex-end;
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
}

@media (max-width: 640px) {
  .toolbar {
    padding: 12px 16px;
  }
  
  .search-box {
    width: 100%;
  }
  
  .table-section {
    padding: 12px 16px;
  }
}

/* ==================== 弹窗样式 ==================== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.modal-container {
  width: 860px;
  max-width: 90vw;
  max-height: 90vh;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.15), 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* 弹窗动画 */
.modal-enter-active {
  animation: modal-overlay-in 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-leave-active {
  animation: modal-overlay-out 0.2s cubic-bezier(0.4, 0, 1, 1);
}

.modal-enter-active .modal-container {
  animation: modal-content-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-leave-active .modal-container {
  animation: modal-content-out 0.2s cubic-bezier(0.4, 0, 1, 1);
}

@keyframes modal-overlay-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes modal-overlay-out {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

@keyframes modal-content-in {
  0% {
    opacity: 0;
    transform: scale(0.9);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes modal-content-out {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.95);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: linear-gradient(to bottom, #fafbfc, #fff);
  border-bottom: 1px solid #f0f0f0;
}

.modal-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.9;
}

.modal-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #555;
  letter-spacing: 0.01em;
}

.modal-close {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover {
  background: #f5f5f5;
}

.modal-close svg {
  width: 16px;
  height: 16px;
  color: #999;
  transition: color 0.15s ease;
}

.modal-close:hover svg {
  color: #666;
}

.modal-body {
  padding: 20px 24px;
  max-height: 65vh;
  overflow-y: auto;
  background: #fff;
}

.form-section {
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #f5f5f5;
}

.form-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.form-section-title {
  font-size: 0.75rem;
  font-weight: 500;
  color: #999;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
  letter-spacing: 0.02em;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-item.full {
  flex: none;
  width: 100%;
}

.form-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: #777;
}

.form-label.required::after {
  content: '*';
  color: #f87171;
  margin-left: 3px;
}

.form-item :deep(.n-input),
.form-item :deep(.n-select),
.form-item :deep(.n-input-number) {
  --n-height: 34px;
  --n-font-size: 0.8125rem;
  --n-border-radius: 8px;
}

.form-item :deep(.n-input),
.form-item :deep(.n-input-number) {
  --n-border: 1px solid #e8e8e8;
  --n-border-hover: 1px solid #d0d0d0;
  --n-border-focus: 1px solid var(--primary-color);
  --n-box-shadow-focus: 0 0 0 2px rgba(107, 141, 245, 0.1);
}

.form-item :deep(.n-base-selection) {
  --n-border: 1px solid #e8e8e8 !important;
  --n-border-hover: 1px solid #d0d0d0 !important;
  --n-border-active: 1px solid var(--primary-color) !important;
}

.form-item :deep(.n-input-wrapper) {
  padding: 0 12px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background: linear-gradient(to top, #fafbfc, #fff);
}

.modal-footer :deep(.n-button) {
  --n-height: 34px;
  --n-padding: 0 20px;
  --n-font-size: 0.8125rem;
  --n-border-radius: 8px;
  font-weight: 500;
}

.modal-footer :deep(.n-button--default-type) {
  --n-border: 1px solid #e0e0e0;
  --n-text-color: #666;
}

.modal-footer :deep(.n-button--default-type:hover) {
  --n-border: 1px solid #d0d0d0;
  --n-text-color: #444;
}

/* ==================== 详情弹窗样式 ==================== */
.detail-modal {
  width: 720px;
}

.code-text {
  font-family: 'Consolas', 'Monaco', monospace;
  background: var(--bg-secondary);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.8125rem;
}

/* ==================== Cron输入框样式 ==================== */
.cron-input-wrapper {
  display: flex;
  gap: 8px;
}

.cron-input-wrapper .n-input {
  flex: 1;
}

.interval-input-wrapper {
  display: flex;
  gap: 8px;
}

.cron-picker-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  color: #5E81F4;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.cron-picker-btn:hover {
  border-color: #5E81F4;
  background: rgba(94, 129, 244, 0.05);
}

.cron-picker-btn svg {
  width: 18px;
  height: 18px;
}

/* ==================== Cron选择器弹窗样式 ==================== */
.cron-picker-modal {
  width: 620px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.12), 0 10px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.04);
  max-width: 90vw;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: modalIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.cron-picker-body {
  padding: 20px 24px 24px;
}

/* Tab 导航 */
.cron-tabs {
  display: flex;
  background: #f8fafc;
  border-radius: 10px;
  padding: 4px;
  gap: 4px;
  margin-bottom: 20px;
}

.cron-tab {
  flex: 1;
  padding: 10px 6px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.cron-tab:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.6);
}

.cron-tab.active {
  color: #5E81F4;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.cron-tab .tab-value {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 0.625rem;
  color: var(--text-muted);
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cron-tab.active .tab-value {
  background: linear-gradient(135deg, rgba(94, 129, 244, 0.15), rgba(94, 129, 244, 0.08));
  color: #5E81F4;
}

/* Tab 内容 */
.cron-tab-content {
  min-height: 200px;
  padding: 4px;
}

.tab-panel {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.panel-options {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.875rem;
  color: var(--text-primary);
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  margin: 0 -12px;
  transition: background 0.15s ease;
}

.radio-item:hover {
  background: #f8fafc;
}

.radio-item input[type="radio"] {
  width: 18px;
  height: 18px;
  accent-color: #5E81F4;
  cursor: pointer;
}

/* 数值选择网格 */
.value-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 8px;
  padding: 16px;
  background: linear-gradient(to bottom, #fafbfc, #f5f7f9);
  border-radius: 10px;
  border: 1px solid #eef0f5;
  max-height: 180px;
  overflow-y: auto;
}

.value-grid::-webkit-scrollbar {
  width: 6px;
}

.value-grid::-webkit-scrollbar-track {
  background: transparent;
}

.value-grid::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 3px;
}

.value-grid.cols-12 {
  grid-template-columns: repeat(12, 1fr);
}

.value-grid.cols-10 {
  grid-template-columns: repeat(10, 1fr);
}

.value-grid.cols-7 {
  grid-template-columns: repeat(7, 1fr);
}

.value-grid.cols-6 {
  grid-template-columns: repeat(6, 1fr);
}

.value-grid.cols-5 {
  grid-template-columns: repeat(5, 1fr);
}

.checkbox-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  background: #fff;
  border: 1.5px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.checkbox-item:hover {
  border-color: #5E81F4;
  color: #5E81F4;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(94, 129, 244, 0.15);
}

.checkbox-item:has(input:checked) {
  background: linear-gradient(135deg, #5E81F4, #4F6CE5);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 2px 8px rgba(94, 129, 244, 0.3);
}

.checkbox-item input[type="checkbox"] {
  display: none;
}

/* 预览 */
.cron-preview {
  background: linear-gradient(to bottom, #f8fafc, #f3f5f9);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #eef0f5;
  margin-top: 20px;
}

.cron-preview .preview-fields {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-bottom: 16px;
}

.cron-preview .field {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: #fff;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-primary);
  min-width: 50px;
  transition: all 0.2s ease;
}

.cron-preview .field em {
  font-style: normal;
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cron-preview .field.active {
  border-color: #5E81F4;
  background: linear-gradient(to bottom, rgba(94, 129, 244, 0.08), rgba(94, 129, 244, 0.03));
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(94, 129, 244, 0.15);
}

.cron-preview .field.active em {
  color: #5E81F4;
}

.cron-preview .preview-expression {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 1.125rem;
  font-weight: 600;
  color: #5E81F4;
  text-align: center;
  margin-bottom: 8px;
  letter-spacing: 1px;
  padding: 8px 16px;
  background: #fff;
  border-radius: 8px;
  display: inline-block;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
  box-sizing: border-box;
}

.cron-preview .preview-desc {
  font-size: 0.875rem;
  color: var(--text-secondary);
  text-align: center;
  padding-top: 8px;
  border-top: 1px dashed #e5e7eb;
}

/* ==================== 状态开关样式 ==================== */
.task-table :deep(.status-switch-wrapper) {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.task-table :deep(.status-switch) {
  position: relative;
  width: 44px;
  height: 22px;
  background: #e0e0e0;
  border-radius: 11px;
  transition: all 0.25s ease;
}

.task-table :deep(.status-switch .switch-dot) {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  transition: all 0.25s ease;
}

.task-table :deep(.status-switch.active) {
  background: linear-gradient(135deg, #7c9cff, #5E81F4);
}

.task-table :deep(.status-switch.active .switch-dot) {
  left: 24px;
}

.task-table :deep(.status-switch:hover) {
  box-shadow: 0 0 0 2px rgba(94, 129, 244, 0.15);
}

</style>
