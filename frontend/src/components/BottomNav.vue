<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const items = [
  { name: 'home', path: '/', label: '首页', icon: 'home' },
  { name: 'chat', path: '/chat', label: '聊天', icon: 'chat' },
  { name: 'tasks', path: '/tasks', label: '任务', icon: 'task' },
  { name: 'diary', path: '/diary', label: '日记', icon: 'book' },
  { name: 'settings', path: '/settings', label: '我的', icon: 'heart' },
]

function isActive(name: string) {
  if (name === 'home') return route.name === 'home'
  if (name === 'tasks') return route.name === 'tasks' || route.name === 'task-detail'
  return route.name === name
}
</script>

<template>
  <nav class="bottom-nav" aria-label="主导航">
    <RouterLink
      v-for="item in items"
      :key="item.name"
      :to="item.path"
      class="nav-item"
      :class="{ active: isActive(item.name) }"
    >
      <span class="nav-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
          <template v-if="item.icon === 'home'">
            <path d="M4 11.5 12 4l8 7.5" />
            <path d="M6.5 10v9h11v-9" />
            <path d="M10 19v-4.5h4V19" />
          </template>
          <template v-else-if="item.icon === 'chat'">
            <path d="M4.5 6.5h15v9.5h-8.5L6 20v-4H4.5z" />
            <circle cx="9" cy="11" r="0.9" fill="currentColor" stroke="none" />
            <circle cx="12.5" cy="11" r="0.9" fill="currentColor" stroke="none" />
            <circle cx="16" cy="11" r="0.9" fill="currentColor" stroke="none" />
          </template>
          <template v-else-if="item.icon === 'task'">
            <rect x="4.5" y="4.5" width="15" height="16" rx="3" />
            <path d="M8.5 10.5 10.5 12.5 15.5 7.8" />
            <path d="M8.5 16h7" />
          </template>
          <template v-else-if="item.icon === 'book'">
            <path d="M12 5.5c-2-1.4-4.6-1.6-7-1v14c2.4-.6 5-.4 7 1 2-1.4 4.6-1.6 7-1v-14c-2.4-.6-5-.4-7 1z" />
            <path d="M12 5.5v14" />
          </template>
          <template v-else>
            <path d="M12 20s-7.2-4.4-7.2-9.2A4.1 4.1 0 0 1 12 8.1a4.1 4.1 0 0 1 7.2 2.7C19.2 15.6 12 20 12 20z" />
          </template>
        </svg>
      </span>
      <span class="nav-label">{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped lang="scss">
.bottom-nav {
  position: fixed;
  left: 50%;
  bottom: max(14px, env(safe-area-inset-bottom));
  transform: translateX(-50%);
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 44px rgba(100, 134, 172, 0.24);
  backdrop-filter: blur(16px);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  min-width: 62px;
  padding: 7px 10px 6px;
  border-radius: 999px;
  color: var(--ink-faint);
  transition: color 0.2s, background 0.2s, transform 0.2s;
}

.nav-item:hover {
  color: var(--sky-ink);
}

.nav-item.active {
  color: #2f6d95;
  background: linear-gradient(135deg, rgba(169, 216, 247, 0.55), rgba(214, 245, 228, 0.7));
  transform: translateY(-1px);
}

.nav-icon svg {
  width: 22px;
  height: 22px;
  display: block;
}

.nav-label {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.3px;
}

@media (max-width: 380px) {
  .nav-item {
    min-width: 54px;
    padding: 6px 6px 4px;
  }
}
</style>