<template>
  <n-list class="nodes-list" :show-divider="false">
    <n-list-item v-for="node in nodes" :key="node.id" class="node-item">
      <div class="node-content">
        <n-avatar 
          :size="40" 
          round
          :class="['node-avatar', node.status]"
        >
          {{ node.name.charAt(0) }}
        </n-avatar>
        <div class="node-details">
          <span class="node-name">{{ node.name }}</span>
          <span class="node-meta">{{ node.ip }} · CPU {{ node.cpu }}%</span>
        </div>
        <div class="node-status-indicator" :class="node.status"></div>
      </div>
    </n-list-item>
  </n-list>
</template>

<script setup>
import { NList, NListItem, NAvatar } from 'naive-ui'

defineProps({
  nodes: {
    type: Array,
    required: true
    // [{ id: 1, name: 'Worker-01', ip: '192.168.1.101', cpu: 45, status: 'online' }]
  }
})
</script>

<style scoped>
.nodes-list {
  background: transparent;
}

.nodes-list :deep(.n-list-item) {
  padding: 0;
}

.nodes-list :deep(.n-list-item__main) {
  width: 100%;
}

.node-item {
  margin-bottom: 12px;
}

.node-item:last-child {
  margin-bottom: 0;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-main);
  border-radius: 10px;
  transition: background 0.15s ease;
}

.node-content:hover {
  background: var(--bg-hover);
}

.node-avatar {
  font-weight: 600;
  font-size: 0.875rem;
  color: white;
}

.node-avatar.online {
  background: linear-gradient(135deg, #2ecc71 0%, #27ae60 100%);
}

.node-avatar.offline {
  background: linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%);
}

.node-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
}

.node-meta {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.node-status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.node-status-indicator.online {
  background: #2ecc71;
  box-shadow: 0 0 8px rgba(46, 204, 113, 0.4);
}

.node-status-indicator.offline {
  background: #95a5a6;
}
</style>
