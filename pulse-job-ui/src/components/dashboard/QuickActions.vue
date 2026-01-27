<template>
  <n-card class="quick-actions-card" :bordered="false">
    <template #header>
      <h3>快捷操作</h3>
    </template>
    <div class="quick-actions-grid">
      <n-button 
        v-for="action in actions" 
        :key="action.key"
        class="quick-action-btn"
        quaternary
        @click="$emit('action', action.key)"
      >
        <div class="action-content">
          <div class="action-icon" :class="action.iconType">
            <n-icon :size="20">
              <component :is="action.icon" />
            </n-icon>
          </div>
          <span class="action-label">{{ action.label }}</span>
        </div>
      </n-button>
    </div>
  </n-card>
</template>

<script setup>
import { h } from 'vue'
import { NCard, NButton, NIcon } from 'naive-ui'

defineProps({
  actions: {
    type: Array,
    required: true
    // [{ key: 'new-task', label: '新建任务', icon: Component, iconType: 'new-task' }]
  }
})

defineEmits(['action'])
</script>

<style scoped>
.quick-actions-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.quick-actions-card :deep(.n-card-header) {
  padding: 20px 24px 0;
}

.quick-actions-card :deep(.n-card-header__main) h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.quick-actions-card :deep(.n-card__content) {
  padding: 16px 24px 24px;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-action-btn {
  padding: 0;
  height: auto;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--bg-main);
  transition: all 0.15s ease;
}

.quick-action-btn:hover {
  background: var(--bg-hover);
  border-color: transparent;
}

.action-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 16px;
  width: 100%;
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon.new-task {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.action-icon.new-node {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.action-icon.schedule {
  background: rgba(155, 89, 182, 0.1);
  color: #9b59b6;
}

.action-icon.logs {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}

.action-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.quick-action-btn:hover .action-label {
  color: var(--text-primary);
}
</style>
