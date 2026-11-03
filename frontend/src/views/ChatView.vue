<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { usePetStore } from '@/stores/pet'
import { useChatStore } from '@/stores/chat'
import { useTasksStore } from '@/stores/tasks'
import { formatDateTime } from '@/utils/format'
import { moodMeta } from '@/constants/labels'

const router = useRouter()
const auth = useAuthStore()
const petStore = usePetStore()
const chat = useChatStore()
const tasksStore = useTasksStore()

const draft = ref('')
const listRef = ref<HTMLElement | null>(null)
const designLoading = ref(false)

const pet = computed(() => petStore.pet)
const mood = computed(() => moodMeta(pet.value?.mood))

const quickTopics = [
  '今天我发现了一个有趣的事',
  '我们聊聊恐龙吧',
  '给我讲个宇宙的小知识',
  '我们做个小任务吧',
]

const visibleMessages = computed(() =>
  chat.messages.filter((message) => message.role === 'user' || message.role === 'assistant'),
)

onMounted(async () => {
  await petStore.load()
  await chat.loadHistory()
  scrollToBottom(false)
})

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
</script>

<template>
  <div class="chat-page">
    <header class="chat-header">
      <button class="back-btn" @click="router.push('/')" aria-label="返回首页">
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
      <button class="back-btn" @click="router.push('/tasks')" aria-label="今日任务">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4.5" y="4.5" width="15" height="16" rx="3" />
          <path d="M8.5 10.5 10.5 12.5 15.5 7.8M8.5 16h7" />
        </svg>
      </button>
    </header>

    <main ref="listRef" class="chat-list">
      <div v-if="!visibleMessages.length && !chat.loading" class="chat-welcome">
        <PetAvatar
          :species="pet?.species || 'fox'"
          :stage="pet?.growthStage || 'seed'"
          mood="excited"
          :size="170"
        />
        <h2>嗨，我是{{ pet?.name || '你的伙伴' }}！</h2>
        <p>今天过得怎么样呀？有什么想和我分享的吗？</p>
      </div>

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
            :size="44"
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
            :size="44"
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
        <button v-for="topic in quickTopics" :key="topic" class="chip quick-chip" @click="quickSend(topic)">
          {{ topic }}
        </button>
      </div>
    </main>

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
  </div>
</template>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 clamp(10px, 3vw, 24px);
  position: relative;
  z-index: 2;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 4px 12px;
  position: sticky;
  top: 0;
  z-index: 5;
  background: linear-gradient(180deg, rgba(240, 249, 255, 0.96), rgba(240, 249, 255, 0.7));
  backdrop-filter: blur(12px);
}

.back-btn {
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.8);
  color: var(--ink-soft);
  box-shadow: 0 6px 16px rgba(118, 148, 182, 0.14);
  transition: transform 0.2s;
}

.back-btn:hover {
  transform: translateY(-2px);
}

.chat-pet {
  flex: none;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.65);
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

.chat-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 2px 210px;
  scroll-behavior: smooth;
}

.chat-welcome {
  margin: auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 10px;
}

.chat-welcome h2 {
  font-size: 22px;
  margin-top: 4px;
}

.chat-welcome p {
  color: var(--ink-soft);
  font-size: 13.5px;
  font-weight: 600;
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
  width: 44px;
  height: 44px;
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
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: min(74%, 520px);
}

.user .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 12px 16px;
  border-radius: 20px 20px 20px 6px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 20px rgba(118, 148, 182, 0.12);
  font-size: 14.5px;
  font-weight: 600;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.user .bubble {
  border-radius: 20px 20px 6px 20px;
  background: linear-gradient(135deg, #8fc7ee, #a9d8f7);
  color: #173f58;
  box-shadow: 0 8px 20px rgba(107, 160, 205, 0.28);
}

.bubble-time {
  font-size: 10px;
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
  width: min(100%, 440px);
  padding: 18px 20px;
  border-radius: 24px;
  background: linear-gradient(160deg, #fffdf7, #fff3dd);
  border: 1.5px solid rgba(255, 217, 122, 0.6);
  box-shadow: 0 14px 32px rgba(196, 160, 80, 0.16);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggest-head {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #b5892e;
  font-weight: 800;
}

.suggest-spark {
  font-size: 15px;
}

.suggest-card h3 {
  font-size: 18px;
}

.suggest-card p {
  font-size: 13px;
  color: var(--ink-soft);
  font-weight: 600;
}

.suggest-card .btn {
  align-self: flex-start;
  margin-top: 4px;
  padding: 9px 18px;
  font-size: 13.5px;
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
  font-size: 11.5px;
  font-weight: 700;
  color: var(--ink-faint);
}

.quick-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 6px;
}

.quick-chip {
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12.5px;
  color: #477d9f;
  border: 1px solid rgba(143, 199, 238, 0.5);
}

.quick-chip:hover {
  background: #ffffff;
  transform: translateY(-2px);
}

.composer {
  position: fixed;
  left: 50%;
  bottom: 88px;
  transform: translateX(-50%);
  width: min(880px, calc(100% - 24px));
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.composer-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px 7px 20px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 40px rgba(100, 134, 172, 0.22);
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
  box-shadow: 0 8px 18px rgba(107, 160, 205, 0.35);
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
  color: rgba(125, 113, 137, 0.75);
  font-weight: 700;
  padding: 0 12px;
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

@media (max-width: 640px) {
  .bubble-wrap {
    max-width: 78%;
  }

  .composer {
    bottom: 84px;
  }

  .chat-list {
    padding-bottom: 205px;
  }
}
</style>