<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import GrowthBar from '@/components/GrowthBar.vue'
import { getMemories } from '@/api/memories'
import type { Memory } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { usePetStore } from '@/stores/pet'
import { useChatStore } from '@/stores/chat'
import { useTasksStore } from '@/stores/tasks'
import { formatDateTime, formatDay } from '@/utils/format'
import { moodMeta } from '@/constants/labels'

const router = useRouter()
const auth = useAuthStore()
const petStore = usePetStore()
const chat = useChatStore()
const tasksStore = useTasksStore()

const draft = ref('')
const listRef = ref<HTMLElement | null>(null)
const designLoading = ref(false)
const confirmNew = ref(false)
const startingNew = ref(false)
const memories = ref<Memory[]>([])

const pet = computed(() => petStore.pet)
const mood = computed(() => moodMeta(pet.value?.mood))
const activeTask = computed(() => tasksStore.activeTask)

const quickTopics = [
  '今天我发现了一个有趣的事',
  '我们聊聊恐龙吧',
  '给我讲个宇宙的小知识',
  '我们做个小任务吧',
]

const visibleMessages = computed(() =>
  chat.messages.filter((message) => message.role === 'user' || message.role === 'assistant'),
)

const isEmpty = computed(() => !visibleMessages.value.length && !chat.loading)

onMounted(async () => {
  await Promise.all([petStore.load(), tasksStore.loadToday()])
  loadMemories()
  await chat.loadHistory()
  scrollToBottom(false)
})

async function loadMemories() {
  try {
    memories.value = await getMemories({ type: 'event', limit: 3 })
  } catch {
    memories.value = []
  }
}

watch(
  () => chat.messages.length,
  () => scrollToBottom(true),
)

watch(
  () => chat.loading,
  () => scrollToBottom(true),
)

async function scrollToBottom(smooth = true) {
  await nextTick()
  const el = listRef.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
}

async function send() {
  const text = draft.value.trim()
  if (!text || chat.loading) return
  const response = await chat.send(text)
  if (response) {
    if (draft.value.trim() === text) draft.value = ''
  } else {
    // 失败时把内容放回输入框，方便直接再试一次
    draft.value = text
  }
}

async function quickSend(text: string) {
  if (chat.loading) return
  const response = await chat.send(text)
  if (!response) draft.value = text
}

async function designTask() {
  if (designLoading.value) return
  designLoading.value = true
  const task = await tasksStore.generate()
  if (task) chat.attachTask(task)
  designLoading.value = false
}

async function beginNewConversation() {
  if (startingNew.value || chat.loading) return
  startingNew.value = true
  const ok = await chat.startNew()
  startingNew.value = false
  if (ok) {
    confirmNew.value = false
    draft.value = ''
    await nextTick()
    scrollToBottom(false)
  }
}
</script>

