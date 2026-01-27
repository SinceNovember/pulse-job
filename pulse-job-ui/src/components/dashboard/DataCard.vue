<template>
  <n-card class="data-card" :bordered="false">
    <template #header>
      <div class="card-header">
        <h3>{{ title }}</h3>
        <div class="card-header-extra">
          <slot name="extra">
            <n-button 
              v-if="showViewAll" 
              text 
              type="primary" 
              @click="$emit('view-all')"
            >
              查看全部
            </n-button>
          </slot>
        </div>
      </div>
    </template>
    <slot></slot>
  </n-card>
</template>

<script setup>
import { NCard, NButton } from 'naive-ui'

defineProps({
  title: {
    type: String,
    required: true
  },
  showViewAll: {
    type: Boolean,
    default: false
  }
})

defineEmits(['view-all'])
</script>

<style scoped>
.data-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.data-card :deep(.n-card-header) {
  padding: 20px 24px 0;
}

.data-card :deep(.n-card__content) {
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

.card-header-extra :deep(.n-button) {
  font-size: 0.875rem;
}
</style>
