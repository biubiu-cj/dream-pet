<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import { getMemories, getMemorySummary } from '@/api/memories'
import { getGrowthDaily } from '@/api/growth'
import type { DailySummary, Memory, MemorySummary } from '@/api/types'
import { formatDateTime, relativeDayLabel, weekdayLabel } from '@/utils/format'
import { usePetStore } from '@/stores/pet'

const router = useRouter()
const petStore = usePetStore()

const summary = ref<MemorySummary | null>(null)
const memories = ref<Memory[]>([])
const days = ref<DailySummary[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  await petStore.load()
  try {
    const [summaryData, memoryData, dailyData] = await Promise.all([
      getMemorySummary(),
      getMemories({ type: 'event', limit: 50 }),
      getGrowthDaily(7),
    ])
    summary.value = summaryData
    memories.value = memoryData
    days.value = dailyData
  } catch (e) {
    error.value = e instanceof Error ? e.message : '日记加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="diary page">
    <header class="page-head">
      <p class="eyebrow">被记住的时光</p>
      <h1>成长日记</h1>
      <p class="sub">{{ petStore.pet?.name || '伙伴' }}把你们的共同经历，都悄悄收好了。</p>
    </header>

    <div v-if="loading" class="card loading">正在翻开记忆的小本本…</div>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <template v-else>
      <section class="summary-card card">
        <div class="summary-head">
          <span class="book-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 5.5c-2-1.4-4.6-1.6-7-1v14c2.4-.6 5-.4 7 1 2-1.4 4.6-1.6 7-1v-14c-2.4-.6-5-.4-7 1z" />
              <path d="M12 5.5v14" />
            </svg>
          </span>
          <div>
            <h2>{{ petStore.pet?.name || '伙伴' }}眼中的你</h2>
            <p class="tiny">来自长期记忆摘要</p>
          </div>
        </div>
        <p class="summary-text">
          {{ summary?.summary || '我们才刚刚认识，等你多和我聊聊，我就会把你喜欢的事情都记下来。' }}
        </p>
        <div class="summary-chips">
          <span v-for="item in summary?.interests ?? []" :key="item" class="chip">{{ item }}</span>
          <span v-for="item in summary?.profile ?? []" :key="item" class="chip profile-chip">{{ item }}</span>
        </div>
      </section>

      <div class="section-title">
        <h2>重要的小事</h2>
        <span class="hint">伙伴会一直记得</span>
      </div>

      <div v-if="memories.length" class="memory-grid">
        <blockquote v-for="memory in memories.slice(0, 8)" :key="memory.id" class="memory-card card">
          <span class="quote-mark">“</span>
          <p>{{ memory.content }}</p>
          <footer>
            <span>{{ formatDateTime(memory.createdAt) }}</span>
            <span class="importance">
              <span v-for="n in memory.importance" :key="n" class="heart">♥</span>
            </span>
          </footer>
        </blockquote>
      </div>
      <EmptyState
        v-else
        title="还没有特别的小事"
        desc="跟伙伴分享你的梦想、发现或今天的心情，它们会被认真记下来。"
      >
        <button class="btn btn-soft" @click="router.push('/chat')">去聊天</button>
      </EmptyState>

      <div class="section-title">
        <h2>每日回顾</h2>
        <span class="hint">最近 7 天</span>
      </div>

      <div v-if="days.length" class="timeline">
        <div v-for="day in days" :key="day.date" class="timeline-item">
          <div class="timeline-rail">
            <span class="timeline-dot" :class="{ milestone: day.milestone }"></span>
          </div>
          <div class="timeline-body card">
            <div class="timeline-top">
              <strong>{{ relativeDayLabel(day.date) }}</strong>
              <span class="time">{{ weekdayLabel(day.date) }} · {{ day.date }}</span>
              <span v-if="day.milestone" class="milestone-tag">里程碑</span>
            </div>
            <p class="day-stats">
              聊天 {{ day.chatCount }} 次 · 完成任务 {{ day.taskCount }} 个 · +{{ day.experience }} 经验
            </p>
            <ul v-if="day.highlights.length" class="day-highlights">
              <li v-for="highlight in day.highlights" :key="highlight">
                <span class="highlight-star">✦</span>{{ highlight }}
              </li>
            </ul>
          </div>
        </div>
      </div>
      <EmptyState
        v-else
        title="时间线还是空空的"
        desc="和伙伴聊天、完成任务，这里就会记录下每一步成长。"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  margin-bottom: 20px;
}

.eyebrow {
  font-size: 12.5px;
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

.summary-card {
  padding: clamp(20px, 3.4vw, 30px);
  background: linear-gradient(160deg, rgba(255, 253, 247, 0.95), rgba(255, 243, 221, 0.85));
  border: 1.5px solid rgba(255, 217, 122, 0.45);
}

.summary-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.book-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: grid;
  place-items: center;
  background: rgba(255, 232, 163, 0.6);
  color: #a8822f;
}

.book-icon svg {
  width: 23px;
  height: 23px;
}

.summary-head h2 {
  font-size: 19px;
}

.tiny {
  font-size: 12px;
  font-weight: 800;
  color: var(--ink-faint);
}

.summary-text {
  margin-top: 15px;
  font-size: 14.5px;
  line-height: 1.8;
  font-weight: 600;
  color: #5c4c3c;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 14px;
}

.profile-chip {
  background: rgba(255, 232, 163, 0.55);
  color: #8a6418;
}

.memory-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.memory-card {
  position: relative;
  padding: 18px 20px 14px;
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.quote-mark {
  position: absolute;
  top: -6px;
  right: 12px;
  font-family: Georgia, serif;
  font-size: 64px;
  color: rgba(242, 160, 182, 0.22);
  line-height: 1;
}

.memory-card p {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.7;
  position: relative;
  z-index: 1;
}

.memory-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-faint);
  font-weight: 700;
}