<template>
  <div class="chat-shell">
    <aside class="chat-side side-left">
      <section class="side-card companion-card">
        <div class="companion-visual">
          <PetAvatar
            :species="pet?.species || 'fox'"
            :stage="pet?.growthStage || 'seed'"
            :mood="pet?.mood || 'happy'"
            :size="168"
          />
        </div>
        <h2>{{ pet?.name || '伙伴' }}</h2>
        <p class="companion-sub">
          {{ pet?.speciesName || '小伙伴' }} · {{ pet?.personalityName || '活泼好奇' }}
        </p>
        <p v-if="pet" class="companion-since">相遇于 {{ formatDay(pet.createdAt) }}</p>
        <GrowthBar
          :level="pet?.level || 1"
          :percent="pet?.levelProgress || 0"
          :exp-to-next="pet?.expToNextLevel || 0"
          :stage-name="pet?.stageName || ''"
        />
        <div class="topic-list">
          <span class="side-label">聊过的话题</span>
          <div v-if="pet?.interests.length" class="topic-chips">
            <span v-for="topic in pet.interests.slice(0, 4)" :key="topic" class="topic-chip">{{ topic }}</span>
          </div>
          <p v-else class="side-muted">还没有记录，聊着聊着就有了</p>
        </div>
        <div class="side-links">
          <button @click="router.push('/growth')">成长档案 <span>→</span></button>
          <button @click="router.push('/diary')">成长日记 <span>→</span></button>
          <button @click="router.push('/settings')">伙伴设置 <span>→</span></button>
        </div>
      </section>
      <p class="side-caption">每一次对话，都会成为你们共同的回忆。</p>
    </aside>

    <section class="chat-page">
      <div class="chat-sky" aria-hidden="true">
      <span class="sky-sun"></span>
      <span class="sky-cloud cloud-a"></span>
      <span class="sky-cloud cloud-b"></span>
      <span class="sky-star s1"></span>
      <span class="sky-star s2"></span>
      <span class="sky-star s3"></span>
      <span class="sky-star s4"></span>
      <span class="sky-star s5"></span>
    </div>

    <header class="chat-header">
      <button class="icon-btn" @click="router.push('/')" aria-label="返回首页">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14.5 5 7.5 12l7 7" />
        </svg>
      </button>
      <div v-if="pet" class="chat-pet">
        <PetAvatar :species="pet.species" :stage="pet.growthStage" :mood="pet.mood" :size="52" :animated="true" />
      </div>
      <div class="chat-title">
        <h1>{{ pet?.name || '伙伴' }}</h1>
        <p><span class="online-dot"></span> {{ mood.label }} · {{ pet?.stageName || '初识伙伴' }}</p>
      </div>
      <div class="header-actions">
        <button class="icon-btn" @click="router.push('/tasks')" aria-label="今日任务" title="今日任务">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="4.5" y="4.5" width="15" height="16" rx="3" />
            <path d="M8.5 10.5 10.5 12.5 15.5 7.8M8.5 16h7" />
          </svg>
        </button>
        <button
          class="icon-btn new-btn"
          :disabled="chat.loading"
          @click="confirmNew = true"
          aria-label="开启新对话"
          title="开启新对话"
        >
          <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4.5 6.5h15v9.5h-8.5L6 20v-4H4.5z" />
            <path d="M12 9v5M9.5 11.5h5" />
          </svg>
        </button>
      </div>
    </header>

    <main ref="listRef" class="chat-list">
      <div v-if="isEmpty" class="chat-welcome">
        <div class="welcome-pet">
          <PetAvatar
            :species="pet?.species || 'fox'"
            :stage="pet?.growthStage || 'seed'"
            mood="excited"
            :size="168"
          />
        </div>
        <h2>嗨，我是{{ pet?.name || '你的伙伴' }}！</h2>
        <p>今天过得怎么样呀？想聊什么都可以，我会认真听你说。</p>
        <div class="quick-topics welcome-topics">
          <button v-for="topic in quickTopics" :key="topic" class="quick-chip" @click="quickSend(topic)">
            {{ topic }}
          </button>
        </div>
      </div>

      <div v-else class="chat-flow">
        <div
          v-for="(message, index) in visibleMessages"
          :key="message.id ?? `m-${index}`"
          class="message-row"
          :class="message.role"
        >
          <div v-if="message.role === 'assistant'" class="msg-avatar">
            <PetAvatar
              :species="pet?.species || 'fox'"
              :stage="pet?.growthStage || 'seed'"
              mood="happy"
              :size="42"
              :animated="false"
            />
          </div>
          <div v-else class="msg-avatar user-avatar">
            {{ (auth.username || '我')[0] }}
          </div>
          <div class="bubble-wrap">
            <div class="bubble">{{ message.content }}</div>
            <span v-if="message.createdAt" class="bubble-time">{{ formatDateTime(message.createdAt) }}</span>
          </div>
        </div>

        <div v-if="chat.loading" class="message-row assistant">
          <div class="msg-avatar">
            <PetAvatar
              :species="pet?.species || 'fox'"
              :stage="pet?.growthStage || 'seed'"
              mood="calm"
              :size="42"
              :animated="false"
              thinking
            />
          </div>
          <div class="bubble-wrap">
            <div class="bubble typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <div v-if="chat.suggestedTask" class="suggest-card">
          <div class="suggest-head">
            <span class="suggest-spark">✦</span>
            <strong>为你准备的小任务</strong>
          </div>
          <h3>{{ chat.suggestedTask.title }}</h3>
          <p>{{ chat.suggestedTask.description }}</p>
          <button class="btn btn-sun" @click="router.push(`/tasks/${chat.suggestedTask!.id}`)">
            去看看这个任务
          </button>
        </div>

        <div v-else-if="chat.canSuggestTask && visibleMessages.length > 0" class="suggest-inline">
          <button class="btn btn-soft" :disabled="designLoading" @click="designTask">
            {{ designLoading ? '伙伴正在想…' : '让伙伴设计一个有趣的小任务' }}
          </button>
        </div>

        <div v-if="chat.error" class="chat-error">
          <p class="error-text">{{ chat.error }}</p>
          <p class="retry-hint">消息已放回输入框，再点一次发送试试。</p>
        </div>

        <div v-if="visibleMessages.length < 2" class="quick-topics">
          <button v-for="topic in quickTopics" :key="topic" class="quick-chip" @click="quickSend(topic)">
            {{ topic }}
          </button>
        </div>
      </div>
    </main>

    <div class="composer-fade" aria-hidden="true"></div>

    <footer class="composer">
      <div class="composer-box">
        <input
          v-model="draft"
          type="text"
          maxlength="500"
          placeholder="和伙伴说点什么吧…"
          :disabled="chat.loading"
          @keyup.enter="send"
        />
        <button class="send-btn" :disabled="!draft.trim() || chat.loading" @click="send" aria-label="发送">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 12 20 4l-4.5 16-3.5-6.5z" />
            <path d="M12 13.5 20 4" />
          </svg>
        </button>
      </div>
      <p class="safety-note">遇到害怕或难过的事，记得也告诉爸爸妈妈、老师等信任的大人。</p>
    </footer>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="confirmNew" class="dialog-mask" @click.self="confirmNew = false">
          <div class="dialog-card">
            <div class="dialog-icon">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4.5 6.5h15v9.5h-8.5L6 20v-4H4.5z" />
                <path d="M12 9v5M9.5 11.5h5" />
              </svg>
            </div>
            <h2>开启新对话？</h2>
            <p>当前聊天窗口会清空，但对话记录和伙伴的记忆仍会保留在成长日记里。</p>
            <div class="dialog-actions">
              <button class="btn btn-ghost" :disabled="startingNew" @click="confirmNew = false">取消</button>
              <button class="btn btn-primary" :disabled="startingNew" @click="beginNewConversation">
                {{ startingNew ? '正在开启…' : '开启新对话' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
      </Teleport>
    </section>

    <aside class="chat-side side-right">
      <section v-if="activeTask" class="side-card task-mini">
        <p class="side-label">今日小任务</p>
        <h3>{{ activeTask.title }}</h3>
        <p>{{ activeTask.description }}</p>
        <div class="task-mini-foot">
          <span>+{{ activeTask.reward }} 经验</span>
          <button @click="router.push(`/tasks/${activeTask.id}`)">去看看 →</button>
        </div>
      </section>
      <section v-else class="side-card task-mini">
        <p class="side-label">今日小任务</p>
        <p class="side-muted">还没有任务，让伙伴想一个吧。</p>
        <button class="btn btn-soft mini-btn" :disabled="designLoading" @click="designTask">
          {{ designLoading ? '正在想…' : '设计一个小任务' }}
        </button>
      </section>

      <section class="side-card">
        <p class="side-label">聊点什么</p>
        <div class="side-chips">
          <button v-for="topic in quickTopics" :key="topic" class="side-chip" @click="quickSend(topic)">
            {{ topic }}
          </button>
        </div>
      </section>

      <section v-if="memories.length" class="side-card memory-mini">
        <p class="side-label">伙伴记得</p>
        <ul>
          <li v-for="memory in memories.slice(0, 3)" :key="memory.id">{{ memory.content }}</li>
        </ul>
      </section>
    </aside>
  </div>
</template>

<style scoped lang="scss">
.chat-shell {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 244px minmax(0, 1fr) 244px;
  gap: 20px;
  width: min(1400px, calc(100% - 48px));
  height: calc(100vh - 48px);
  height: calc(100dvh - 48px);
  margin: 24px auto;
}

.chat-page {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  border-radius: 36px;
  overflow: hidden;
  background: linear-gradient(180deg, #e8f6ff 0%, #f4fbff 42%, #fff8ee 100%);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 30px 80px rgba(94, 126, 165, 0.22);
}

/* ---------- 左右侧栏 ---------- */
.chat-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow-y: auto;
  padding: 2px 0 12px;
  scrollbar-width: thin;
}

.side-card {
  padding: 18px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.92);
  box-shadow: var(--elev-1);
  backdrop-filter: blur(14px);
}

.companion-card {
  text-align: center;
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.94), rgba(255, 246, 232, 0.78));
}

