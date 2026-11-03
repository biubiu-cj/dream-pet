<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const error = ref('')
const guestLoading = ref(false)

const title = computed(() => (mode.value === 'login' ? '欢迎回来' : '认识一下吧'))
const submitText = computed(() =>
  submitting.value ? '正在进入…' : mode.value === 'login' ? '出发去冒险' : '创建我的账号',
)

function setMode(next: 'login' | 'register') {
  if (mode.value === next) return
  mode.value = next
  confirmPassword.value = ''
  error.value = ''
}

function switchMode() {
  setMode(mode.value === 'login' ? 'register' : 'login')
}

async function afterAuth(hasPet: boolean) {
  await router.replace(hasPet ? '/' : '/create-pet')
}

async function submit() {
  if (submitting.value) return
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '先把名字和密码填好哦'
    return
  }
  if (mode.value === 'register') {
    if (password.value.length < 6) {
      error.value = '密码至少需要 6 位'
      return
    }
    if (!confirmPassword.value) {
      error.value = '请再输入一次密码确认'
      return
    }
    if (password.value !== confirmPassword.value) {
      error.value = '两次输入的密码不一样，请再检查一下'
      return
    }
  }
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({ username: username.value.trim(), password: password.value })
    } else {
      await auth.register({ username: username.value.trim(), password: password.value })
    }
    await afterAuth(auth.hasPet)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '出了点小问题，请稍后再试'
  } finally {
    submitting.value = false
  }
}

async function enterAsGuest() {
  if (guestLoading.value) return
  error.value = ''
  guestLoading.value = true
  try {
    await auth.guest()
    await afterAuth(false)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '体验账号创建失败'
  } finally {
    guestLoading.value = false
  }
}
</script>

