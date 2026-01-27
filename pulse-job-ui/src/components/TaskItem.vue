<template>
  <div class="task-item" :class="{ completed: task.completed }">
    <n-checkbox 
      :checked="task.completed"
      size="medium"
      @update:checked="$emit('toggle')"
    />
    <div class="task-content">
      <span class="task-title">{{ task.title }}</span>
    </div>
    <div class="task-meta">
      <div class="meta-item" title="子任务">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="8" y1="6" x2="21" y2="6"/>
          <line x1="8" y1="12" x2="21" y2="12"/>
          <line x1="8" y1="18" x2="21" y2="18"/>
          <line x1="3" y1="6" x2="3.01" y2="6"/>
          <line x1="3" y1="12" x2="3.01" y2="12"/>
          <line x1="3" y1="18" x2="3.01" y2="18"/>
        </svg>
        <span>{{ task.subtasks.current }}/{{ task.subtasks.total }}</span>
      </div>
      <div class="meta-item" title="附件">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
        </svg>
        <span>{{ task.attachments }}</span>
      </div>
      <div class="meta-item" title="评论">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <span>{{ task.comments }}</span>
      </div>
    </div>
    <n-tag :type="statusType" size="small" round>
      {{ statusLabel }}
    </n-tag>
    <n-avatar :size="36" round :style="{ background: avatarColor }">
      {{ avatarInitial }}
    </n-avatar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { NCheckbox, NTag, NAvatar } from 'naive-ui'

const props = defineProps({
  task: Object
})

defineEmits(['toggle'])

const statusConfig = {
  completed: { label: '完毕', type: 'success' },
  pending: { label: '待办的', type: 'warning' },
  waiting: { label: '等候接听', type: 'info' }
}

const statusLabel = computed(() => statusConfig[props.task.status]?.label || props.task.status)
const statusType = computed(() => statusConfig[props.task.status]?.type || 'default')

const avatarColors = ['#5E81F4', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']
const avatarColor = computed(() => avatarColors[props.task.id % avatarColors.length])
const avatarInitial = computed(() => `U${props.task.id % 10}`)
</script>

<style scoped>
.task-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-card);
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.15s ease;
  animation: slideIn 0.3s ease forwards;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.task-item:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.task-item.completed {
  opacity: 0.7;
}

.task-item.completed .task-title {
  text-decoration: line-through;
  color: var(--text-muted);
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--text-primary);
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.meta-item svg {
  width: 14px;
  height: 14px;
}

@media (max-width: 640px) {
  .task-item {
    flex-wrap: wrap;
    padding: 14px 16px;
  }

  .task-meta {
    width: 100%;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--border-light);
  }
}
</style>