.companion-visual {
  display: flex;
  justify-content: center;
  margin: 2px 0 4px;
}

.companion-card h2 {
  font-size: 22px;
}

.companion-sub {
  margin-top: 2px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--ink-faint);
}

.companion-since {
  margin: 2px 0 10px;
  font-size: 10.5px;
  font-weight: 700;
  color: var(--ink-faint);
}

.side-label {
  display: block;
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 1.4px;
  color: var(--ink-faint);
}

.topic-list {
  margin-top: 14px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.topic-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.topic-chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(223, 241, 255, 0.78);
  color: #477d9f;
  font-size: 11px;
  font-weight: 800;
}

.side-muted {
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--ink-faint);
}

.side-links {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 14px;
  text-align: left;
}

.side-links button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.62);
  color: #477d9f;
  font-size: 11.5px;
  font-weight: 800;
  transition: background 0.2s, transform 0.2s;
}

.side-links button:hover {
  background: #ffffff;
  transform: translateX(2px);
}

.side-caption {
  padding: 0 8px;
  text-align: center;
  font-size: 10.5px;
  font-weight: 700;
  color: var(--ink-faint);
}

.task-mini h3 {
  margin: 6px 0 2px;
  font-size: 15px;
}

.task-mini p {
  font-size: 11.5px;
  line-height: 1.6;
  font-weight: 600;
  color: var(--ink-soft);
}

