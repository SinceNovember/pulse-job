<template>
  <n-card class="stats-card" :bordered="false">
    <div class="stats-card-content">
      <div class="stat-icon" :class="iconType">
        <slot name="icon">
          <n-icon :size="24">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
            </svg>
          </n-icon>
        </slot>
      </div>
      <div class="stat-content">
        <n-statistic :value="value" class="stat-value">
          <template #suffix>
            <span v-if="suffix" class="stat-suffix">{{ suffix }}</span>
          </template>
        </n-statistic>
        <span class="stat-label">{{ label }}</span>
      </div>
      <div v-if="trend" class="stat-trend" :class="trend.type">
        <n-icon :size="16">
          <svg v-if="trend.type === 'up'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M7 17l5-5 5 5"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M7 7l5 5 5-5"/>
          </svg>
        </n-icon>
        <span>{{ trend.value }}</span>
      </div>
    </div>
  </n-card>
</template>

<script setup>
import { NCard, NStatistic, NIcon } from 'naive-ui'

defineProps({
  value: {
    type: [Number, String],
    required: true
  },
  label: {
    type: String,
    required: true
  },
  suffix: {
    type: String,
    default: ''
  },
  iconType: {
    type: String,
    default: 'default' // nodes, tasks, success, queue
  },
  trend: {
    type: Object,
    default: null // { type: 'up' | 'down', value: '+3' }
  }
})
</script>

<style scoped>
.stats-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stats-card :deep(.n-card__content) {
  padding: 20px 24px;
}

.stats-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon :deep(svg) {
  width: 24px;
  height: 24px;
}

.stat-icon.nodes {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.stat-icon.tasks {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.stat-icon.success {
  background: rgba(155, 89, 182, 0.1);
  color: #9b59b6;
}

.stat-icon.queue {
  background: rgba(241, 196, 15, 0.1);
  color: #f1c40f;
}

.stat-icon.default {
  background: rgba(94, 129, 244, 0.1);
  color: #5E81F4;
}

.stat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value :deep(.n-statistic-value) {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.stat-value :deep(.n-statistic-value__content) {
  font-size: 1.5rem;
  font-weight: 700;
}

.stat-suffix {
  font-size: 1rem;
  font-weight: 500;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.875rem;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
}

.stat-trend.up {
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
}

.stat-trend.down {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}
</style>
