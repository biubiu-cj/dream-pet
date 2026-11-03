import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentPet } from '@/api/pets'
import type { Pet } from '@/api/types'
import { ApiError } from '@/api/http'
import { useAuthStore } from './auth'

export const usePetStore = defineStore('pet', () => {
  const pet = ref<Pet | null>(null)
  const loaded = ref(false)
  const loading = ref(false)
  const error = ref('')

  async function load(force = false) {
    if (loading.value) return pet.value
    if (loaded.value && !force) return pet.value
    loading.value = true
    error.value = ''
    try {
      pet.value = await getCurrentPet()
      useAuthStore().setHasPet(true)
    } catch (e) {
      if (e instanceof ApiError && e.code === 404) {
        pet.value = null
        useAuthStore().setHasPet(false)
      } else {
        error.value = e instanceof Error ? e.message : '伙伴暂时走丢了'
      }
    } finally {
      loading.value = false
      loaded.value = true
    }
    return pet.value
  }

  function setPet(value: Pet) {
    pet.value = value
    loaded.value = true
    useAuthStore().setHasPet(true)
  }

  function reset() {
    pet.value = null
    loaded.value = false
    error.value = ''
  }

  return { pet, loaded, loading, error, load, setPet, reset }
})