.task-mini-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 11px;
  font-size: 11px;
  font-weight: 800;
  color: #b5892e;
}

.task-mini-foot button {
  color: #477d9f;
  font-size: 11px;
  font-weight: 800;
}

.mini-btn {
  margin-top: 10px;
  padding: 8px 14px;
  font-size: 11.5px;
}

.side-chips {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}

.side-chip {
  padding: 8px 11px;
  border-radius: 12px;
  border: 1px solid rgba(143, 199, 238, 0.28);
  background: rgba(255, 255, 255, 0.62);
  color: #477d9f;
  font-size: 11px;
  font-weight: 700;
  text-align: left;
  transition: background 0.2s, transform 0.2s;
}

.side-chip:hover {
  background: #ffffff;
  transform: translateX(2px);
}

.memory-mini ul {
  margin: 8px 0 0;
  padding-left: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.memory-mini li {
  font-size: 11px;
  line-height: 1.6;
  font-weight: 600;
  color: var(--ink-soft);
}

/* ---------- 梦幻天空背景 ---------- */
.chat-sky {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.sky-sun {
  position: absolute;
  top: -90px;
  right: -70px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 236, 168, 0.55), transparent 65%);
}

.sky-cloud {
  position: absolute;
  height: 42px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
  filter: blur(0.5px);
}

.sky-cloud::before,
.sky-cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}

.sky-cloud::before {
  width: 54px;
  height: 54px;
  top: -24px;
  left: 20px;
}

.sky-cloud::after {
  width: 38px;
  height: 38px;
  top: -15px;
  right: 24px;
}

.cloud-a {
  width: 140px;
  top: 12%;
  left: -46px;
  opacity: 0.8;
}

.cloud-b {
  width: 110px;
  top: 30%;
  right: -34px;
  opacity: 0.55;
}

.sky-star {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.95);
  animation: sky-twinkle 4.2s ease-in-out infinite;
}

.s1 {
  top: 17%;
  left: 18%;
}

.s2 {
  top: 24%;
  right: 16%;
  animation-delay: 1s;
}

.s3 {
  top: 40%;
  left: 8%;
  animation-delay: 2s;
  width: 4px;
  height: 4px;
}

