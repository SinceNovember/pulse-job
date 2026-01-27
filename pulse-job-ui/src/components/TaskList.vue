<template>
  <div class="task-list">
    <TaskItem 
      v-for="(task, index) in tasks" 
      :key="task.id"
      :task="task"
      :style="{ animationDelay: `${index * 0.05}s` }"
      @toggle="$emit('toggle-task', task.id)"
    />
  </div>
</template>

<script setup>
import TaskItem from './TaskItem.vue'

defineProps({
  tasks: Array
})

defineEmits(['toggle-task'])
</script>

<style scoped>
.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
