<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    desc?: string
  }>(),
  { desc: '' },
)
</script>

<template>
  <div class="empty-state">
    <div class="empty-art" aria-hidden="true">
      <svg viewBox="0 0 120 90" fill="none">
        <ellipse cx="60" cy="76" rx="38" ry="7" fill="rgba(118, 148, 182, 0.12)" />
        <circle cx="60" cy="46" r="26" fill="#eaf6ff" stroke="#cdE7fa" stroke-width="2" />
        <circle cx="51" cy="43" r="2.6" fill="#8fc7ee" />
        <circle cx="69" cy="43" r="2.6" fill="#8fc7ee" />
        <path d="M53 53 Q60 58 67 53" stroke="#8fc7ee" stroke-width="2.4" stroke-linecap="round" />
        <path d="M92 24 l2.4 5.2 5.2 2.4 -5.2 2.4 -2.4 5.2 -2.4 -5.2 -5.2 -2.4 5.2 -2.4z" fill="#ffe8a3" />
        <circle cx="26" cy="30" r="3" fill="#ffd5e0" />
        <circle cx="34" cy="66" r="2" fill="#d8f5e4" />
      </svg>
    </div>
    <h3>{{ title }}</h3>
    <p v-if="desc" class="muted">{{ desc }}</p>
    <div class="empty-action">
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
  padding: 36px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.6);
  border: 1.5px dashed rgba(143, 199, 238, 0.6);
}

.empty-art svg {
  width: 110px;
  height: 82px;
}

.empty-state h3 {
  font-size: 17px;
  font-weight: 800;
}

.empty-state p {
  font-size: 13.5px;
  font-weight: 600;
  max-width: 340px;
}

.empty-action {
  margin-top: 10px;
}
</style>