<template>
  <n-list class="alerts-list" :show-divider="false">
    <n-list-item v-for="alert in alerts" :key="alert.id" class="alert-item">
      <div class="alert-content" :class="alert.level">
        <div class="alert-icon">
          <n-icon :size="16">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </n-icon>
        </div>
        <div class="alert-info">
          <span class="alert-message">{{ alert.message }}</span>
          <span class="alert-time">{{ alert.time }}</span>
        </div>
      </div>
    </n-list-item>
  </n-list>
</template>

<script setup>
import { NList, NListItem, NIcon } from 'naive-ui'

defineProps({
  alerts: {
    type: Array,
    required: true
    // [{ id: 1, level: 'warning' | 'error', message: '...', time: '3分钟前' }]
  }
})
</script>

<style scoped>
.alerts-list {
  background: transparent;
}

.alerts-list :deep(.n-list-item) {
  padding: 0;
}

.alerts-list :deep(.n-list-item__main) {
  width: 100%;
}

.alert-item {
  margin-bottom: 12px;
}

.alert-item:last-child {
  margin-bottom: 0;
}

.alert-content {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  background: var(--bg-main);
}

.alert-content.warning {
  border-left: 3px solid #f1c40f;
}

.alert-content.error {
  border-left: 3px solid #e74c3c;
}

.alert-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-content.warning .alert-icon {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}

.alert-content.error .alert-icon {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.alert-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alert-message {
  font-size: 0.8125rem;
  color: var(--text-primary);
  line-height: 1.4;
}

.alert-time {
  font-size: 0.75rem;
  color: var(--text-muted);
}
</style>