.s4 {
  top: 9%;
  left: 54%;
  animation-delay: 1.6s;
  width: 4px;
  height: 4px;
  background: #ffe8a3;
}

.s5 {
  top: 48%;
  right: 10%;
  animation-delay: 2.6s;
  width: 4px;
  height: 4px;
  background: #ffe8a3;
}

/* ---------- 头部 ---------- */
.chat-header {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: max(12px, env(safe-area-inset-top)) 18px 12px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.75);
}

.icon-btn {
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.82);
  color: var(--ink-soft);
  box-shadow: var(--elev-1);
  transition: transform 0.2s var(--ease-spring), background 0.2s;
}

.icon-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  background: #ffffff;
}

.icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.new-btn {
  background: rgba(216, 245, 228, 0.92);
  color: #3f7d62;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-pet {
  flex: none;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

.chat-title {
  flex: 1;
  min-width: 0;
}

.chat-title h1 {
  font-size: 18px;
  line-height: 1.2;
}

.chat-title p {
  font-size: 11.5px;
  color: var(--ink-faint);
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 5px;
}

.online-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #7cc9a4;
  box-shadow: 0 0 0 3px rgba(124, 201, 164, 0.2);
}

/* ---------- 消息区 ---------- */
.chat-list {
  position: relative;
  z-index: 1;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  padding: 18px 22px 172px;
  scroll-behavior: smooth;
  overscroll-behavior: contain;
}

.chat-flow {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-welcome {
  margin: auto;
  width: 100%;
  max-width: 540px;
  padding: 18px 10px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.welcome-pet {
  display: flex;
  justify-content: center;
  margin-bottom: 2px;
}

.chat-welcome h2 {
  font-size: 22px;
  margin-top: 2px;
}

.chat-welcome p {
  color: var(--ink-soft);
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.7;
}

.welcome-topics {
  margin-top: 16px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 9px;
  animation: message-in 0.35s cubic-bezier(0.34, 1.4, 0.64, 1);
}

.message-row.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex: none;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd97a, #ffc0d2);
  color: #7c4a5e;
  font-weight: 900;
  font-size: 14px;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  box-shadow: 0 6px 14px rgba(242, 160, 182, 0.28);
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: min(72%, 520px);
}

.user .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 12px 16px;
  border-radius: 20px 20px 20px 6px;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 6px 18px rgba(94, 126, 165, 0.1);
  font-size: 14.5px;
  font-weight: 600;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.user .bubble {
  border-radius: 20px 20px 6px 20px;
  background: linear-gradient(135deg, #8fc7ee, #a9d8f7 60%, #b8e3d2);
  color: #173f58;
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(107, 160, 205, 0.26);
}

.bubble-time {
  font-size: 10.5px;
  color: var(--ink-faint);
  font-weight: 700;
  padding: 0 4px;
}

.bubble.typing {
  display: flex;
  gap: 5px;
  padding: 15px 18px;
}

.bubble.typing span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #8fc7ee;
  animation: typing 1.2s ease-in-out infinite;
}

.bubble.typing span:nth-child(2) {
  animation-delay: 0.18s;
}

.bubble.typing span:nth-child(3) {
  animation-delay: 0.36s;
}

.suggest-card {
  align-self: center;
  width: min(100%, 430px);
  padding: 17px 20px;
  border-radius: 22px;
  background: linear-gradient(160deg, #fffdf7, #fff3dd);
  border: 1.5px solid rgba(255, 217, 122, 0.6);
  box-shadow: 0 10px 26px rgba(196, 160, 80, 0.15);
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.suggest-head {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #8a6620;
  font-weight: 800;
}

.suggest-spark {
  font-size: 15px;
}

.suggest-card h3 {
  font-size: 17px;
}

.suggest-card p {
  font-size: 12.5px;
  color: var(--ink-soft);
  font-weight: 600;
  line-height: 1.6;
}

.suggest-card .btn {
  align-self: flex-start;
  margin-top: 3px;
  padding: 8px 16px;
  font-size: 13px;
}

.suggest-inline {
  align-self: center;
  margin-top: 2px;
}

.chat-error {
  align-self: center;
  max-width: 90%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: center;
}

.retry-hint {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-faint);
}

.quick-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  justify-content: center;
  margin-top: 4px;
}

.quick-chip {
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12.5px;
  color: #477d9f;
  border: 1px solid rgba(143, 199, 238, 0.5);
  background: rgba(255, 255, 255, 0.8);
  padding: 7px 14px;
  border-radius: 999px;
  font-weight: 700;
}

.quick-chip:hover {
  background: #ffffff;
  transform: translateY(-2px);
}

/* ---------- 输入区 ---------- */
.composer-fade {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 158px;
  z-index: 4;
  pointer-events: none;
  background: linear-gradient(
    180deg,
    rgba(250, 253, 255, 0) 0%,
    rgba(250, 253, 255, 0.86) 45%,
    rgba(251, 253, 255, 0.98) 75%,
    #fbfdff 100%
  );
}

.composer {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: max(14px, env(safe-area-inset-bottom));
  z-index: 6;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.composer-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px 7px 20px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 36px rgba(94, 126, 165, 0.2);
  border: 1.5px solid rgba(255, 255, 255, 0.95);
}

.composer-box input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14.5px;
  font-weight: 600;
  padding: 8px 0;
}

