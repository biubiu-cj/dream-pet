<script setup lang="ts">
import { computed } from 'vue'
import type { GrowthStage, Mood, Species } from '@/api/types'
import foxImg from '@/assets/pets/fox.png'
import rabbitImg from '@/assets/pets/rabbit.png'
import catImg from '@/assets/pets/cat.png'
import bearImg from '@/assets/pets/bear.png'
import dinoImg from '@/assets/pets/dino.png'

const props = withDefaults(
  defineProps<{
    species?: Species
    stage?: GrowthStage
    mood?: Mood
    size?: number
    thinking?: boolean
    animated?: boolean
  }>(),
  {
    species: 'fox',
    stage: 'seed',
    mood: 'happy',
    size: 220,
    thinking: false,
    animated: true,
  },
)

const IMAGES: Record<Species, string> = {
  fox: foxImg,
  rabbit: rabbitImg,
  cat: catImg,
  bear: bearImg,
  dino: dinoImg,
}

const petImage = computed(() => IMAGES[props.species] ?? IMAGES.fox)
const sparkleCount = computed(() => (props.stage === 'dream' ? 7 : props.stage === 'bloom' ? 5 : 0))
</script>

<template>
  <div
    class="pet-avatar"
    :class="[
      `species-${species}`,
      `stage-${stage}`,
      `mood-${mood}`,
      { thinking, animated },
    ]"
    :style="{ width: `${size}px`, height: `${size}px` }"
  >
    <div class="aura"></div>
    <div class="ground-shadow"></div>
    <div v-if="stage === 'dream'" class="halo"></div>

    <div class="pet-stage">
      <img class="pet-image" :src="petImage" :alt="`${species} 伙伴形象`" draggable="false" />

      <div v-if="stage === 'dream'" class="crown" aria-hidden="true">
        <svg viewBox="0 0 100 68" fill="none">
          <path
            d="M12 52 L12 20 L30 34 L50 8 L70 34 L88 20 L88 52 Z"
            fill="#ffd97a"
            stroke="#e9b94e"
            stroke-width="4"
            stroke-linejoin="round"
          />
          <circle cx="50" cy="16" r="5" fill="#fff7da" />
          <circle cx="18" cy="26" r="3.6" fill="#fff7da" />
          <circle cx="82" cy="26" r="3.6" fill="#fff7da" />
        </svg>
      </div>
    </div>

    <div v-if="sparkleCount" class="sparkles">
      <span v-for="i in sparkleCount" :key="i" :class="['sparkle', `s${i}`]"></span>
    </div>

    <div v-if="thinking" class="think-dots" aria-label="伙伴正在思考">
      <span></span><span></span><span></span>
    </div>
    <div v-if="mood === 'sleepy'" class="sleep-z" aria-hidden="true">z</div>
  </div>
</template>

<style scoped lang="scss">
.pet-avatar {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  justify-content: center;
  user-select: none;
}

.aura {
  position: absolute;
  inset: 5% 0 2%;
  border-radius: 50%;
  background: radial-gradient(
    circle at 50% 58%,
    rgba(255, 255, 255, 0.95),
    rgba(223, 241, 255, 0.4) 58%,
    transparent 74%
  );
  pointer-events: none;
}

.ground-shadow {
  position: absolute;
  bottom: 1%;
  left: 50%;
  width: 56%;
  height: 7%;
  transform: translateX(-50%);
  border-radius: 50%;
  background: rgba(94, 126, 165, 0.18);
  filter: blur(6px);
  z-index: 0;
}

.pet-stage {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.pet-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center bottom;
  transform-origin: 50% 100%;
  transition: filter 0.4s ease;
  filter: drop-shadow(0 10px 14px rgba(94, 126, 165, 0.12));
}

.stage-sprout .pet-stage {
  transform: scale(1.02);
}

.stage-bloom .pet-stage {
  transform: scale(1.05);
}

.stage-dream .pet-stage {
  transform: scale(1.07);
}

.animated .pet-image {
  animation: pet-breathe 4.4s ease-in-out infinite;
}

.animated.mood-excited .pet-image {
  animation: pet-sway 2.9s ease-in-out infinite;
  filter: saturate(1.06) drop-shadow(0 10px 14px rgba(94, 126, 165, 0.12));
}

