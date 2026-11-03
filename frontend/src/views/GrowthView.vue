<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import GrowthBar from '@/components/GrowthBar.vue'
import { getAchievements, getGrowthDaily, getGrowthOverview } from '@/api/growth'
import type { Achievement, DailySummary, GrowthOverview } from '@/api/types'
import { eventMeta, STAGE_OPTIONS } from '@/constants/labels'
import { formatDateTime } from '@/utils/format'
import { usePetStore } from '@/stores/pet'

const petStore = usePetStore()
const overview = ref<GrowthOverview | null>(null)
const daily = ref<DailySummary[]>([])
const achievements = ref<Achievement[]>([])
const loading = ref(true)
const error = ref('')

const weekly = computed(() => {
  const chat = daily.value.reduce((sum, day) => sum + day.chatCount, 0)
  const tasks = daily.value.reduce((sum, day) => sum + day.taskCount, 0)
  const experience = daily.value.reduce((sum, day) => sum + day.experience, 0)
  const highlights: string[] = []
  for (const day of daily.value) {
    for (const item of day.highlights) {
      if (!highlights.includes(item)) highlights.push(item)
    }
  }
  return {
    chat,
    tasks,
    experience,
    activeDays: daily.value.length,
    highlights: highlights.slice(0, 4),
  }
})

const unlockedCount = computed(() => achievements.value.filter((item) => item.unlocked).length)

function badgePercent(badge: Achievement) {
  return badge.target > 0 ? Math.round((badge.progress / badge.target) * 100) : 0
}

const pet = computed(() => overview.value?.pet ?? petStore.pet)

/** 聊天 +2 经验的流水不逐条展示，只保留任务/升级/阶段等有意义的事件 */
const importantEvents = computed(() =>
  (overview.value?.recentEvents ?? [])
    .filter((event) => event.eventType !== 'chat_exp')
    .slice(0, 8),
)

const currentStageIndex = computed(() => {
  const level = pet.value?.level ?? 1
  let index = 0
  STAGE_OPTIONS.forEach((stage, i) => {
    if (level >= stage.minLevel) index = i
  })
  return index
})

