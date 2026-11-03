<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import { getTask, startTask } from '@/api/tasks'
import type { CompleteTaskResponse, Task } from '@/api/types'
import { categoryMeta, DIFFICULTY_LABELS, STATUS_META } from '@/constants/labels'
import { formatDateTime } from '@/utils/format'
import { usePetStore } from '@/stores/pet'
import { useTasksStore } from '@/stores/tasks'

const route = useRoute()
const router = useRouter()
const petStore = usePetStore()
const tasksStore = useTasksStore()

const taskId = Number(route.params.id)
const task = ref<Task | null>(null)
const loading = ref(true)
const error = ref('')
const share = ref('')
const completing = ref(false)
const result = ref<CompleteTaskResponse | null>(null)

const pet = computed(() => petStore.pet)
const category = computed(() => categoryMeta(task.value?.category))
const stars = computed(() => Math.min(3, Math.max(1, task.value?.difficulty ?? 1)))
const isCompleted = computed(() => task.value?.status === 'completed')

onMounted(async () => {
  await petStore.load()
  task.value =
    tasksStore.today.find((item) => item.id === taskId) ??
    tasksStore.history.find((item) => item.id === taskId) ??
    null
  if (!task.value) {
    try {
      task.value = await getTask(taskId)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '任务不存在'
    }
  }
  // 打开待完成任务时置为「进行中」，给前端更真实的状态反馈
  if (task.value && task.value.status === 'pending') {
    try {
      task.value = await startTask(taskId)
    } catch {
      // 开始状态失败不影响查看/完成
    }
  }
  loading.value = false
})

async function complete() {
  if (!task.value || completing.value) return
  completing.value = true
  result.value = await tasksStore.complete(taskId, share.value.trim() || undefined)
  completing.value = false
}
</script>

<template>
  <div class="task-detail page">
    <button class="back" @click="router.back()">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M14.5 5 7.5 12l7 7" />
      </svg>
      返回
    </button>

    <div v-if="loading" class="card loading">正在打开任务卡片…</div>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <template v-else-if="task">
      <section class="detail-card card" :style="{ '--accent': category.color, '--accent-bg': category.bg }">
        <div class="detail-head">
          <span class="cat-badge">{{ category.label }}</span>
          <span class="status">{{ STATUS_META[task.status].label }}</span>
        </div>
        <h1>{{ task.title }}</h1>
        <p class="desc">{{ task.description }}</p>

        <div class="meta-row">
          <div class="meta">
            <span class="meta-label">难度</span>
            <span class="stars">
              <span v-for="n in 3" :key="n" class="star" :class="{ on: n <= stars }">★</span>
              <em>{{ DIFFICULTY_LABELS[stars] }}</em>
            </span>
          </div>
          <div class="meta">
            <span class="meta-label">完成奖励</span>
            <span class="reward">+{{ task.reward }} 经验</span>
          </div>
          <div class="meta">
            <span class="meta-label">创建时间</span>
            <span class="meta-value">{{ formatDateTime(task.createdAt) }}</span>
          </div>
        </div>
      </section>

      <section v-if="isCompleted || result" class="done-card card">
        <div class="done-pet">
          <PetAvatar
            :species="pet?.species || 'fox'"
            :stage="pet?.growthStage || 'seed'"
            mood="excited"
            :size="150"
          />
        </div>
        <div class="done-info">
          <h2>任务完成啦！</h2>
          <p v-if="result" class="feedback">“{{ result.feedback }}”</p>
          <p v-else class="feedback">这个任务已经完成过了，星星为你骄傲！</p>
          <div v-if="result" class="gain-row">
            <span class="gain">+{{ result.xpGained }} 经验</span>
            <span v-if="result.levelUp" class="gain level">升到 Lv.{{ result.newLevel }}</span>
            <span v-if="result.stageUp" class="gain stage">进入新阶段</span>
          </div>
          <div class="done-actions">
            <button class="btn btn-primary" @click="router.push('/tasks')">继续探索</button>
            <button class="btn btn-ghost" @click="router.push('/growth')">查看成长</button>
          </div>
        </div>
      </section>

      <section v-else class="complete-card card">
        <h2>完成得怎么样？</h2>
        <p class="complete-hint">把你的发现告诉伙伴吧（可以不写，完成就很棒了）。</p>
        <textarea
          v-model="share"
          class="textarea"
          maxlength="500"
          placeholder="例如：我发现霸王龙的牙齿很尖，三角龙有三只角…"
        ></textarea>
        <p v-if="tasksStore.error" class="error-text">{{ tasksStore.error }}</p>
        <button class="btn btn-sun complete-btn" :disabled="completing" @click="complete">
          {{ completing ? '伙伴正在为你鼓掌…' : '我完成任务啦' }}
        </button>
        <p class="gentle">不用着急，任务随时可以回来慢慢做。</p>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
