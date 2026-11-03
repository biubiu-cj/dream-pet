<script setup lang="ts">
import { computed } from 'vue'
import type { Task } from '@/api/types'
import { categoryMeta, DIFFICULTY_LABELS, STATUS_META } from '@/constants/labels'

const props = withDefaults(
  defineProps<{
    task: Task
    compact?: boolean
    clickable?: boolean
  }>(),
  { compact: false, clickable: true },
)

const meta = computed(() => categoryMeta(props.task.category))
const stars = computed(() => Math.min(3, Math.max(1, props.task.difficulty)))
</script>

<template>
  <article
    class="task-card"
    :class="{ compact, done: task.status === 'completed', clickable }"
    :style="{ '--accent': meta.color, '--accent-bg': meta.bg }"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    @click="clickable && $emit('open', task)"
    @keydown.enter="clickable && $emit('open', task)"
  >
    <div class="task-icon" aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <template v-if="task.category === 'knowledge'">
          <path d="M12 6.5c-1.8-1.3-4.2-1.5-6.5-1v12c2.3-.5 4.7-.3 6.5 1 1.8-1.3 4.2-1.5 6.5-1v-12c-2.3-.5-4.7-.3-6.5 1z" />
          <path d="M12 6.5v12" />
        </template>
        <template v-else-if="task.category === 'observe'">
          <path d="M3.5 12s3-5.5 8.5-5.5 8.5 5.5 8.5 5.5-3 5.5-8.5 5.5S3.5 12 3.5 12z" />
          <circle cx="12" cy="12" r="2.6" />
        </template>
        <template v-else-if="task.category === 'creative'">
          <path d="M12 3.5a8.5 8.5 0 1 0 0 17c1.4 0 2-0.9 2-1.9 0-1.3-1.2-1.6-1.2-2.8 0-1 .8-1.6 1.9-1.6h1.5a4 4 0 0 0 4-4.1c0-3.7-3.6-6.6-8.2-6.6z" />
          <circle cx="8.5" cy="9" r="1" fill="currentColor" stroke="none" />
          <circle cx="12" cy="7.5" r="1" fill="currentColor" stroke="none" />
          <circle cx="15.5" cy="9.5" r="1" fill="currentColor" stroke="none" />
        </template>
        <template v-else-if="task.category === 'story'">
          <path d="M19 4.5c-3.4.6-6.2 2.2-7 4.5-.9 2.4.4 5 2.8 6.4 2.4 1.4 5.3 1 6.7-.8" />
          <path d="M5 19.5c1.2-3.6 3.4-6.4 6.2-8.2" />
        </template>
        <template v-else-if="task.category === 'life'">
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2.5v2M12 19.5v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M2.5 12h2M19.5 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4" />
        </template>
        <template v-else>
          <rect x="4" y="4" width="7" height="7" rx="2" />
          <rect x="13" y="4" width="7" height="7" rx="2" />
          <rect x="4" y="13" width="7" height="7" rx="2" />
          <path d="M16.5 13v7M13 16.5h7" />
        </template>
      </svg>
    </div>

    <div class="task-main">
      <div class="task-top">
        <span class="task-cat">{{ meta.label }}</span>
        <span class="task-status" :class="task.status">{{ STATUS_META[task.status].label }}</span>
      </div>
      <h3 class="task-title">{{ task.title }}</h3>
      <p v-if="!compact" class="task-desc">{{ task.description }}</p>
      <div class="task-meta">
        <span class="difficulty" :title="DIFFICULTY_LABELS[stars]">
          <span v-for="n in 3" :key="n" class="star" :class="{ on: n <= stars }">★</span>
        </span>
        <span class="reward">+{{ task.reward }} 经验</span>
      </div>
    </div>
  </article>
</template>

<style scoped lang="scss">
.task-card {
  display: flex;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 26px rgba(118, 148, 182, 0.12);
  transition: transform 0.22s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.22s;
  position: relative;
  overflow: hidden;
}

.task-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  background: var(--accent);
  opacity: 0.7;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 36px rgba(118, 148, 182, 0.2);
}

.task-card.done {
  opacity: 0.72;
}

.task-icon {
  flex: none;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: var(--accent-bg);
  color: var(--accent);
}

.task-icon svg {
  width: 24px;
  height: 24px;
}

.task-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.task-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-cat {
  font-size: 12px;
  font-weight: 800;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 2px 10px;
  border-radius: 999px;
}

.task-status {
  margin-left: auto;
  font-size: 11.5px;
  font-weight: 800;
  color: var(--ink-faint);
}

.task-status.completed {
  color: #4e9378;
}

.task-status.in_progress {
  color: #c07c45;
}

.task-title {
  font-size: 17.5px;
  font-weight: 800;
}

.task-desc {
  font-size: 13.5px;
  color: var(--ink-soft);
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 3px;
}

.star {
  color: rgba(169, 158, 181, 0.35);
  font-size: 13px;
}

.star.on {
  color: #f2b33d;
}

.reward {
  font-size: 12.5px;
  font-weight: 800;
  color: #b5892e;
  background: rgba(255, 232, 163, 0.45);
  padding: 1px 10px;
  border-radius: 999px;
}

.task-card.compact {
  padding: 14px 16px;
  gap: 12px;
}

.compact .task-icon {
  width: 38px;
  height: 38px;
  border-radius: 13px;
}

.compact .task-title {
  font-size: 15.5px;
}
</style>