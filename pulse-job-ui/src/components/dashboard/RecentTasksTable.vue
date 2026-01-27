<template>
  <div class="table-wrapper">
    <n-data-table
      :columns="columns"
      :data="tasks"
      :bordered="false"
      :bottom-bordered="false"
      :single-line="false"
      :pagination="false"
      class="tasks-table"
    />
  </div>
</template>

<script setup>
import { h } from 'vue'
import { NDataTable } from 'naive-ui'

defineProps({
  tasks: {
    type: Array,
    required: true
  }
})

const statusConfig = {
  success: { label: '成功', class: 'status-badge success' },
  running: { label: '运行中', class: 'status-badge running' },
  failed: { label: '失败', class: 'status-badge failed' },
  pending: { label: '等待中', class: 'status-badge pending' }
}

const columns = [
  {
    title: '任务名称',
    key: 'name',
    width: 180,
    render(row) {
      return h('div', { class: 'task-info' }, [
        h('span', { class: 'task-name' }, row.name),
        h('span', { class: 'task-id' }, row.id)
      ])
    }
  },
  {
    title: '执行节点',
    key: 'node',
    width: 160,
    render(row) {
      return h('div', { class: 'node-info' }, [
        h('span', { class: 'node-name' }, row.node),
        h('span', { class: 'node-ip' }, row.nodeIp)
      ])
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      const config = statusConfig[row.status] || { label: row.status, class: 'status-badge' }
      return h('span', { class: config.class }, config.label)
    }
  },
  {
    title: '耗时',
    key: 'duration',
    width: 100,
    render(row) {
      return h('span', { class: 'duration-text' }, row.duration)
    }
  },
  {
    title: '完成时间',
    key: 'completedAt',
    width: 120,
    render(row) {
      return h('span', { class: 'time-text' }, row.completedAt)
    }
  }
]
</script>

<style scoped>
.table-wrapper {
  overflow-x: auto;
}

/* 自定义单元格样式 */
.tasks-table :deep(.task-info),
.tasks-table :deep(.node-info) {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tasks-table :deep(.task-name),
.tasks-table :deep(.node-name) {
  font-weight: 500;
  color: var(--text-primary);
}

.tasks-table :deep(.task-id),
.tasks-table :deep(.node-ip) {
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* 状态标签样式 */
.tasks-table :deep(.status-badge) {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

.tasks-table :deep(.status-badge.success) {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.tasks-table :deep(.status-badge.running) {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.tasks-table :deep(.status-badge.failed) {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.tasks-table :deep(.status-badge.pending) {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}
</style>
