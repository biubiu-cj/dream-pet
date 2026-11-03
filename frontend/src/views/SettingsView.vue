<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import { updatePet } from '@/api/pets'
import { deleteAccount } from '@/api/account'
import type { Personality, Species } from '@/api/types'
import {
  PERSONALITY_OPTIONS,
  SPECIES_OPTIONS,
  speciesOption,
} from '@/constants/labels'
import { useAuthStore } from '@/stores/auth'
import { usePetStore } from '@/stores/pet'
import { useChatStore } from '@/stores/chat'
import { useTasksStore } from '@/stores/tasks'

const router = useRouter()
const auth = useAuthStore()
const petStore = usePetStore()
const chatStore = useChatStore()
const tasksStore = useTasksStore()

const name = ref('')
const species = ref<Species>('fox')
const personality = ref<Personality>('curious')
const saving = ref(false)
const saved = ref(false)
const error = ref('')
const confirmingLogout = ref(false)
const confirmingDelete = ref(false)
const deleting = ref(false)

const pet = computed(() => petStore.pet)
const selectedSpeciesName = computed(() => speciesOption(species.value).name)

watch(
  pet,
  (value) => {
    if (!value) return
    name.value = value.name
    species.value = value.species
    personality.value = value.personality
  },
  { immediate: true },
)

onMounted(async () => {
  await petStore.load()
})


async function save() {
  if (!pet.value || saving.value) return
  error.value = ''
  saved.value = false
  if (!name.value.trim()) {
    error.value = '伙伴名字不能为空'
    return
  }
  saving.value = true
  try {
    const updated = await updatePet(pet.value.id, {
      name: name.value.trim(),
      species: species.value,
      personality: personality.value,
    })
    petStore.setPet(updated)
    saved.value = true
    window.setTimeout(() => (saved.value = false), 2600)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败，请稍后再试'
  } finally {
    saving.value = false
  }
}

function logout() {
  auth.logout()
  petStore.reset()
  chatStore.reset()
  tasksStore.reset()
  router.replace('/welcome')
}

function confirmLogout() {
  confirmingLogout.value = false
  logout()
}