<template>
  <div class="welcome page">
    <header class="topbar">
      <div class="brand">
        <span class="brand-star" aria-hidden="true">
          <svg viewBox="0 0 40 40" fill="none">
            <path
              d="M20 4 24.6 15.4 36 17.2 27.4 25.2 30 36 20 30.2 10 36 12.6 25.2 4 17.2 15.4 15.4z"
              fill="#ffe8a3"
              stroke="#e9b94e"
              stroke-width="2.2"
              stroke-linejoin="round"
            />
          </svg>
        </span>
        <div class="brand-text">
          <span class="brand-name">DreamPet</span>
          <span class="brand-tag">AI 梦想养成伙伴</span>
        </div>
      </div>
      <span class="safe-pill">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3 5 6v5c0 4.6 3 8.3 7 10 4-1.7 7-5.4 7-10V6z" />
          <path d="M9.5 12.2 11.3 14l3.4-3.6" />
        </svg>
        安全陪伴 · 无广告
      </span>
    </header>

    <div class="welcome-grid">
      <section class="hero">
        <p class="eyebrow">
          <span class="eyebrow-dot"></span>
          为 6~10 岁孩子设计的 AI 伙伴
        </p>
        <h1>
          每一次陪伴<br />
          <span>都是一次成长</span>
        </h1>
        <p class="intro">
          和它聊聊天、做个小任务，把今天的小发现记下来。
          它会记住你说过的话，陪你一起慢慢长大。
        </p>

        <div class="showcase">
          <span class="showcase-glow"></span>
          <span class="showcase-star star-a"></span>
          <span class="showcase-star star-b"></span>
          <span class="showcase-star star-c"></span>

          <div class="mood-pill"><span class="mood-dot"></span>今天也元气满满</div>

          <div class="floating-chip chip-left">
            <span class="chip-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 20s-7.2-4.4-7.2-9.2A4.1 4.1 0 0 1 12 8.1a4.1 4.1 0 0 1 7.2 2.7C19.2 15.6 12 20 12 20z" />
              </svg>
            </span>
            会记住你说过的话
          </div>
          <div class="floating-chip chip-right">
            <span class="chip-icon task">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4.5" y="4.5" width="15" height="16" rx="3" />
                <path d="M8.5 10.5 10.5 12.5 15.5 7.8M8.5 16h7" />
              </svg>
            </span>
            一起完成小任务
          </div>

          <PetAvatar species="fox" stage="bloom" mood="excited" :size="300" />
          <div class="showcase-base"></div>
        </div>

        <div class="features">
          <div class="feature">
            <span class="f-icon memory">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 20s-7.2-4.4-7.2-9.2A4.1 4.1 0 0 1 12 8.1a4.1 4.1 0 0 1 7.2 2.7C19.2 15.6 12 20 12 20z" />
              </svg>
            </span>
            <div>
              <strong>长期记忆</strong>
              <small>记住你们聊过的小事</small>
            </div>
          </div>
          <div class="feature">
            <span class="f-icon task">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 3.5 14.6 9l6 .9-4.3 4.2 1 6-5.3-2.8-5.3 2.8 1-6L3.4 9.9l6-.9z" />
              </svg>
            </span>
            <div>
              <strong>趣味任务</strong>
              <small>每天一个轻轻的小探索</small>
            </div>
          </div>
          <div class="feature">
            <span class="f-icon growth">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 19c0-5 3.5-9 8-9 0 5-3 9-8 9z" />
                <path d="M13 10c0-4 2.5-6.5 6-7 .3 4-2 7-6 7z" />
                <path d="M5 19v-4" />
              </svg>
            </span>
            <div>
              <strong>一起成长</strong>
              <small>升级解锁新的成长阶段</small>
            </div>
          </div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="auth-card">
          <div class="tabs" role="tablist">
            <button :class="{ active: mode === 'login' }" role="tab" @click="setMode('login')">登录</button>
            <button :class="{ active: mode === 'register' }" role="tab" @click="setMode('register')">注册</button>
          </div>

          <h2>{{ title }}</h2>
          <p class="auth-hint">
            {{ mode === 'login' ? '输入你的名字，伙伴在等你。' : '起一个好记的名字，两次输入相同的密码。' }}
          </p>

          <form class="auth-form" @submit.prevent="submit">
            <label class="input-wrap">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="8.5" r="3.6" />
                  <path d="M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5" />
                </svg>
              </span>
              <input
                v-model="username"
                type="text"
                autocomplete="username"
                placeholder="你的名字，例如：小星星"
                maxlength="20"
              />
            </label>

            <label class="input-wrap">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="5" y="10" width="14" height="10" rx="3" />
                  <path d="M8.5 10V7.5a3.5 3.5 0 0 1 7 0V10" />
                </svg>
              </span>
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
                placeholder="密码，至少 6 位"
                maxlength="64"
              />
              <button
                type="button"
                class="eye-btn"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3.5 12s3-5.5 8.5-5.5 8.5 5.5 8.5 5.5-3 5.5-8.5 5.5S3.5 12 3.5 12z" />
                  <circle cx="12" cy="12" r="2.6" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 5l16 14" />
                  <path d="M9.9 6.8A9.6 9.6 0 0 1 12 6.5c5.5 0 8.5 5.5 8.5 5.5a15 15 0 0 1-3 3.5" />
                  <path d="M6.4 8.9A15.8 15.8 0 0 0 3.5 12s3 5.5 8.5 5.5c1.2 0 2.3-.3 3.3-.7" />
                </svg>
              </button>
            </label>

            <label v-if="mode === 'register'" class="input-wrap">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="5" y="10" width="14" height="10" rx="3" />
                  <path d="M8.5 10V7.5a3.5 3.5 0 0 1 7 0V10" />
                  <path d="M9.5 15l1.7 1.7 3.3-3.4" />
                </svg>
              </span>
              <input
                v-model="confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="再输入一次密码"
                maxlength="64"
              />
            </label>

            <p v-if="error" class="error-text">{{ error }}</p>

            <button class="btn submit-btn" type="submit" :disabled="submitting">
              {{ submitText }}
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h13M13 6.5l5.5 5.5-5.5 5.5" />
              </svg>
            </button>
          </form>

          <div class="divider"><span>或者</span></div>

          <button class="guest-btn" :disabled="guestLoading" @click="enterAsGuest">
            <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3.5 13.9 9l5.6.9-4 4 1 5.6-5.5-2.7-5.5 2.7 1-5.6-4-4L7.1 9z" />
            </svg>
            {{ guestLoading ? '正在准备…' : '一键体验，不用注册' }}
          </button>

          <button class="switch-link" @click="switchMode">
            {{ mode === 'login' ? '还没有账号？去注册' : '已经有账号？去登录' }}
          </button>

          <p class="privacy">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3 5 6v5c0 4.6 3 8.3 7 10 4-1.7 7-5.4 7-10V6z" />
              <path d="M9.5 12.2 11.3 14l3.4-3.6" />
            </svg>
            不收集真实姓名、学校或住址。这里是属于你的安全小天地。
          </p>
        </div>

        <p class="panel-note">每一次对话、每一个小任务，都会成为你们共同的回忆。</p>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.welcome {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100vh;
  padding-top: 22px;
  max-width: 1180px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: clamp(20px, 4vh, 40px);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.brand-star {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 10px 24px rgba(118, 148, 182, 0.16);
}

.brand-star svg {
  width: 26px;
  height: 26px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-name {
  font-family: 'Baloo 2', 'Nunito', sans-serif;
  font-weight: 800;
  font-size: 20px;
  letter-spacing: 0.5px;
  color: #3d6f92;
}

.brand-tag {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.2px;
  color: var(--ink-faint);
}

.safe-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: #4e8a6f;
  font-size: 11.5px;
  font-weight: 800;
  box-shadow: 0 8px 20px rgba(118, 148, 182, 0.12);
}

.welcome-grid {
  display: grid;
  grid-template-columns: 1.12fr 0.88fr;
  gap: clamp(28px, 5vw, 64px);
  align-items: center;
}

.hero {
  min-width: 0;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #5d93bd;
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 0.6px;
  box-shadow: 0 8px 20px rgba(118, 148, 182, 0.12);
}

.eyebrow-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #7cc9a4;
  box-shadow: 0 0 0 4px rgba(124, 201, 164, 0.2);
}