.composer-box:focus-within {
  border-color: rgba(143, 199, 238, 0.85);
  box-shadow: 0 16px 36px rgba(94, 126, 165, 0.2), 0 0 0 4px rgba(169, 216, 247, 0.24);
}

.composer-box input::placeholder {
  color: var(--ink-faint);
}

.send-btn {
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #8fc7ee, #b8e3d2);
  color: #23506e;
  box-shadow: var(--elev-2);
  transition: transform 0.2s, opacity 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.06);
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.safety-note {
  text-align: center;
  font-size: 10.5px;
  color: rgba(125, 113, 137, 0.7);
  font-weight: 700;
  padding: 0 12px;
}

/* ---------- 新对话确认弹窗 ---------- */
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 95;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(58, 73, 99, 0.36);
  backdrop-filter: blur(6px);
}

.dialog-card {
  width: min(390px, 100%);
  padding: 28px 26px 24px;
  border-radius: 28px;
  background: linear-gradient(170deg, #fffdf8, #f2fbf7);
  border: 2px solid #ffffff;
  box-shadow: 0 30px 80px rgba(60, 84, 120, 0.35);
  text-align: center;
}

.dialog-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 10px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: rgba(216, 245, 228, 0.75);
  color: #3f7d62;
}

.dialog-card h2 {
  font-size: 21px;
}

.dialog-card p {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.7;
  color: var(--ink-soft);
}

.dialog-actions {
  display: flex;
  gap: 9px;
  margin-top: 18px;
}

.dialog-actions .btn {
  flex: 1;
  padding: 11px;
  font-size: 14px;
}

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@keyframes sky-twinkle {
  0%,
  100% {
    opacity: 0.25;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.15);
  }
}

@media (max-width: 1180px) {
  .chat-shell {
    grid-template-columns: 232px minmax(0, 1fr);
  }

  .side-right {
    display: none;
  }
}

@media (max-width: 900px) {
  .chat-shell {
    display: block;
    width: 100%;
    height: 100vh;
    height: 100dvh;
    margin: 0;
  }

  .chat-side {
    display: none;
  }

  .chat-page {
    width: 100%;
    height: 100%;
    margin: 0;
    border-radius: 0;
    border: none;
    box-shadow: none;
  }

  .chat-header {
    padding: 12px 14px;
  }

  .chat-list {
    padding: 14px 16px 164px;
  }

  .composer {
    left: 12px;
    right: 12px;
  }
}

@media (max-width: 640px) {
  .bubble-wrap {
    max-width: 80%;
  }

  .chat-pet {
    width: 46px;
    height: 46px;
  }

  .chat-title h1 {
    font-size: 17px;
  }

  .icon-btn {
    width: 38px;
    height: 38px;
    border-radius: 13px;
  }

  .chat-list {
    padding: 12px 14px 160px;
  }

  .chat-welcome {
    padding: 10px 6px;
  }

  .chat-welcome h2 {
    font-size: 20px;
  }

  .composer-fade {
    height: 150px;
  }

  .quick-chip {
    font-size: 12px;
    padding: 6px 12px;
  }
}
</style>