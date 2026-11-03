<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import GrowthBar from '@/components/GrowthBar.vue'
import TaskCard from '@/components/TaskCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getGrowthDaily } from '@/api/growth'
import type { DailySummary } from '@/api/types'
import { moodMeta } from '@/constants/labels'
import { greetingByHour, relativeDayLabel, todayLabel, weekdayLabel } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { usePetStore } from '@/stores/pet'
import { useTasksStore } from '@/stores/tasks'

const router = useRouter()
const auth = useAuthStore()
const petStore = usePetStore()
const tasksStore = useTasksStore()

const recentDays = ref<DailySummary[]>([])
const eventsLoading = ref(false)

const pet = computed(() => petStore.pet)
const mood = computed(() => moodMeta(pet.value?.mood))
const activeTask = computed(() => tasksStore.activeTask)

onMounted(async () => {
  await Promise.all([petStore.load(), tasksStore.loadToday()])
  eventsLoading.value = true
  try {
    recentDays.value = await getGrowthDaily(3)
  } catch {
    recentDays.value = []
  } finally {
    eventsLoading.value = false
  }
})

async function designTask() {
  const task = await tasksStore.generate()
  if (task) {
    await router.push(`/tasks/${task.id}`)
  }
}
</script>

<template>
  <div class="home page">
    <header class="home-top">
      <div>
        <p class="date-line">{{ todayLabel() }}</p>
        <h1>{{ greetingByHour() }}，{{ auth.username || '小探险家' }}</h1>
      </div>
      <button class="avatar-btn" @click="router.push('/settings')" aria-label="伙伴设置">
        <span>{{ (auth.username || 'D')[0] }}</span>
      </button>
    </header>

    <div v-if="petStore.error" class="error-text">{{ petStore.error }}</div>

    <template v-if="pet">
      <section class="hero card">
        <div class="hero-pet">
          <div class="pet-halo"></div>
          <PetAvatar
            :species="pet.species"
            :stage="pet.growthStage"
            :mood="pet.mood"
            :size="270"
          />
          <div class="speech-bubble">
            <span class="mood-dot"></span>
            {{ mood.phrase }}
          </div>
        </div>

        <div class="hero-info">
          <p class="meet-line">我的梦想伙伴</p>
          <h2>{{ pet.name }}</h2>
          <p class="tags">
            <span class="chip">{{ pet.speciesName }}</span>
            <span class="chip">{{ pet.personalityName }}</span>
            <span class="chip mood-chip">{{ mood.label }}中</span>
          </p>
          <GrowthBar
            :level="pet.level"
            :percent="pet.levelProgress"
            :exp-to-next="pet.expToNextLevel"
            :stage-name="pet.stageName"
          />
          <div class="hero-actions">
            <button class="btn btn-primary" @click="router.push('/chat')">
              <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4.5 6.5h15v9.5h-8.5L6 20v-4H4.5z" />
              </svg>
              和{{ pet.name }}聊天
            </button>
            <button class="btn btn-ghost" @click="router.push('/growth')">成长档案</button>
          </div>
          </div>
      </section>

      <div class="quick-grid">
        <button class="quick-card" @click="router.push('/chat')">
          <span class="quick-icon chat">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4.5 6.5h15v9.5h-8.5L6 20v-4H4.5z" />
            </svg>
          </span>
          <span><strong>聊聊天</strong><small>分享今天的故事</small></span>
        </button>
        <button class="quick-card" @click="router.push('/tasks')">
          <span class="quick-icon task">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <rect x="4.5" y="4.5" width="15" height="16" rx="3" />
              <path d="M8.5 10.5 10.5 12.5 15.5 7.8M8.5 16h7" />
            </svg>
          </span>
          <span><strong>小任务</strong><small>一起探索新发现</small></span>
        </button>
        <button class="quick-card" @click="router.push('/diary')">
          <span class="quick-icon diary">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 5.5c-2-1.4-4.6-1.6-7-1v14c2.4-.6 5-.4 7 1 2-1.4 4.6-1.6 7-1v-14c-2.4-.6-5-.4-7 1z" />
              <path d="M12 5.5v14" />
            </svg>
          </span>
          <span><strong>成长日记</strong><small>回顾共同的经历</small></span>
        </button>
        <button class="quick-card" @click="router.push('/settings')">
          <span class="quick-icon settings">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3.2" />
              <path d="M19.4 13.5a7.6 7.6 0 0 0 0-3l1.7-1.3-1.8-3.1-2 .8a7.6 7.6 0 0 0-2.6-1.5L14.4 3h-3.6l-.3 2.2a7.6 7.6 0 0 0-2.6 1.5l-2-.8L4 9l1.7 1.3a7.6 7.6 0 0 0 0 3L4 14.6l1.8 3.1 2-.8a7.6 7.6 0 0 0 2.6 1.5l.3 2.2h3.6l.3-2.2a7.6 7.6 0 0 0 2.6-1.5l2 .8 1.8-3.1z" />
            </svg>
          </span>
          <span><strong>我的伙伴</strong><small>更换形象和档案</small></span>
        </button>
      </div>

      <div class="section-title">
        <h2>今日小任务</h2>
        <span class="hint">轻松自愿，不着急</span>
      </div>

      <TaskCard v-if="activeTask" :task="activeTask" @open="(t) => router.push(`/tasks/${t.id}`)" />
      <EmptyState
        v-else
        title="今天还没有任务"
        desc="让你的伙伴根据你们聊过的话题，设计一个有趣的小探索吧。"
      >
        <button class="btn btn-sun" :disabled="tasksStore.generating" @click="designTask">
          {{ tasksStore.generating ? '伙伴正在想…' : '让伙伴设计新任务' }}
        </button>
      </EmptyState>
      <p v-if="tasksStore.error" class="error-text gap-top">{{ tasksStore.error }}</p>

      <div class="section-title">
        <h2>最近几天</h2>
        <button class="hint link" @click="router.push('/diary')">全部日记</button>
      </div>

      <div v-if="eventsLoading" class="card recent-card muted">正在整理这几天的回忆…</div>
      <div v-else-if="recentDays.length" class="day-list">
        <article
          v-for="day in recentDays"
          :key="day.date"
          class="day-card card"
          :class="{ milestone: day.milestone }"
        >
          <div class="day-head">
            <div class="day-date">
              <strong>{{ relativeDayLabel(day.date) }}</strong>
              <span>{{ weekdayLabel(day.date) }} · {{ day.date.slice(5).replace('-', '/') }}</span>
            </div>
            <span v-if="day.milestone" class="milestone-tag">成长里程碑</span>
          </div>
          <div class="day-stats">
            <span class="day-stat"><span class="stat-dot chat"></span>聊天 {{ day.chatCount }} 次</span>
            <span class="day-stat"><span class="stat-dot task"></span>完成任务 {{ day.taskCount }} 个</span>
            <span class="day-stat exp">+{{ day.experience }} 经验</span>
          </div>
          <ul v-if="day.highlights.length" class="day-highlights">
            <li v-for="highlight in day.highlights" :key="highlight">
              <span class="highlight-star">✦</span>{{ highlight }}
            </li>
          </ul>
        </article>
      </div>
      <EmptyState
        v-else
        title="故事才刚开始"
        desc="和伙伴聊聊天，或者完成第一个小任务，这里就会留下你们共同的回忆。"
      >
        <button class="btn btn-soft" @click="router.push('/chat')">去和伙伴说说话</button>
      </EmptyState>
    </template>

    <div v-else-if="!petStore.loading" class="loading-card card">
      <p>正在找回你的伙伴…</p>
      <button class="btn btn-ghost" @click="petStore.load(true)">重新加载</button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.home-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.date-line {
  font-size: 12.5px;
  font-weight: 800;
  color: var(--ink-faint);
  letter-spacing: 0.5px;
}