onMounted(async () => {
  await petStore.load()
  try {
    const [overviewData, dailyData, achievementData] = await Promise.all([
      getGrowthOverview(),
      getGrowthDaily(7),
      getAchievements(),
    ])
    overview.value = overviewData
    daily.value = dailyData
    achievements.value = achievementData
  } catch (e) {
    error.value = e instanceof Error ? e.message : '成长档案加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="growth page">
    <header class="page-head">
      <p class="eyebrow">一起长大</p>
      <h1>成长档案</h1>
      <p class="sub">每一段经历，都会变成伙伴成长的力量。</p>
    </header>

    <div v-if="loading" class="card loading">正在整理成长记录…</div>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <template v-else-if="pet">
      <section class="growth-hero card">
        <div class="hero-visual">
          <PetAvatar
            :species="pet.species"
            :stage="pet.growthStage"
            :mood="pet.mood"
            :size="230"
          />
        </div>
        <div class="hero-body">
          <p class="meet-line">{{ pet.name }} 的成长状态</p>
          <h2>{{ pet.stageName }}</h2>
          <GrowthBar
            :level="pet.level"
            :percent="pet.levelProgress"
            :exp-to-next="pet.expToNextLevel"
          />
          <div class="stats">
            <div class="stat">
              <strong>{{ overview?.totalExp ?? pet.experience }}</strong>
              <span>累计经验</span>
            </div>
            <div class="stat">
              <strong>{{ overview?.completedTasks ?? 0 }}</strong>
              <span>完成任务</span>
            </div>
            <div class="stat">
              <strong>{{ (overview?.interests ?? pet.interests).length }}</strong>
              <span>探索主题</span>
            </div>
            <div class="stat">
              <strong>{{ pet.level }}</strong>
              <span>当前等级</span>
            </div>
          </div>
        </div>
      </section>

      <div class="section-title">
        <h2>本周小结</h2>
        <span class="hint">最近 7 天</span>
      </div>

      <section class="weekly card">
        <div class="weekly-stats">
          <div class="weekly-stat">
            <strong>{{ weekly.chat }}</strong>
            <span>聊天次数</span>
          </div>
          <div class="weekly-stat">
            <strong>{{ weekly.tasks }}</strong>
            <span>完成任务</span>
          </div>
          <div class="weekly-stat">
            <strong>+{{ weekly.experience }}</strong>
            <span>获得经验</span>
          </div>
          <div class="weekly-stat">
            <strong>{{ weekly.activeDays }}</strong>
            <span>互动天数</span>
          </div>
        </div>
        <ul v-if="weekly.highlights.length" class="weekly-highlights">
          <li v-for="item in weekly.highlights" :key="item">
            <span class="highlight-star">✦</span>{{ item }}
          </li>
        </ul>
        <p v-else class="weekly-empty">这一周还在慢慢开始，去和伙伴聊聊天吧。</p>
      </section>

      <div class="section-title">
        <h2>成长阶段</h2>
        <span class="hint">和伙伴一起慢慢长大</span>
      </div>

      <div class="stages card">
        <div
          v-for="(stage, index) in STAGE_OPTIONS"
          :key="stage.value"
          class="stage-node"
          :class="{
            reached: index <= currentStageIndex,
            current: index === currentStageIndex,
          }"
        >
          <div class="node-dot">
            <span>{{ index + 1 }}</span>
          </div>
          <div class="node-body">
            <strong>{{ stage.label }}</strong>
            <small>{{ stage.range }}</small>
            <p>{{ stage.desc }}</p>
          </div>
          <div v-if="index < STAGE_OPTIONS.length - 1" class="node-line"></div>
        </div>
      </div>

      <div class="section-title">
        <h2>成就徽章</h2>
        <span class="hint">已解锁 {{ unlockedCount }} / {{ achievements.length }}</span>
      </div>
      <div v-if="achievements.length" class="badge-grid">
        <div
          v-for="badge in achievements"
          :key="badge.key"
          class="badge-card card"
          :class="{ unlocked: badge.unlocked }"
        >
          <div class="badge-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3.5 14.6 9l6 .9-4.3 4.2 1 6-5.3-2.8-5.3 2.8 1-6L3.4 9.9l6-.9z" />
            </svg>
          </div>
          <div class="badge-body">
            <strong>{{ badge.title }}</strong>
            <p>{{ badge.description }}</p>
            <div class="badge-progress">
              <div class="badge-track">
                <div class="badge-fill" :style="{ width: `${badgePercent(badge)}%` }"></div>
              </div>
              <span>{{ badge.progress }}/{{ badge.target }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="section-title">
        <h2>探索过的主题</h2>
      </div>
      <div class="interest-card card">
        <span
          v-for="item in overview?.interests ?? pet.interests"
          :key="item"
          class="interest-chip"
        >
          {{ item }}
        </span>
        <p v-if="!(overview?.interests ?? pet.interests).length" class="muted">
          还没有记录话题，去和伙伴随便聊聊吧。
        </p>
      </div>

      <div class="section-title">
        <h2>重要成长事件</h2>
        <span class="hint">最近的 8 条</span>
      </div>

      <div v-if="importantEvents.length" class="event-list">
        <div v-for="event in importantEvents" :key="event.id" class="event-item card">
          <span
            class="event-icon"
            :style="{ background: eventMeta(event.eventType).bg, color: eventMeta(event.eventType).color }"
          >
            <span class="event-dot"></span>
          </span>
          <div class="event-main">
            <div class="event-top">
              <strong>{{ eventMeta(event.eventType).label }}</strong>
              <span class="event-time">{{ formatDateTime(event.createdAt) }}</span>
            </div>
            <p>{{ event.description }}</p>
          </div>
          <span v-if="event.experienceGained > 0" class="exp-tag">+{{ event.experienceGained }}</span>
        </div>
      </div>
      <div v-else class="card empty-events muted">还没有成长事件，去完成第一个任务吧。</div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-head {
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

.loading {
  padding: 32px;
  text-align: center;
  font-weight: 700;
}

.growth-hero {
  display: grid;
  grid-template-columns: 0.75fr 1.25fr;
  gap: 16px;
  padding: clamp(18px, 3vw, 30px);
  align-items: center;
  overflow: hidden;
}

.hero-visual {
  display: flex;
  justify-content: center;
  background: radial-gradient(circle at 50% 62%, rgba(223, 241, 255, 0.9), transparent 66%);
  border-radius: 24px;
}

.hero-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meet-line {
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 2px;
  color: var(--ink-faint);
}

.hero-body h2 {
  font-size: clamp(24px, 3vw, 30px);
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 4px;
}

.stat {
  padding: 10px 8px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  text-align: center;
}

.stat strong {
  display: block;
  font-family: 'Baloo 2', sans-serif;
  font-size: 20px;
  color: #2f6d95;
}

.stat span {
  font-size: 10.5px;
  font-weight: 800;
  color: var(--ink-faint);
}

.stages {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  padding: 20px;
}

.stage-node {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 9px;
  text-align: center;
  opacity: 0.55;
  transition: opacity 0.3s;
}

.stage-node.reached {
  opacity: 1;
}

.node-dot {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(223, 241, 255, 0.7);
  color: #7aaFD3;
  font-weight: 900;
  font-family: 'Baloo 2', sans-serif;
  border: 2px solid rgba(169, 216, 247, 0.6);
  position: relative;
  z-index: 2;
}

.reached .node-dot {
  background: linear-gradient(135deg, #a9d8f7, #d8f5e4);
  color: #2f6d95;
  border-color: #ffffff;
  box-shadow: 0 8px 18px rgba(107, 160, 205, 0.28);
}

.current .node-dot {
  transform: scale(1.12);
  box-shadow: 0 0 0 6px rgba(169, 216, 247, 0.25), 0 10px 22px rgba(107, 160, 205, 0.3);
}

.node-body strong {
  font-size: 13.5px;
  display: block;
}

.node-body small {
  font-size: 10.5px;
  font-weight: 800;
  color: #5d93bd;
}

.node-body p {
  margin-top: 3px;
  font-size: 10.5px;
  color: var(--ink-faint);
  font-weight: 600;
}

.node-line {
  position: absolute;
  top: 19px;
  left: calc(50% + 26px);
  width: calc(100% - 52px);
  height: 3px;
  border-radius: 999px;
  background: rgba(169, 216, 247, 0.4);
}

.reached .node-line {
  background: linear-gradient(90deg, #a9d8f7, #d8f5e4);
}

.interest-card {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 20px;
}

.interest-chip {
  padding: 7px 17px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(223, 241, 255, 0.9), rgba(216, 245, 228, 0.8));
  color: #3d6f8f;
  font-weight: 800;
  font-size: 13px;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 13px 17px;
  border-radius: 18px;
}

.event-icon {
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
}

.event-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.8;
}

.event-main {
  flex: 1;
  min-width: 0;
}

.event-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.event-top strong {
  font-size: 13px;
  font-weight: 800;
}

.event-time {
  font-size: 10.5px;
  color: var(--ink-faint);
  font-weight: 700;
}

.event-main p {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
}

.exp-tag {
  flex: none;
  font-size: 12px;
  font-weight: 900;
  color: #b5892e;
  background: rgba(255, 232, 163, 0.5);
  padding: 3px 10px;
  border-radius: 999px;
}

.empty-events {
  padding: 24px;
  text-align: center;
  font-weight: 700;
}

.weekly {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.weekly-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.weekly-stat {
  text-align: center;
  padding: 10px 6px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.65);
}

.weekly-stat strong {
  display: block;
  font-family: 'Baloo 2', sans-serif;
  font-size: 22px;
  color: #2f6d95;
}

.weekly-stat span {
  font-size: 10.5px;
  font-weight: 800;
  color: var(--ink-faint);
}

.weekly-highlights {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.weekly-highlights li {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-soft);
  display: flex;
  gap: 6px;
  line-height: 1.5;
}

.weekly-empty {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-faint);
}

.highlight-star {
  color: #e9b94e;
  flex: none;
  font-size: 11px;
  margin-top: 1px;
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.badge-card {
  padding: 14px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  opacity: 0.55;
  transition: opacity 0.2s, transform 0.2s;
}

.badge-card.unlocked {
  opacity: 1;
  border: 1.5px solid rgba(255, 217, 122, 0.5);
  background: linear-gradient(160deg, rgba(255, 253, 247, 0.95), rgba(255, 249, 236, 0.9));
}

.badge-icon {
  width: 38px;
  height: 38px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  background: rgba(169, 216, 247, 0.35);
  color: #8fb8d8;
}

.badge-card.unlocked .badge-icon {
  background: linear-gradient(135deg, #ffe8a3, #ffd97a);
  color: #a8822f;
  box-shadow: 0 6px 14px rgba(233, 185, 78, 0.28);
}

.badge-icon svg {
  width: 20px;
  height: 20px;
}

.badge-body strong {
  font-size: 13px;
}

.badge-body p {
  font-size: 10.5px;
  line-height: 1.5;
  font-weight: 600;
  color: var(--ink-faint);
  min-height: 30px;
}

.badge-progress {
  display: flex;
  align-items: center;
  gap: 6px;
}

.badge-track {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: rgba(169, 216, 247, 0.3);
  overflow: hidden;
}

.badge-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #ffd97a, #b8e3d2);
  transition: width 0.6s;
}

.badge-progress span {
  font-size: 9.5px;
  font-weight: 800;
  color: var(--ink-faint);
}

@media (max-width: 800px) {
  .weekly-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .badge-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .growth-hero {
    grid-template-columns: 1fr;
  }

  .stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .stages {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
  }

  .node-line {
    display: none;
  }
}
</style>