async function removeAccount() {
  if (deleting.value) return
  deleting.value = true
  error.value = ''
  try {
    await deleteAccount()
    logout()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '删除失败，请稍后再试'
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="settings page">
    <header class="page-head">
      <div class="page-head-main">
        <p class="eyebrow">我的伙伴</p>
        <h1>伙伴设置</h1>
        <p class="sub">可以随时更换伙伴种类、修改名字和性格，成长与记忆都会保留。</p>
      </div>
      <button class="btn btn-ghost logout-top" @click="confirmingLogout = true">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
          <path d="M10 8l-4 4 4 4M6 12h9" />
        </svg>
        退出登录
      </button>
    </header>

    <div v-if="!pet" class="card loading">正在读取伙伴资料…</div>

    <template v-else>
      <section class="settings-card card">
        <div class="pet-preview">
          <PetAvatar
            :species="species"
            :stage="pet.growthStage"
            mood="happy"
            :size="200"
          />
          <p class="preview-name">{{ name || pet.name }}</p>
          <p class="preview-stage">{{ selectedSpeciesName }} · Lv.{{ pet.level }} {{ pet.stageName }}</p>
        </div>

        <div class="form-area">
          <div class="field">
            <label>伙伴种类 <small>可以换成其他小伙伴</small></label>
            <div class="species-picker">
              <button
                v-for="option in SPECIES_OPTIONS"
                :key="option.value"
                class="species-choice"
                :class="{ active: species === option.value }"
                :style="{ '--tint': option.color }"
                @click="species = option.value; saved = false"
              >
                <PetAvatar :species="option.value" stage="seed" mood="happy" :size="62" :animated="false" />
                <span>{{ option.name }}</span>
              </button>
            </div>
          </div>

          <div class="field">
            <label for="pet-name">伙伴名字</label>
            <input id="pet-name" v-model="name" class="input" maxlength="16" />
          </div>

          <div class="field">
            <label>伙伴性格</label>
            <div class="personality-chips">
              <button
                v-for="option in PERSONALITY_OPTIONS"
                :key="option.value"
                class="personality-chip"
                :class="{ active: personality === option.value }"
                @click="personality = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <p v-if="error" class="error-text">{{ error }}</p>
          <p v-if="saved" class="saved-text">已经记住啦！</p>

          <button class="btn btn-primary save-btn" :disabled="saving" @click="save">
            {{ saving ? '正在保存…' : '保存伙伴资料' }}
          </button>
        </div>
      </section>

      <section class="account-card card">
        <div class="account-main">
          <div class="account-avatar">{{ (auth.username || 'D')[0] }}</div>
          <div class="account-info">
            <p class="label">当前账号</p>
            <p class="value">{{ auth.username }}</p>
            <p class="account-status"><span class="status-dot"></span> 已登录 · 数据仅自己可见</p>
          </div>
          <span class="mode-badge">真实 AI 模式</span>
        </div>
        <p class="mode-desc">
          当前只使用真实文本大模型，伙伴的回复由 AI 生成。系统已启用意图识别（规则优先，大模型兜底），
          用于判断「想要任务 / 回忆过去 / 分享兴趣 / 情绪低落」等，并调整伙伴的回应方式。
        </p>
        <div class="account-actions">
          <button class="btn btn-ghost logout" @click="confirmingLogout = true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
              <path d="M10 8l-4 4 4 4M6 12h9" />
            </svg>
            退出登录
          </button>
        </div>
      </section>

      <section class="safety-card card">
        <h2>安全与隐私</h2>
        <ul>
          <li>DreamPet 不需要你的真实姓名、学校、住址或照片。</li>
          <li>你在聊天里说的话只有你和伙伴知道，不会展示给其他人。</li>
          <li>遇到让你害怕或难过的事情，请一定告诉爸爸妈妈、老师等可信任的大人。</li>
          <li>伙伴是虚拟的 AI 朋友，不能替代现实中的家人和朋友。</li>
        </ul>
      </section>

      <section class="danger-card card">
        <div>
          <h2>删除我的数据</h2>
          <p>删除后，伙伴、兴趣、任务和所有聊天记录都会消失，无法恢复。</p>
        </div>
        <button v-if="!confirmingDelete" class="btn btn-pink" @click="confirmingDelete = true">
          删除全部数据
        </button>
        <div v-else class="confirm-row">
          <button class="btn btn-ghost" :disabled="deleting" @click="confirmingDelete = false">
            再想想
          </button>
          <button class="btn btn-pink" :disabled="deleting" @click="removeAccount">
            {{ deleting ? '正在删除…' : '确认删除' }}
          </button>
        </div>
      </section>
    </template>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="confirmingLogout" class="dialog-mask" @click.self="confirmingLogout = false">
          <div class="dialog-card">
            <div class="dialog-icon">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
                <path d="M10 8l-4 4 4 4M6 12h9" />
              </svg>
            </div>
            <h2>要退出登录吗？</h2>
            <p>伙伴、成长和你们的记忆都会好好保存，下次回来继续陪它长大。</p>
            <div class="dialog-actions">
              <button class="btn btn-ghost" @click="confirmingLogout = false">继续陪伴</button>
              <button class="btn btn-pink" @click="confirmLogout">退出登录</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-head-main {
  min-width: 0;
}

.logout-top {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  font-size: 13px;
  margin-top: 4px;
}

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
  width: min(380px, 100%);
  padding: 28px 26px 24px;
  border-radius: var(--radius-lg);
  background: linear-gradient(170deg, #fffdf8, #fff5e8);
  border: 2px solid #ffffff;
  box-shadow: var(--elev-3);
  text-align: center;
}

.dialog-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 10px;
  border-radius: var(--radius-lg);
  display: grid;
  place-items: center;
  background: rgba(255, 232, 163, 0.55);
  color: #a8822f;
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

.eyebrow {
  font-size: 12px;
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

.settings-card {
  display: grid;
  grid-template-columns: 0.7fr 1.3fr;
  gap: 22px;
  padding: clamp(18px, 3vw, 30px);
  margin-bottom: 16px;
}

.pet-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 14px;
  border-radius: var(--radius-lg);
  background: radial-gradient(circle at 50% 55%, rgba(223, 241, 255, 0.9), transparent 68%);
}

.preview-name {
  font-family: 'Baloo 2', sans-serif;
  font-size: 21px;
  font-weight: 800;
  color: #35475c;
}

.preview-stage {
  font-size: 12px;
  font-weight: 800;
  color: var(--ink-faint);
}

.form-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field label small {
  font-size: 12px;
  color: var(--ink-faint);
  margin-left: 4px;
}

.species-picker {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.species-choice {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  padding: 8px 4px 7px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid transparent;
  transition: all 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.species-choice:hover {
  transform: translateY(-2px);
}

.species-choice.active {
  border-color: var(--tint);
  background: #ffffff;
  box-shadow: var(--elev-2);
  transform: translateY(-2px) scale(1.03);
}

.species-choice span {
  font-size: 12px;
  font-weight: 800;
  color: var(--ink-soft);
}

.species-choice.active span {
  color: var(--ink);
}

.personality-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 560px) {
  .species-picker {
    grid-template-columns: repeat(3, 1fr);
  }
}

.personality-chip {
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid transparent;
  font-weight: 800;
  font-size: 13px;
  color: var(--ink-soft);
  transition: all 0.2s;
}

.personality-chip.active {
  border-color: #a9d8f7;
  background: #ffffff;
  color: #2f6d95;
  box-shadow: var(--elev-2);
}

.saved-text {
  font-size: 13px;
  font-weight: 800;
  color: #4e9378;
}

.save-btn {
  align-self: flex-start;
  padding: 12px 30px;
}

.account-card {
  padding: 20px 24px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.account-main {
  display: flex;
  align-items: center;
  gap: 13px;
}

.account-avatar {
  flex: none;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ffd97a, #ffc0d2);
  color: #7c4a5e;
  font-weight: 900;
  font-size: 19px;
  box-shadow: var(--elev-2);
}

.account-info {
  flex: 1;
  min-width: 0;
}

.label {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--ink-faint);
}

.value {
  font-size: 17px;
  font-weight: 800;
  line-height: 1.3;
}

.account-status {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 1px;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-faint);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #7cc9a4;
  box-shadow: 0 0 0 3px rgba(124, 201, 164, 0.18);
}

.mode-badge {
  flex: none;
  font-size: 12px;
  font-weight: 800;
  padding: 5px 13px;
  border-radius: var(--radius-pill);
  background: rgba(216, 245, 228, 0.75);
  color: #376e56;
}

.mode-desc {
  font-size: 12.5px;
  color: var(--ink-soft);
  font-weight: 600;
  line-height: 1.7;
}

.account-actions {
  display: flex;
  justify-content: flex-end;
}

.logout {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  font-size: 13px;
}

.safety-card {
  padding: 20px 24px;
  margin-bottom: 16px;
}

.safety-card h2 {
  font-size: 17px;
  margin-bottom: 8px;
}

.safety-card ul {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.safety-card li {
  font-size: 12.5px;
  color: var(--ink-soft);
  font-weight: 600;
}

.danger-card {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  border: 1.5px dashed rgba(242, 160, 182, 0.55);
  background: rgba(255, 248, 250, 0.8);
}

.danger-card h2 {
  font-size: 16px;
  color: #a84a62;
}

.danger-card p {
  font-size: 12px;
  color: var(--ink-soft);
  font-weight: 600;
  margin-top: 3px;
}

.danger-card .btn {
  padding: 9px 20px;
  font-size: 13px;
}

.confirm-row {
  display: flex;
  gap: 8px;
}

@media (max-width: 760px) {
  .settings-card {
    grid-template-columns: 1fr;
  }

  .save-btn {
    width: 100%;
  }
}
</style>