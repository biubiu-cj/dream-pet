<script setup lang="ts">
withDefaults(
  defineProps<{
    level: number
    percent: number
    expToNext?: number
    stageName?: string
  }>(),
  { expToNext: 0, stageName: '' },
)
</script>

<template>
  <div class="growth-bar">
    <div class="growth-head">
      <span class="level-badge">Lv.{{ level }}</span>
      <span v-if="stageName" class="stage-name">{{ stageName }}</span>
      <span class="exp-hint">
        {{ expToNext > 0 ? `再获得 ${expToNext} 经验升级` : '已经满级啦' }}
      </span>
    </div>
    <div class="bar-track">
      <div class="bar-fill" :style="{ width: `${Math.min(100, Math.max(0, percent))}%` }">
        <span class="bar-shine"></span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.growth-bar {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.growth-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.level-badge {
  padding: 3px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ffd97a, #ffe9a8);
  color: #7c5a17;
  font-weight: 900;
  font-size: 13px;
  font-family: 'Baloo 2', sans-serif;
  box-shadow: 0 4px 12px rgba(233, 185, 78, 0.3);
}

.stage-name {
  font-weight: 800;
  font-size: 13.5px;
  color: var(--ink-soft);
}

.exp-hint {
  margin-left: auto;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-faint);
}

.bar-track {
  position: relative;
  height: 16px;
  border-radius: 999px;
  background: rgba(169, 216, 247, 0.32);
  overflow: hidden;
  box-shadow: inset 0 2px 6px rgba(93, 147, 189, 0.1);
}

.bar-fill {
  position: relative;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8fc7ee, #b8e3d2 65%, #ffd97a);
  transition: width 0.9s cubic-bezier(0.34, 1.2, 0.64, 1);
  overflow: hidden;
}

.bar-shine {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    100deg,
    transparent 20%,
    rgba(255, 255, 255, 0.55) 45%,
    transparent 65%
  );
  animation: shine 2.6s linear infinite;
}

@keyframes shine {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(120%);
  }
}
</style>