.task-detail {
  max-width: 860px;
}

.back {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px 8px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: var(--ink-soft);
  font-weight: 800;
  font-size: 13px;
  margin-bottom: 14px;
}

.back:hover {
  background: #ffffff;
}

.loading {
  padding: 32px;
  text-align: center;
  font-weight: 700;
}

.detail-card {
  padding: clamp(22px, 4vw, 34px);
  margin-bottom: 18px;
  position: relative;
  overflow: hidden;
}

.detail-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 6px;
  background: var(--accent);
  opacity: 0.7;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.cat-badge {
  font-size: 12px;
  font-weight: 800;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 3px 12px;
  border-radius: 999px;
}

.status {
  margin-left: auto;
  font-size: 12px;
  font-weight: 800;
  color: var(--ink-faint);
}

.detail-card h1 {
  font-size: clamp(25px, 3.6vw, 34px);
}

.desc {
  margin-top: 12px;
  font-size: 15.5px;
  font-weight: 600;
  color: var(--ink-soft);
  line-height: 1.8;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 26px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed rgba(125, 113, 137, 0.18);
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.meta-label {
  font-size: 11px;
  font-weight: 800;
  color: var(--ink-faint);
  letter-spacing: 1px;
}

.stars {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.star {
  color: rgba(169, 158, 181, 0.35);
}

.star.on {
  color: #f2b33d;
}

.stars em {
  font-style: normal;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-soft);
  margin-left: 5px;
}

.reward {
  font-weight: 900;
  color: #b5892e;
  background: rgba(255, 232, 163, 0.5);
  padding: 2px 12px;
  border-radius: 999px;
  font-size: 13px;
}

.meta-value {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--ink-soft);
}

.complete-card {
  padding: clamp(20px, 4vw, 30px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.complete-card h2,
.done-card h2 {
  font-size: 20px;
}

.complete-hint {
  font-size: 13px;
  color: var(--ink-soft);
  font-weight: 600;
}

.complete-btn {
  width: 100%;
  margin-top: 4px;
  padding: 15px;
  font-size: 16.5px;
}

.gentle {
  text-align: center;
  font-size: 11.5px;
  color: var(--ink-faint);
  font-weight: 700;
}

.done-card {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 16px;
  padding: clamp(20px, 4vw, 30px);
  align-items: center;
  background: linear-gradient(160deg, #fffdf7, #eefaf2);
}

.done-pet {
  display: flex;
  justify-content: center;
}

.done-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback {
  font-size: 14px;
  color: var(--ink-soft);
  font-weight: 600;
  line-height: 1.75;
}

.gain-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.gain {
  font-size: 12px;
  font-weight: 900;
  padding: 3px 12px;
  border-radius: 999px;
  background: rgba(255, 232, 163, 0.6);
  color: #9a6f1c;
}

.gain.level {
  background: rgba(169, 216, 247, 0.55);
  color: #2f6d95;
}

.gain.stage {
  background: rgba(216, 245, 228, 0.7);
  color: #3f7d62;
}

.done-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 6px;
}

.done-actions .btn {
  padding: 10px 20px;
  font-size: 13.5px;
}

@media (max-width: 640px) {
  .done-card {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .done-info {
    align-items: center;
  }

  .done-actions {
    justify-content: center;
  }
}
</style>