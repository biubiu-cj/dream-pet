import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { GrowthStage } from '@/api/types'

interface CelebrationPayload {
  levelUp: boolean
  stageUp: boolean
  newLevel: number
  newStage: GrowthStage
  xpGained: number
  title?: string
}

export const useCelebrationStore = defineStore('celebration', () => {
  const visible = ref(false)
  const payload = ref<CelebrationPayload | null>(null)

  let timer: number | undefined

  function celebrate(data: CelebrationPayload) {
    if (!data.levelUp && !data.stageUp) return
    payload.value = data
    visible.value = true
    window.clearTimeout(timer)
    timer = window.setTimeout(close, 5200)
  }

  function close() {
    visible.value = false
    payload.value = null
    window.clearTimeout(timer)
  }

  return { visible, payload, celebrate, close }
})