.hero h1 {
  margin-top: 16px;
  font-size: clamp(38px, 4.6vw, 58px);
  line-height: 1.14;
  color: #35475c;
  letter-spacing: 0.5px;
}

.hero h1 span {
  background: linear-gradient(110deg, #ef9a55 10%, #ea7f95 55%, #a78bfa 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.intro {
  margin-top: 14px;
  max-width: 460px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.8;
  color: var(--ink-soft);
}

.showcase {
  position: relative;
  margin-top: 20px;
  height: 330px;
  border-radius: 36px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 72%, rgba(255, 255, 255, 0.98), rgba(223, 241, 255, 0.5) 52%, rgba(255, 255, 255, 0) 74%),
    linear-gradient(160deg, rgba(255, 255, 255, 0.55), rgba(255, 244, 228, 0.4));
  border: 1.5px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 24px 60px rgba(118, 148, 182, 0.16);
}

.showcase::before {
  content: '';
  position: absolute;
  top: -30%;
  left: 50%;
  width: 70%;
  height: 90%;
  transform: translateX(-50%);
  background: radial-gradient(circle, rgba(255, 232, 163, 0.35), transparent 65%);
}

.showcase-glow {
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 72%;
  height: 26%;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(255, 255, 255, 0.9), transparent 70%);
}

.showcase-star {
  position: absolute;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #ffd97a;
  box-shadow: 0 0 14px rgba(255, 217, 122, 0.9);
  animation: twinkle 3.6s ease-in-out infinite;
}

.star-a {
  top: 18%;
  left: 12%;
}

.star-b {
  top: 12%;
  right: 14%;
  animation-delay: 1.2s;
  background: #ffc0d2;
}

.star-c {
  top: 44%;
  right: 6%;
  width: 6px;
  height: 6px;
  animation-delay: 2.2s;
}

.mood-pill {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.85);
  font-size: 10.5px;
  font-weight: 800;
  color: var(--ink-soft);
  box-shadow: 0 6px 16px rgba(118, 148, 182, 0.12);
  white-space: nowrap;
}

.mood-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #7cc9a4;
}

.floating-chip {
  position: absolute;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 13px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 26px rgba(118, 148, 182, 0.18);
  font-size: 11px;
  font-weight: 800;
  color: var(--ink-soft);
  white-space: nowrap;
  animation: floaty 5s ease-in-out infinite;
}

.chip-left {
  top: 26%;
  left: 4%;
}

.chip-right {
  top: 44%;
  right: 3%;
  animation-delay: 1.4s;
}

.chip-icon {
  width: 22px;
  height: 22px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: #ffe4ea;
  color: #d97898;
}

.chip-icon svg {
  width: 13px;
  height: 13px;
}

.chip-icon.task {
  background: #e2f2ff;
  color: #5d93bd;
}

.showcase-base {
  position: absolute;
  bottom: 10px;
  left: 50%;
  width: 46%;
  height: 14px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: rgba(94, 126, 165, 0.1);
  filter: blur(7px);
}

.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 9px;
  margin-top: 16px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.85);
}

.f-icon {
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 11px;
  display: grid;
  place-items: center;
}

.f-icon svg {
  width: 17px;
  height: 17px;
}

.f-icon.memory {
  background: #ffe4ea;
  color: #d97898;
}

.f-icon.task {
  background: #fff0d6;
  color: #b07a3d;
}

.f-icon.growth {
  background: #ddf5e8;
  color: #4e9378;
}

.feature strong {
  display: block;
  font-size: 11.5px;
  font-weight: 800;
}

.feature small {
  display: block;
  font-size: 9.5px;
  font-weight: 700;
  color: var(--ink-faint);
  line-height: 1.4;
}

.auth-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 440px;
  margin: 0 auto;
}