.home-top h1 {
  font-size: clamp(22px, 3.2vw, 30px);
  margin-top: 2px;
}

.avatar-btn {
  flex: none;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd97a, #ffc0d2);
  color: #7c4a5e;
  font-weight: 900;
  font-size: 18px;
  box-shadow: 0 10px 24px rgba(242, 160, 182, 0.35);
  transition: transform 0.2s;
}

.avatar-btn:hover {
  transform: scale(1.06) rotate(-4deg);
}

.hero {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 18px;
  padding: clamp(18px, 3vw, 30px);
  overflow: hidden;
  position: relative;
}

.hero::before {
  content: '';
  position: absolute;
  top: -60%;
  right: -10%;
  width: 60%;
  height: 160%;
  background: radial-gradient(circle, rgba(255, 236, 190, 0.35), transparent 65%);
  pointer-events: none;
}

.hero-pet {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  min-height: 280px;
}

.pet-halo {
  position: absolute;
  bottom: 6px;
  left: 50%;
  width: 260px;
  height: 260px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(circle at 50% 62%, rgba(223, 241, 255, 0.95), rgba(255, 255, 255, 0) 68%);
}

.speech-bubble {
  position: absolute;
  top: 2px;
  left: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 15px;
  border-radius: 18px 18px 18px 4px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 10px 24px rgba(118, 148, 182, 0.16);
  font-size: 12.5px;
  font-weight: 800;
  color: var(--ink-soft);
  animation: bubble-breathe 4.6s ease-in-out infinite;
}

