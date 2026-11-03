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
  gap: 10px;
}

.growth-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.level-badge {
  padding: 4px 13px;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, #f5bd4f, #ffd97a);
  color: #5f400c;
  font-weight: 900;
  font-size: 13px;
  font-family: 'Baloo 2', sans-serif;
  box-shadow: 0 4px 12px rgba(233, 185, 78, 0.32);
}

.stage-name {
  font-weight: 800;
  font-size: 14px;
  color: var(--ink-soft);
}

.exp-hint {
  margin-left: auto;
  padding-left: 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-faint);
}

.bar-track {
  position: relative;
  height: 14px;
  border-radius: var(--radius-pill);
  background: rgba(123, 146, 177, 0.14);
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(93, 147, 189, 0.12);
}

.bar-fill {
  position: relative;
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, #6fb0e4, #8fd4c4 62%, #f5c863);
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