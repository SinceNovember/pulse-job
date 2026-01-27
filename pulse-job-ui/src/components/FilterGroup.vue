<template>
  <div class="filter-group">
    <button 
      class="filter-btn"
      :class="{ active: activeFilter === 'all' }"
      @click="$emit('filter-change', 'all')"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="4" y1="21" x2="4" y2="14"/>
        <line x1="4" y1="10" x2="4" y2="3"/>
        <line x1="12" y1="21" x2="12" y2="12"/>
        <line x1="12" y1="8" x2="12" y2="3"/>
        <line x1="20" y1="21" x2="20" y2="16"/>
        <line x1="20" y1="12" x2="20" y2="3"/>
      </svg>
      全部
    </button>
    <button 
      class="filter-btn"
      :class="{ active: activeFilter === 'completed' }"
      @click="$emit('filter-change', 'completed')"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
        <polyline points="14 2 14 8 20 8"/>
        <line x1="16" y1="13" x2="8" y2="13"/>
        <line x1="16" y1="17" x2="8" y2="17"/>
      </svg>
      完毕
    </button>
    <button 
      class="filter-btn"
      :class="{ active: activeFilter === 'tracking' }"
      @click="$emit('filter-change', 'tracking')"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <polyline points="12 6 12 12 16 14"/>
      </svg>
      跟踪
    </button>
  </div>
</template>

<script setup>
defineProps({
  filters: Array,
  activeFilter: String
})

defineEmits(['filter-change'])
</script>

<style scoped>
.filter-group {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 24px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-btn:hover {
  border-color: var(--primary-light);
  color: var(--primary-color);
}

.filter-btn.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.filter-btn svg {
  width: 16px;
  height: 16px;
}

@media (max-width: 640px) {
  .filter-group {
    flex-wrap: wrap;
  }

  .filter-btn {
    padding: 8px 16px;
    font-size: 0.8125rem;
  }
}
</style>