.animated.mood-calm .pet-image {
  animation-duration: 5.4s;
}

.animated.mood-sleepy .pet-image {
  animation: pet-sleep-sway 6.6s ease-in-out infinite;
  filter: brightness(0.95) saturate(0.85) drop-shadow(0 10px 14px rgba(94, 126, 165, 0.1));
}

.animated.thinking .pet-image {
  animation: think-tilt 2.6s ease-in-out infinite;
}

.crown {
  position: absolute;
  top: -1%;
  left: 50%;
  width: 30%;
  transform: translateX(-50%);
  z-index: 2;
  pointer-events: none;
  animation: crown-float 3.6s ease-in-out infinite;
}

.crown svg {
  width: 100%;
  height: auto;
  display: block;
  filter: drop-shadow(0 4px 8px rgba(233, 185, 78, 0.35));
}

.halo {
  position: absolute;
  top: 2%;
  left: 50%;
  width: 74%;
  aspect-ratio: 1;
  transform: translateX(-50%);
  border-radius: 50%;
  border: 3px solid rgba(255, 217, 122, 0.75);
  box-shadow: 0 0 38px rgba(255, 226, 140, 0.8), inset 0 0 26px rgba(255, 226, 140, 0.5);
  animation: halo-pulse 3.2s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}

.sparkles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 3;
}

.sparkle {
  position: absolute;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: radial-gradient(circle, #fff7d8 20%, #ffd97a 70%, transparent 72%);
  animation: sparkle-twinkle 2.8s ease-in-out infinite;
}

.s1 { top: 18%; left: 4%; }
.s2 { top: 7%; right: 8%; animation-delay: 0.5s; }
.s3 { top: 42%; right: -2%; animation-delay: 1.1s; }
.s4 { bottom: 18%; left: -2%; animation-delay: 1.6s; }
.s5 { bottom: 7%; right: 12%; animation-delay: 0.9s; }
.s6 { top: 32%; left: 0; animation-delay: 2s; }
.s7 { top: 55%; right: 3%; animation-delay: 1.4s; }

.think-dots {
  position: absolute;
  top: 3%;
  right: -4%;
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 18px rgba(100, 134, 172, 0.24);
  z-index: 4;
}

.think-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8fc7ee;
  animation: dot-bounce 1.2s ease-in-out infinite;
}

.think-dots span:nth-child(2) {
  animation-delay: 0.18s;
}

.think-dots span:nth-child(3) {
  animation-delay: 0.36s;
}

.sleep-z {
  position: absolute;
  top: 9%;
  right: 7%;
  font-family: 'Baloo 2', sans-serif;
  font-size: 24px;
  font-weight: 800;
  color: #9c8ad4;
  animation: z-float 3.4s ease-in-out infinite;
  z-index: 4;
}

@keyframes pet-breathe {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-5px) scale(1.012);
  }
}

@keyframes pet-sway {
  0%,
  100% {
    transform: translateY(0) rotate(-1.2deg);
  }
  50% {
    transform: translateY(-6px) rotate(1.2deg);
  }
}

@keyframes pet-sleep-sway {
  0%,
  100% {
    transform: translateY(0) rotate(-2deg);
  }
  50% {
    transform: translateY(-3px) rotate(2deg);
  }
}

@keyframes think-tilt {
  0%,
  100% {
    transform: rotate(-2deg);
  }
  50% {
    transform: rotate(2.5deg);
  }
}

@keyframes crown-float {
  0%,
  100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(-3px);
  }
}

@keyframes halo-pulse {
  0%,
  100% {
    opacity: 0.6;
    transform: translateX(-50%) scale(0.98);
  }
  50% {
    opacity: 1;
    transform: translateX(-50%) scale(1.04);
  }
}

@keyframes sparkle-twinkle {
  0%,
  100% {
    opacity: 0.2;
    transform: scale(0.6);
  }
  50% {
    opacity: 1;
    transform: scale(1.15);
  }
}

@keyframes dot-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.55;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

@keyframes z-float {
  0%,
  100% {
    opacity: 0.5;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-8px);
  }
}
</style>