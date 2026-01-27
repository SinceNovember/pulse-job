<template>
  <div class="add-task-container">
    <n-checkbox disabled size="medium" />
    <n-input 
      v-model:value="taskTitle"
      placeholder="输入以添加新任务......"
      :bordered="false"
      size="medium"
      @keypress.enter="handleSubmit"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { NInput, NCheckbox } from 'naive-ui'

const emit = defineEmits(['add-task'])

const taskTitle = ref('')

const handleSubmit = () => {
  if (taskTitle.value.trim()) {
    emit('add-task', taskTitle.value.trim())
    taskTitle.value = ''
  }
}
</script>

<style scoped>
.add-task-container {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: var(--bg-card);
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.add-task-container :deep(.n-input) {
  --n-border: none;
  --n-border-hover: none;
  --n-border-focus: none;
  --n-box-shadow-focus: none;
  --n-color: transparent;
  --n-color-focus: transparent;
}

.add-task-container :deep(.n-input .n-input__input-el) {
  font-size: 0.9375rem;
}
</style>
