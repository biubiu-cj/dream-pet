<script setup lang="ts">
const stars = Array.from({ length: 26 }, (_, index) => ({
  id: index,
  left: `${(index * 37 + 9) % 96}%`,
  top: `${(index * 23 + 5) % 62}%`,
  size: 2 + ((index * 7) % 3),
  delay: `${(index % 7) * 0.6}s`,
  duration: `${3 + (index % 5)}s`,
}))
</script>

<template>
  <div class="dream-scene" aria-hidden="true">
    <div class="sky-glow"></div>
    <div class="sun">
      <div class="sun-core"></div>
    </div>

    <div class="stars">
      <span
        v-for="star in stars"
        :key="star.id"
        class="star"
        :style="{
          left: star.left,
          top: star.top,
          width: `${star.size}px`,
          height: `${star.size}px`,
          animationDelay: star.delay,
          animationDuration: star.duration,
        }"
      ></span>
    </div>

    <div class="cloud cloud-a"></div>
    <div class="cloud cloud-b"></div>
    <div class="cloud cloud-c"></div>

    <svg class="hills" viewBox="0 0 1440 280" preserveAspectRatio="none">
      <path
        d="M0,190 C170,120 300,230 470,170 C650,105 760,205 940,160 C1110,118 1240,205 1440,140 L1440,280 L0,280 Z"
        fill="rgba(190, 230, 205, 0.75)"
      />
      <path
        d="M0,225 C190,180 340,250 520,215 C720,175 880,245 1060,210 C1230,178 1340,235 1440,205 L1440,280 L0,280 Z"
        fill="rgba(160, 216, 184, 0.85)"
      />
    </svg>
  </div>
</template>

<style scoped lang="scss">
.dream-scene {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: linear-gradient(180deg, #e7f5ff 0%, #f3f9ff 38%, #fff6ea 74%, #fdf1df 100%);
  pointer-events: none;
}

.sky-glow {
  position: absolute;
  top: -20%;
  left: -10%;
  width: 120%;
  height: 75%;
  background:
    radial-gradient(circle at 22% 30%, rgba(255, 255, 255, 0.85), transparent 45%),
    radial-gradient(circle at 78% 18%, rgba(255, 236, 190, 0.55), transparent 48%);
}

.sun {
  position: absolute;
  top: 7%;
  right: 8%;
  width: 108px;
  height: 108px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 236, 168, 0.95), rgba(255, 226, 140, 0.25) 62%, transparent 72%);
  animation: sun-breathe 7s ease-in-out infinite;
}

.sun-core {
  position: absolute;
  inset: 26%;
  border-radius: 50%;
  background: linear-gradient(145deg, #ffe9a8, #ffd97a);
  box-shadow: 0 0 40px rgba(255, 217, 122, 0.85);
}

.stars {
  position: absolute;
  inset: 0;
}

.star {
  position: absolute;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.95);
  animation: twinkle 4s ease-in-out infinite;
}

.cloud {
  position: absolute;
  height: 52px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  filter: blur(1px);
  box-shadow: 0 18px 40px rgba(150, 185, 220, 0.18);
}

.cloud::before,
.cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}

.cloud::before {
  width: 64px;
  height: 64px;
  top: -28px;
  left: 22px;
}

.cloud::after {
  width: 46px;
  height: 46px;
  top: -18px;
  right: 30px;
}

.cloud-a {
  width: 168px;
  top: 16%;
  left: 6%;
  animation: drift 32s linear infinite;
}

.cloud-b {
  width: 128px;
  top: 30%;
  left: 58%;
  opacity: 0.8;
  animation: drift 44s linear infinite reverse;
}

.cloud-c {
  width: 196px;
  top: 8%;
  left: 44%;
  opacity: 0.55;
  animation: drift 56s linear infinite;
}

.hills {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 240px;
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1.15);
  }
}

@keyframes drift {
  from {
    transform: translateX(-6vw);
  }
  to {
    transform: translateX(106vw);
  }
}

@keyframes sun-breathe {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.06);
  }
}

@media (max-width: 640px) {
  .cloud-c {
    display: none;
  }
  .sun {
    width: 84px;
    height: 84px;
  }
}
</style>