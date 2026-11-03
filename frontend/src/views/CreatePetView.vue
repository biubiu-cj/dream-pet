<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import PetAvatar from '@/components/pets/PetAvatar.vue'
import { createPet } from '@/api/pets'
import { PERSONALITY_OPTIONS, SPECIES_OPTIONS } from '@/constants/labels'
import type { Personality, Species } from '@/api/types'
import { usePetStore } from '@/stores/pet'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const petStore = usePetStore()
const auth = useAuthStore()

const species = ref<Species>('fox')
const name = ref('')
const personality = ref<Personality>('curious')
const submitting = ref(false)
const error = ref('')

const nameIdeas = ['星星', '豆豆', '小光', '团子', '米粒', '闪闪', '果果', '小云']
const selectedSpecies = computed(() => SPECIES_OPTIONS.find((item) => item.value === species.value)!)
const selectedPersonality = computed(
  () => PERSONALITY_OPTIONS.find((item) => item.value === personality.value)!,
)

function randomName() {
  name.value = nameIdeas[Math.floor(Math.random() * nameIdeas.length)]!
}

async function submit() {
  if (submitting.value) return
  error.value = ''
  const cleanName = name.value.trim()
  if (!cleanName) {
    error.value = '给你的伙伴起个名字吧'
    return
  }
  if (cleanName.length > 16) {
    error.value = '名字最多 16 个字'
    return
  }
  submitting.value = true
  try {
    const pet = await createPet({
      name: cleanName,
      species: species.value,
      personality: personality.value,
    })
    petStore.setPet(pet)
    auth.setHasPet(true)
    await router.replace('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="create page">
    <header class="create-header">
      <p class="eyebrow">第一步 · 相遇</p>
      <h1>创建你的专属伙伴</h1>
      <p class="sub">它会记住你的名字和你说过的话，陪你一起冒险。</p>
    </header>

    <div class="create-grid">
      <section class="preview-card card">
        <div class="preview-stage">
          <span class="preview-star s1"></span>
          <span class="preview-star s2"></span>
          <PetAvatar
            :species="species"
            stage="seed"
            mood="excited"
            :size="250"
            :animated="true"
          />
        </div>
        <div class="preview-info">
          <h2>{{ name.trim() || '还没有名字' }}</h2>
          <p class="preview-tags">
            <span class="chip">{{ selectedSpecies.name }}</span>
            <span class="chip">{{ selectedPersonality.label }}</span>
          </p>
          <p class="preview-desc">{{ selectedPersonality.desc }}</p>
          <p class="preview-hint">想聊什么都可以，伙伴会慢慢记住你说过的话</p>
        </div>
      </section>

      <section class="form-card card">
        <div class="form-block">
          <h3 class="block-title">选择伙伴种类</h3>
          <div class="species-grid">
            <button
              v-for="option in SPECIES_OPTIONS"
              :key="option.value"
              class="species-item"
              :class="{ active: species === option.value }"
              :style="{ '--tint': option.color }"
              @click="species = option.value"
            >
              <span class="species-avatar">
                <PetAvatar :species="option.value" stage="seed" mood="happy" :size="76" :animated="false" />
              </span>
              <span class="species-name">{{ option.name }}</span>
              <span class="species-blurb">{{ option.blurb }}</span>
            </button>
          </div>
        </div>

        <div class="form-block">
          <h3 class="block-title">给它起个名字</h3>
          <div class="name-row">
            <input
              v-model="name"
              class="input"
              maxlength="16"
              placeholder="例如：星星"
              @keyup.enter="submit"
            />
            <button class="btn btn-ghost dice" type="button" @click="randomName">换个灵感</button>
          </div>
        </div>

        <div class="form-block">
          <h3 class="block-title">选择性格</h3>
          <div class="personality-grid">
            <button
              v-for="option in PERSONALITY_OPTIONS"
              :key="option.value"
              class="personality-item"
              :class="{ active: personality === option.value }"
              @click="personality = option.value"
            >
              <strong>{{ option.label }}</strong>
              <span>{{ option.desc }}</span>
            </button>
          </div>
        </div>

        <p v-if="error" class="error-text">{{ error }}</p>

        <button class="btn btn-sun create-btn" :disabled="submitting" @click="submit">
          {{ submitting ? '正在唤醒伙伴…' : '和它见面吧' }}
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.create {
  padding-top: 36px;
}

.create-header {
  text-align: center;
  margin-bottom: 26px;
}

.eyebrow {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  color: #5d93bd;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.create-header h1 {
  font-size: clamp(27px, 4vw, 38px);
}

.sub {
  margin-top: 8px;
  color: var(--ink-soft);
  font-weight: 600;
  font-size: 14.5px;
}

.create-grid {
  display: grid;
  grid-template-columns: 0.72fr 1.28fr;
  gap: 24px;
  align-items: start;
}

.preview-card {
  position: sticky;
  top: 24px;
  padding: 18px 20px 24px;
  overflow: hidden;
}

.preview-stage {
  position: relative;
  display: flex;
  justify-content: center;
  padding: 10px 0 0;
  border-radius: 22px;
  background: radial-gradient(circle at 50% 62%, rgba(223, 241, 255, 0.85), rgba(255, 255, 255, 0.2) 70%);
}

.preview-star {
  position: absolute;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #ffe8a3;
  box-shadow: 0 0 12px rgba(255, 217, 122, 0.9);
  animation: twinkle 3.4s ease-in-out infinite;
}

.s1 {
  top: 18%;
  left: 16%;
}

.s2 {
  top: 30%;
  right: 14%;
  animation-delay: 1.2s;
  background: #ffd5e0;
}

.preview-info {
  text-align: center;
  margin-top: 8px;
}

.preview-info h2 {
  font-size: 24px;
}

.preview-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

.preview-desc {
  margin-top: 10px;
  font-size: 13px;
  color: var(--ink-soft);
  font-weight: 600;
}

.preview-hint {
  margin-top: 12px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--ink-faint);
  line-height: 1.6;
}

.form-card {
  padding: clamp(20px, 3vw, 30px);
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.block-title {
  font-size: 16px;
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.block-title small {
  font-size: 11.5px;
  color: var(--ink-faint);
  font-weight: 700;
}

.species-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.species-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 10px 6px 9px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid transparent;
  transition: all 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.species-item:hover {
  transform: translateY(-3px);
}

.species-item.active {
  border-color: var(--tint);
  background: #ffffff;
  box-shadow: 0 12px 26px rgba(118, 148, 182, 0.18);
  transform: translateY(-3px) scale(1.03);
}

.species-avatar {
  width: 70px;
  height: 70px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.species-name {
  font-size: 12.5px;
  font-weight: 800;
}

.species-blurb {
  font-size: 10px;
  color: var(--ink-faint);
  font-weight: 700;
  text-align: center;
}

.name-row {
  display: flex;
  gap: 10px;
}

.name-row .input {
  flex: 1;
}

.dice {
  flex: none;
  padding-left: 18px;
  padding-right: 18px;
}

.personality-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.personality-item {
  text-align: left;
  padding: 12px 15px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
  gap: 3px;
  transition: all 0.2s;
}

.personality-item strong {
  font-size: 14px;
}

.personality-item span {
  font-size: 11.5px;
  color: var(--ink-faint);
  font-weight: 600;
}

.personality-item.active {
  border-color: #a9d8f7;
  background: #ffffff;
  box-shadow: 0 10px 22px rgba(118, 148, 182, 0.14);
}

.create-btn {
  width: 100%;
  font-size: 17px;
  padding: 15px;
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.4;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@media (max-width: 900px) {
  .create-grid {
    grid-template-columns: 1fr;
  }

  .preview-card {
    position: static;
  }

  .preview-stage {
    padding-top: 0;
  }
}

@media (max-width: 560px) {
  .species-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .name-row {
    flex-direction: column;
  }
}
</style>