.auth-card {
  position: relative;
  padding: clamp(24px, 3vw, 32px);
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(22px);
  border: 1.5px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 30px 70px rgba(94, 126, 165, 0.2);
  overflow: hidden;
}

.auth-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 10%;
  width: 80%;
  height: 3px;
  border-radius: 0 0 999px 999px;
  background: linear-gradient(90deg, transparent, #ffd97a, #ffc0d2, transparent);
  opacity: 0.8;
}

.tabs {
  display: flex;
  gap: 6px;
  padding: 5px;
  border-radius: 999px;
  background: rgba(223, 241, 255, 0.55);
  margin-bottom: 20px;
}

.tabs button {
  flex: 1;
  padding: 9px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 14px;
  color: var(--ink-soft);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.tabs button.active {
  background: #ffffff;
  color: #2f6d95;
  box-shadow: 0 6px 16px rgba(93, 147, 189, 0.18);
}

.auth-card h2 {
  font-size: 25px;
}

.auth-hint {
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 600;
  margin-top: 4px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 52px;
  padding: 0 15px;
  border-radius: 17px;
  border: 2px solid rgba(169, 216, 247, 0.45);
  background: rgba(255, 255, 255, 0.9);
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.input-wrap:focus-within {
  border-color: #8cc5ee;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(169, 216, 247, 0.3);
}

.input-icon {
  flex: none;
  width: 19px;
  height: 19px;
  color: #9db2c7;
}

.input-icon svg {
  width: 100%;
  height: 100%;
}

.input-wrap input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  padding: 0;
}

.input-wrap input::placeholder {
  color: #b3aabf;
  font-weight: 600;
}

.eye-btn {
  flex: none;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #a99eb5;
  transition: background 0.2s, color 0.2s;
}

.eye-btn:hover {
  background: rgba(223, 241, 255, 0.7);
  color: #5d93bd;
}

.eye-btn svg {
  width: 18px;
  height: 18px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  margin-top: 4px;
  font-size: 16px;
  background: linear-gradient(135deg, #7fb9e6, #a8d5f5 55%, #b8e3d2);
  color: #23506e;
  box-shadow: 0 14px 30px rgba(107, 160, 205, 0.35);
}

.submit-btn svg {
  transition: transform 0.2s;
}

.submit-btn:hover svg {
  transform: translateX(3px);
}

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
  color: var(--ink-faint);
  font-size: 11px;
  font-weight: 700;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(125, 113, 137, 0.14);
}

.guest-btn {
  width: 100%;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 999px;
  border: 1.5px dashed rgba(233, 185, 78, 0.55);
  background: rgba(255, 248, 226, 0.7);
  color: #8a6418;
  font-weight: 800;
  font-size: 13.5px;
  transition: all 0.2s;
}

.guest-btn:hover:not(:disabled) {
  background: rgba(255, 244, 210, 0.95);
  transform: translateY(-1px);
}

.guest-btn:disabled {
  opacity: 0.6;
}

.switch-link {
  display: block;
  margin: 14px auto 0;
  font-size: 12.5px;
  font-weight: 800;
  color: #5d93bd;
}

.switch-link:hover {
  text-decoration: underline;
}

.privacy {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 16px;
  padding-top: 13px;
  border-top: 1px dashed rgba(125, 113, 137, 0.14);
  font-size: 10.5px;
  line-height: 1.7;
  color: var(--ink-faint);
  font-weight: 600;
}

.privacy svg {
  flex: none;
  margin-top: 2px;
  color: #7cc9a4;
}

.panel-note {
  text-align: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--ink-faint);
  padding: 0 10px;
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@keyframes floaty {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-7px);
  }
}

@media (max-width: 960px) {
  .welcome-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .hero {
    text-align: center;
  }

  .intro {
    margin-left: auto;
    margin-right: auto;
  }

  .showcase {
    height: 290px;
    max-width: 560px;
    margin-left: auto;
    margin-right: auto;
  }

  .auth-panel {
    max-width: 560px;
  }
}

@media (max-width: 640px) {
  .welcome {
    justify-content: flex-start;
    padding-top: 14px;
  }

  .safe-pill {
    display: none;
  }

  .hero h1 {
    font-size: clamp(32px, 9vw, 42px);
  }

  .showcase {
    height: 270px;
    border-radius: 28px;
  }

  .floating-chip {
    display: none;
  }

  .features {
    gap: 6px;
  }

  .feature {
    flex-direction: column;
    gap: 5px;
    text-align: center;
    padding: 9px 6px;
  }

  .feature small {
    display: none;
  }

  .auth-panel {
    max-width: 100%;
  }

  .auth-card {
    border-radius: 28px;
  }
}

</style>