.mood-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7cc9a4;
  box-shadow: 0 0 0 4px rgba(124, 201, 164, 0.2);
}

.hero-info {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
  padding: 8px 0;
}

.meet-line {
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 2px;
  color: var(--ink-faint);
}

.hero-info h2 {
  font-size: clamp(28px, 4vw, 38px);
  line-height: 1.1;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.mood-chip {
  background: rgba(216, 245, 228, 0.8);
  color: #3f7d62;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
}

.hero-actions .btn svg {
  flex: none;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 11px;
  text-align: left;
  padding: 14px 15px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 24px rgba(118, 148, 182, 0.1);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.2s;
}

.quick-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(118, 148, 182, 0.18);
}

.quick-icon {
  flex: none;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
}

.quick-icon svg {
  width: 21px;
  height: 21px;
}

.quick-icon.chat {
  background: #e1f2ff;
  color: #4e8ab8;
}

.quick-icon.task {
  background: #fff0d6;
  color: #b07a3d;
}

.quick-icon.diary {
  background: #fbe7f4;
  color: #b8739f;
}

.quick-icon.settings {
  background: #eae4ff;
  color: #7a6bbf;
}

.quick-card strong {
  display: block;
  font-size: 13.5px;
  font-weight: 800;
}

.quick-card small {
  display: block;
  font-size: 10.5px;
  color: var(--ink-faint);
  font-weight: 700;
  white-space: nowrap;
}

.day-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.day-card {
  padding: 15px 18px;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
}

.day-card.milestone {
  border: 1.5px solid rgba(255, 217, 122, 0.55);
  background: linear-gradient(160deg, rgba(255, 253, 247, 0.95), rgba(255, 246, 228, 0.88));
}

.day-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.day-date {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.day-date strong {
  font-size: 16px;
}

.day-date span {
  font-size: 10.5px;
  color: var(--ink-faint);
  font-weight: 700;
  letter-spacing: 0.3px;
}

.milestone-tag {
  font-size: 10.5px;
  font-weight: 800;
  color: #8a6418;
  background: rgba(255, 232, 163, 0.6);
  padding: 3px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

.day-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 9px;
}

.day-stat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 800;
  color: var(--ink-soft);
  background: rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  padding: 3px 10px;
}

.stat-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.stat-dot.chat {
  background: #8fc7ee;
}

.stat-dot.task {
  background: #7cc9a4;
}

.day-stat.exp {
  color: #b5892e;
  background: rgba(255, 232, 163, 0.5);
}

.day-highlights {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.day-highlights li {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-soft);
  display: flex;
  gap: 6px;
  line-height: 1.5;
}

.highlight-star {
  color: #e9b94e;
  flex: none;
  font-size: 11px;
  margin-top: 1px;
}

.recent-card {
  padding: 20px;
  font-weight: 700;
  font-size: 13px;
}

.link {
  color: #5d93bd;
  font-weight: 800;
}

.gap-top {
  margin-top: 10px;
}

.loading-card {
  padding: 40px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  font-weight: 700;
}

@keyframes bubble-breathe {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero-pet {
    min-height: 260px;
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 420px) {
  .quick-card small {
    display: none;
  }

  .quick-card {
    padding: 12px;
  }
}
</style>