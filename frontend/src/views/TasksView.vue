<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TaskCard from '@/components/TaskCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import type { Task } from '@/api/types'
import { useTasksStore } from '@/stores/tasks'

const router = useRouter()
const tasksStore = useTasksStore()

const tab = ref<'today' | 'history'>('today')

const activeList = computed(() =>
  tasksStore.today.filter((task) => task.status !== 'completed'),
)
const doneList = computed(() =>
  tasksStore.today.filter((task) => task.status === 'completed'),
)

onMounted(async () => {
  await tasksStore.loadToday()
  await tasksStore.loadHistory()
})

async function generate() {
  const task = await tasksStore.generate()
  if (task) router.push(`/tasks/${task.id}`)
}

function open(task: Task) {
  router.push(`/tasks/${task.id}`)
}
</script>

<template>
  <div class="tasks page">
    <header class="page-head">
      <div>
        <p class="eyebrow">一起探索</p>
        <h1>今日小任务</h1>
        <p class="sub">轻松有趣，完成不了也没关系，伙伴会一直陪着你。</p>
      </div>
      <button class="btn btn-sun" :disabled="tasksStore.generating" @click="generate">
        {{ tasksStore.generating ? '伙伴正在想…' : '换一个新任务' }}
      </button>
    </header>

    <div class="tabs">
      <button :class="{ active: tab === 'today' }" @click="tab = 'today'">今天</button>
      <button :class="{ active: tab === 'history' }" @click="tab = 'history'">历史任务</button>
    </div>

    <p v-if="tasksStore.error" class="error-text">{{ tasksStore.error }}</p>

    <template v-if="tab === 'today'">
      <div v-if="activeList.length" class="task-list">
        <TaskCard v-for="task in activeList" :key="task.id" :task="task" @open="open" />
      </div>
      <EmptyState
        v-else
        title="还没有进行中的任务"
        desc="让伙伴根据你的兴趣，为你设计一个 10~20 分钟的小探索吧。"
      >
        <button class="btn btn-sun" :disabled="tasksStore.generating" @click="generate">
          {{ tasksStore.generating ? '伙伴正在想…' : '让伙伴设计新任务' }}
        </button>
      </EmptyState>

      <template v-if="doneList.length">
        <div class="section-title">
          <h2>今天完成的</h2>
          <span class="hint">干得漂亮</span>
        </div>
        <div class="task-list done-list">
          <TaskCard v-for="task in doneList" :key="task.id" :task="task" @open="open" />
        </div>
      </template>
    </template>

    <template v-else>
      <div v-if="tasksStore.history.length" class="task-list">
        <TaskCard v-for="task in tasksStore.history" :key="task.id" :task="task" @open="open" />
      </div>
      <EmptyState
        v-else
        title="还没有历史任务"
        desc="每完成一次探索，这里就会多一枚成长的小脚印。"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.eyebrow {
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 2px;
  color: var(--ink-faint);
}

.page-head h1 {
  font-size: clamp(26px, 4vw, 34px);
  margin-top: 2px;
}

.sub {
  margin-top: 6px;
  font-size: 13.5px;
  color: var(--ink-soft);
  font-weight: 600;
}

.tabs {
  display: inline-flex;
  gap: 5px;
  padding: 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.65);
  margin-bottom: 18px;
}

.tabs button {
  padding: 8px 22px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 13.5px;
  color: var(--ink-soft);
  transition: all 0.2s;
}

.tabs button.active {
  background: #ffffff;
  color: #2f6d95;
  box-shadow: 0 6px 16px rgba(93, 147, 189, 0.16);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.done-list {
  opacity: 0.85;
}

@media (max-width: 640px) {
  .page-head .btn {
    width: 100%;
  }
}
</style>