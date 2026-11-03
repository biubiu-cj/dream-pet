<script setup lang="ts">
import { computed } from 'vue'
import { useCelebrationStore } from '@/stores/celebration'
import { stageOption } from '@/constants/labels'

const celebration = useCelebrationStore()

const title = computed(() => {
  if (!celebration.payload) return ''
  return celebration.payload.stageUp ? '成长阶段晋级！' : '升级啦！'
})

const stageLabel = computed(() =>
  celebration.payload?.stageUp ? stageOption(celebration.payload.newStage).label : '',
)
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="celebration.visible && celebration.payload" class="celebration-mask" @click.self="celebration.close()">
        <div class="confetti" aria-hidden="true">
          <span v-for="i in 18" :key="i" :class="`c${i}`"></span>
        </div>
        <div class="celebration-card pop-enter-active">
          <div class="glow"></div>
          <div class="big-star" aria-hidden="true">
            <svg viewBox="0 0 64 64" fill="none">
              <path
                d="M32 6 39.4 23.2 58 25.2 44.4 37.8 48.6 56.2 32 46.6 15.4 56.2 19.6 37.8 6 25.2 24.6 23.2z"
                fill="#ffe8a3"
                stroke="#e9b94e"
                stroke-width="2.4"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <h2>{{ title }}</h2>
          <p class="level-line">
            你的伙伴成长到 <strong>Lv.{{ celebration.payload.newLevel }}</strong>
          </p>
          <p v-if="celebration.payload.stageUp" class="stage-line">进入「{{ stageLabel }}」阶段</p>
          <p class="xp-line" v-if="celebration.payload.xpGained > 0">
            本次获得 +{{ celebration.payload.xpGained }} 经验
          </p>
          <button class="btn btn-sun" @click="celebration.close()">太棒啦，继续冒险</button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped lang="scss">
.celebration-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(58, 73, 99, 0.35);
  backdrop-filter: blur(6px);
}

.celebration-card {
  position: relative;
  width: min(400px, 94vw);
  padding: 34px 30px 30px;
  border-radius: 32px;
  text-align: center;
  background: linear-gradient(170deg, #fffdf7, #fff4e2);
  border: 2px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 30px 80px rgba(60, 84, 120, 0.35);
  overflow: hidden;
}

.glow {
  position: absolute;
  top: -40%;
  left: 50%;
  width: 130%;
  height: 110%;
  transform: translateX(-50%);
  background: radial-gradient(circle, rgba(255, 232, 163, 0.6), transparent 62%);
  pointer-events: none;
}

.big-star svg {
  width: 74px;
  height: 74px;
  animation: star-spin 6s linear infinite;
  filter: drop-shadow(0 8px 18px rgba(233, 185, 78, 0.45));
}

.celebration-card h2 {
  margin: 10px 0 6px;
  font-size: 26px;
  color: #7c5a17;
}

.level-line {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-soft);
}

.level-line strong {
  color: #2f6d95;
  font-family: 'Baloo 2', sans-serif;
  font-size: 20px;
}

.stage-line {
  margin-top: 6px;
  font-weight: 800;
  color: #b5892e;
}

.xp-line {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-faint);
}

.celebration-card .btn {
  margin-top: 18px;
  width: 100%;
}

.confetti {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.confetti span {
  position: absolute;
  top: -20px;
  width: 9px;
  height: 14px;
  border-radius: 3px;
  opacity: 0.9;
  animation: confetti-fall 3.4s linear infinite;
}

.c1 { left: 6%; background: #ffc0d2; animation-delay: 0s; }
.c2 { left: 14%; background: #a9d8f7; animation-delay: 0.5s; }
.c3 { left: 22%; background: #ffe8a3; animation-delay: 1s; }
.c4 { left: 30%; background: #d8f5e4; animation-delay: 1.4s; }
.c5 { left: 38%; background: #e6ddfb; animation-delay: 0.2s; }
.c6 { left: 46%; background: #ffd9c2; animation-delay: 1.8s; }
.c7 { left: 54%; background: #a9d8f7; animation-delay: 0.8s; }
.c8 { left: 62%; background: #ffe8a3; animation-delay: 2.1s; }
.c9 { left: 70%; background: #ffc0d2; animation-delay: 1.2s; }
.c10 { left: 78%; background: #d8f5e4; animation-delay: 0.4s; }
.c11 { left: 86%; background: #e6ddfb; animation-delay: 1.6s; }
.c12 { left: 92%; background: #ffd9c2; animation-delay: 2.4s; }
.c13 { left: 10%; background: #a9d8f7; animation-delay: 2.8s; }
.c14 { left: 34%; background: #ffc0d2; animation-delay: 2.6s; }
.c15 { left: 58%; background: #ffe8a3; animation-delay: 3s; }
.c16 { left: 74%; background: #d8f5e4; animation-delay: 2.9s; }
.c17 { left: 90%; background: #e6ddfb; animation-delay: 3.2s; }
.c18 { left: 50%; background: #ffc0d2; animation-delay: 3.4s; }

@keyframes confetti-fall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  100% {
    transform: translateY(110vh) rotate(540deg);
    opacity: 0.7;
  }
}

@keyframes star-spin {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>