.heart {
  color: #f2a0b6;
  font-size: 12px;
  margin-left: 1px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-rail {
  width: 26px;
  display: flex;
  justify-content: center;
  position: relative;
}

.timeline-rail::after {
  content: '';
  position: absolute;
  top: 26px;
  bottom: -4px;
  width: 2px;
  background: rgba(169, 216, 247, 0.45);
  border-radius: var(--radius-pill);
}

.timeline-item:last-child .timeline-rail::after {
  display: none;
}

.timeline-dot {
  width: 16px;
  height: 16px;
  margin-top: 16px;
  border-radius: 50%;
  background: #cfe9fb;
  border: 3px solid #ffffff;
  box-shadow: var(--elev-1);
  position: relative;
  z-index: 1;
}

.timeline-dot.milestone {
  background: linear-gradient(135deg, #ffd97a, #ffe9a8);
  box-shadow: 0 0 0 4px rgba(255, 232, 163, 0.35), var(--elev-1);
}

.milestone-tag {
  font-size: 12px;
  font-weight: 800;
  color: #8a6418;
  background: rgba(255, 232, 163, 0.6);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  margin-left: auto;
}

.day-stats {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-soft);
  margin-top: 4px;
}

.day-highlights {
  list-style: none;
  margin: 8px 0 0;
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
  color: #966e13;
  flex: none;
  font-size: 12px;
  margin-top: 1px;
}

.timeline-body {
  flex: 1;
  margin-bottom: 10px;
  padding: 13px 17px;
  border-radius: var(--radius-md);
}

.timeline-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.timeline-top strong {
  font-size: 13px;
  font-weight: 800;
}

.time {
  font-size: 12px;
  color: var(--ink-faint);
  font-weight: 700;
}

.timeline-body p {
  font-size: 13px;
  color: var(--ink-soft);
  font-weight: 600;
  margin-top: 3px;
}

.exp {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  font-weight: 900;
  color: #8a6620;
  background: rgba(255, 232, 163, 0.5);
  padding: 2px 10px;
  border-radius: var(--radius-pill);
}
</style>