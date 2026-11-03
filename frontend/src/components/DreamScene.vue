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
        fill="rgba(206, 236, 219, 0.5)"
      />
      <path
        d="M0,225 C190,180 340,250 520,215 C720,175 880,245 1060,210 C1230,178 1340,235 1440,205 L1440,280 L0,280 Z"
        fill="rgba(185, 225, 204, 0.55)"
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
  background:
    radial-gradient(1100px 700px at 12% -8%, #eef8ff 0%, rgba(238, 248, 255, 0) 62%),
    radial-gradient(900px 640px at 92% 4%, #fff6e8 0%, rgba(255, 246, 232, 0) 58%),
    linear-gradient(180deg, #f2f9ff 0%, #f7fbff 46%, #fdf7ef 100%);
  pointer-events: none;
}

.sky-glow {
  position: absolute;
  top: -20%;
  left: -10%;
  width: 120%;
  height: 75%;
  background:
    radial-gradient(circle at 22% 30%, rgba(255, 255, 255, 0.6), transparent 45%),
    radial-gradient(circle at 78% 18%, rgba(255, 236, 190, 0.3), transparent 48%);
}

/* 太阳降为柔和氛围光斑：不再是抢视线的高亮色块，也不会被画布裁出硬边 */
.sun {
  position: absolute;
  top: 5%;
  right: 7%;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 233, 168, 0.4) 0%, rgba(255, 226, 140, 0.14) 46%, transparent 70%);
  filter: blur(6px);
  animation: sun-breathe 9s ease-in-out infinite;
}

.sun-core {
  position: absolute;
  inset: 34%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 240, 190, 0.5), rgba(255, 217, 122, 0.18) 70%, transparent);
  box-shadow: 0 0 36px rgba(255, 217, 122, 0.3);
}

.stars {
  position: absolute;
  inset: 0;
}

.star {
  position: absolute;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.8);
  opacity: 0.6;
  animation: twinkle 4s ease-in-out infinite;
}

.cloud {
  position: absolute;
  height: 52px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.5);
  filter: blur(3px);
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
  top: 14%;
  left: 6%;
  animation: drift 32s linear infinite;
}

.cloud-b {
  width: 128px;
  top: 28%;
  left: 58%;
  opacity: 0.6;
  animation: drift 44s linear infinite reverse;
}

.cloud-c {
  width: 196px;
  top: 7%;
  left: 44%;
  opacity: 0.4;
  animation: drift 56s linear infinite;
}

/* 山丘淡化为底部氛围带，并用遮罩羽化上沿，避免与卡片中段形成假横线 */
.hills {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 170px;
  opacity: 0.42;
  mask-image: linear-gradient(180deg, transparent 0%, #000 55%);
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 55%);
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