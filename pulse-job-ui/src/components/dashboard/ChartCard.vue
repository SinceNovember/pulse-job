<template>
  <n-card class="chart-card" :bordered="false">
    <template #header>
      <div class="card-header">
        <h3>{{ title }}</h3>
        <div class="card-actions">
          <button
            v-for="period in periods"
            :key="period"
            :class="['period-btn', { active: activePeriod === period }]"
            @click="handlePeriodChange(period)"
          >
            {{ period }}
          </button>
        </div>
      </div>
    </template>
    <div class="chart-container">
      <div class="chart-bars">
        <div 
          v-for="(bar, index) in data" 
          :key="index" 
          class="chart-bar-wrapper"
        >
          <n-tooltip trigger="hover">
            <template #trigger>
              <div 
                class="chart-bar" 
                :style="{ height: bar.value + '%' }"
              ></div>
            </template>
            {{ bar.count }} 个任务
          </n-tooltip>
          <span class="bar-label">{{ bar.label }}</span>
        </div>
      </div>
    </div>
  </n-card>
</template>

<script setup>
import { NCard, NTooltip } from 'naive-ui'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  periods: {
    type: Array,
    default: () => ['日', '周', '月']
  },
  activePeriod: {
    type: String,
    default: '周'
  },
  data: {
    type: Array,
    required: true
    // [{ label: '周一', value: 65, count: 234 }]
  }
})

const emit = defineEmits(['period-change'])

const handlePeriodChange = (period) => {
  emit('period-change', period)
}
</script>

<style scoped>
.chart-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.chart-card :deep(.n-card-header) {
  padding: 20px 24px 0;
}

.chart-card :deep(.n-card__content) {
  padding: 20px 24px 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

/* 周期切换按钮 - 原版样式 */
.card-actions {
  display: flex;
  gap: 4px;
  background: var(--bg-main);
  padding: 4px;
  border-radius: 8px;
}

.period-btn {
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.period-btn:hover {
  color: var(--text-primary);
}

.period-btn.active {
  background: var(--bg-card);
  color: var(--primary-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.chart-container {
  height: 240px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  width: 100%;
  height: 100%;
  padding-bottom: 30px;
}

.chart-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.chart-bar {
  width: 100%;
  max-width: 48px;
  background: linear-gradient(180deg, #5E81F4 0%, #7B9CF5 100%);
  border-radius: 6px 6px 0 0;
  transition: all 0.3s ease;
  cursor: pointer;
  min-height: 4px;
}

.chart-bar:hover {
  background: linear-gradient(180deg, #4A6CD4 0%, #5E81F4 100%);
  transform: scaleY(1.02);
}

.bar-label {
  margin-top: 12px;
  font-size: 0.75rem;
  color: var(--text-muted);
}
</style>
